package com.jobchef.worker.generation;

import tools.jackson.databind.ObjectMapper;
import com.jobchef.shared.config.BackendProperties;
import com.jobchef.shared.error.ApiException;
import com.jobchef.shared.service.PreviewArtifactStore;
import com.jobchef.shared.service.ResumePrivacyGuard;
import com.jobchef.shared.service.RedisRateLimiter;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class GenerationWorkService {
    private static final Logger LOG = LoggerFactory.getLogger(GenerationWorkService.class);
    private static final int MAX_ATTEMPTS = 3;
    private static final int MAX_RECOVERY_BATCH = 20;
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final ApplicationGenerator generator;
    private final RedisRateLimiter limiter;
    private final BackendProperties properties;
    private final TransactionTemplate transactions;
    private final LatexPreviewRenderer previewRenderer;
    private final PreviewArtifactStore artifacts;

    public GenerationWorkService(
            JdbcTemplate jdbc,
            ObjectMapper objectMapper,
            ApplicationGenerator generator,
            RedisRateLimiter limiter,
            BackendProperties properties,
            TransactionTemplate transactions,
            LatexPreviewRenderer previewRenderer,
            PreviewArtifactStore artifacts) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
        this.generator = generator;
        this.limiter = limiter;
        this.properties = properties;
        this.transactions = transactions;
        this.previewRenderer = previewRenderer;
        this.artifacts = artifacts;
    }

    public void process(UUID applicationId, int generationVersion) {
        String capacityToken = UUID.randomUUID().toString();
        String capacityKey = "jobchef:generation:leases";
        boolean acquired;
        try {
            acquired = limiter.acquireLease(
                    capacityKey, capacityToken, properties.limits().maxConcurrentGenerations(), Duration.ofMinutes(7));
        } catch (ApiException exception) {
            throw new RetryGenerationException("Rate-limit storage is unavailable; Pub/Sub should retry.");
        }
        if (!acquired) {
            throw new RetryGenerationException("All configured generation slots are currently occupied.");
        }
        try {
            Claim claim = transactions.execute(status -> claimLocked(applicationId, generationVersion));
            if (claim == null) return;
            generateAndRecord(claim);
        } finally {
            limiter.releaseLease(capacityKey, capacityToken);
        }
    }

    private void generateAndRecord(Claim claim) {
        String uncommittedArtifactKey = null;
        try {
            if (claim.resumeText().length() + claim.jobDescription().length()
                    > properties.limits().maxGenerationInputCharacters()) {
                throw new GenerationFailure("GENERATION_INPUT_TOO_LARGE", false, false,
                        "The selected resume and job description exceed the configured generation input limit.");
            }
            String resumeForGeneration = claim.resumeText();
            if (!generator.demoMode()) {
                resumeForGeneration = parseResumeOnce(claim);
                int inputCharacters = resumeForGeneration.length()
                        + claim.jobDescription().length() + claim.jobTitle().length() + claim.company().length();
                if (!reserveDailyProviderCall(inputCharacters)) {
                    throw new GenerationFailure("GLOBAL_DAILY_LIMIT", false, true,
                            "The configured daily provider-call or token allowance has been reached. No provider request was made.");
                }
            }
            var generated = generator.generate(
                    claim.jobTitle(), claim.company(), claim.jobDescription(), resumeForGeneration);
            String generatedJson;
            try {
                generatedJson = objectMapper.writeValueAsString(generated);
            } catch (Exception exception) {
                throw new GenerationFailure("INVALID_MODEL_RESPONSE", false, false,
                        "Generated content could not be validated and was not stored.");
            }
            if (ResumePrivacyGuard.containsObviousContactData(generatedJson)) {
                throw new GenerationFailure("GENERATED_CONTACT_DATA", false, false,
                        "Generated output contained apparent contact details and was not stored.");
            }
            byte[] previewPdf = previewRenderer.render(generated);
            String objectKey = "applications/" + claim.id() + "/" + claim.generationVersion()
                    + "/" + UUID.randomUUID() + ".pdf";
            uncommittedArtifactKey = objectKey;
            artifacts.put(objectKey, previewPdf);
            Boolean persisted = transactions.execute(
                    status -> persistReady(claim, generated, objectKey));
            if (!Boolean.TRUE.equals(persisted)) {
                deleteArtifact(uncommittedArtifactKey);
            }
            uncommittedArtifactKey = null;
        } catch (GenerationFailure failure) {
            deleteArtifact(uncommittedArtifactKey);
            transactions.executeWithoutResult(status ->
                    recordFailure(claim, failure.code(), failure.getMessage(), failure.quota(),
                            failure.retryable()));
            if (failure.retryable() && !failure.quota() && claim.attempt() < MAX_ATTEMPTS) {
                throw new RetryGenerationException("The generation attempt will be retried.");
            }
        } catch (ApiException exception) {
            deleteArtifact(uncommittedArtifactKey);
            boolean quota = exception.status() == HttpStatus.TOO_MANY_REQUESTS;
            boolean retryable = exception.status() == HttpStatus.SERVICE_UNAVAILABLE;
            transactions.executeWithoutResult(status ->
                    recordFailure(claim, exception.code(), exception.getMessage(), quota, retryable));
            if (retryable && claim.attempt() < MAX_ATTEMPTS) {
                throw new RetryGenerationException("The rate limiter is unavailable; Pub/Sub should retry.");
            }
        } catch (RuntimeException exception) {
            deleteArtifact(uncommittedArtifactKey);
            transactions.executeWithoutResult(status -> recordFailure(
                    claim, "GENERATION_FAILED",
                    "The generation attempt failed unexpectedly. Retry from the application screen.",
                    false, false));
            LOG.error("Generation failed for application {} at version {}.",
                    claim.id(), claim.generationVersion());
        }
    }

    private void deleteArtifact(String objectKey) {
        if (objectKey == null) return;
        try {
            artifacts.delete(objectKey);
        } catch (RuntimeException exception) {
            LOG.error("Could not delete uncommitted preview artifact {}.", objectKey, exception);
        }
    }

    private String parseResumeOnce(Claim claim) {
        List<String> stored = jdbc.query("""
                SELECT parsed_content::text FROM resumes
                WHERE id = ? AND device_id = ? AND parsed_content IS NOT NULL
                """, (rs, row) -> rs.getString(1), claim.resumeId(), claim.deviceId());
        if (!stored.isEmpty()) return validateParsedResume(stored.getFirst());

        if (!reserveDailyProviderCall(claim.resumeText().length())) {
            throw new GenerationFailure("GLOBAL_DAILY_LIMIT", false, true,
                    "The configured daily provider-call or token allowance has been reached. No provider request was made.");
        }
        String parsed = generator.parseResume(claim.resumeText());
        if (parsed == null || parsed.isBlank()
                || parsed.length() > properties.limits().maxGeneratedCharacters()
                || ResumePrivacyGuard.containsObviousContactData(parsed)) {
            throw new GenerationFailure("INVALID_PARSED_RESUME", false, false,
                    "The sanitized resume could not be parsed safely and was not stored.");
        }
        validateParsedResume(parsed);
        List<String> persisted = jdbc.query("""
                UPDATE resumes SET parsed_content = ?::jsonb
                WHERE id = ? AND device_id = ? AND parsed_content IS NULL
                RETURNING parsed_content::text
                """, (rs, row) -> rs.getString(1), parsed, claim.resumeId(), claim.deviceId());
        if (!persisted.isEmpty()) return validateParsedResume(persisted.getFirst());
        String winner = jdbc.queryForObject("""
                SELECT parsed_content::text FROM resumes
                WHERE id = ? AND device_id = ? AND parsed_content IS NOT NULL
                """, String.class, claim.resumeId(), claim.deviceId());
        return validateParsedResume(winner);
    }

    private String validateParsedResume(String parsed) {
        try {
            GeminiClient.GeneratedResume resume =
                    objectMapper.readValue(parsed, GeminiClient.GeneratedResume.class);
            if (resume == null || resume.headline() == null || resume.summary() == null
                    || resume.skills() == null || resume.experience() == null || resume.education() == null
                    || ResumePrivacyGuard.containsObviousContactData(parsed)
                    || parsed.length() > properties.limits().maxGeneratedCharacters()) {
                throw new IllegalArgumentException("Invalid structured resume fields.");
            }
            return parsed;
        } catch (Exception exception) {
            throw new GenerationFailure("INVALID_PARSED_RESUME", false, false,
                    "The saved structured resume could not be validated.");
        }
    }

    private boolean reserveDailyProviderCall(int inputCharacters) {
        int reservedTokens = Math.max(1, (inputCharacters + 3) / 4) + PROMPT_TOKEN_RESERVE + MAX_OUTPUT_TOKENS;
        return !jdbc.query("""
                INSERT INTO global_generation_usage(usage_day, provider_calls, reserved_tokens)
                VALUES (CURRENT_DATE, 1, ?)
                ON CONFLICT (usage_day) DO UPDATE
                    SET provider_calls = global_generation_usage.provider_calls + 1,
                        reserved_tokens = global_generation_usage.reserved_tokens + ?
                    WHERE global_generation_usage.provider_calls < ?
                      AND global_generation_usage.reserved_tokens + ? <= ?
                RETURNING provider_calls
                """, (rs, row) -> rs.getInt(1),
                reservedTokens, reservedTokens,
                properties.limits().maxGlobalGenerationsPerDay(),
                reservedTokens, properties.limits().maxGlobalTokensPerDay()).isEmpty();
    }

    Claim claimLocked(UUID applicationId, int generationVersion) {
        List<Boolean> saved = jdbc.query("""
                SELECT EXISTS (
                    SELECT 1 FROM decisions d
                    JOIN applications a ON a.device_id = d.device_id AND a.job_id = d.job_id
                    WHERE a.id = ? AND d.status = 'SAVED'
                )
                """, (rs, row) -> rs.getBoolean(1), applicationId);
        if (saved.isEmpty() || !saved.getFirst()) {
            jdbc.update("""
                    UPDATE applications SET status = 'CANCELLED', lease_until = NULL,
                        lease_token = NULL, updated_at = now()
                    WHERE id = ? AND generation_version = ? AND status IN ('QUEUED', 'PROCESSING')
                    """, applicationId, generationVersion);
            return null;
        }
        UUID leaseToken = UUID.randomUUID();
        List<Claim> claimed = jdbc.query("""
                UPDATE applications a
                SET status = 'PROCESSING',
                    attempts = attempts + 1,
                    lease_until = now() + interval '6 minutes',
                    lease_token = ?,
                    updated_at = now()
                WHERE a.id = ? AND a.generation_version = ? AND a.attempts < ?
                  AND (a.status = 'QUEUED' OR (a.status = 'PROCESSING' AND a.lease_until < now()))
                  AND EXISTS (
                      SELECT 1 FROM decisions d
                      WHERE d.device_id = a.device_id AND d.job_id = a.job_id AND d.status = 'SAVED'
                  )
                RETURNING a.id, a.device_id, a.job_id, a.resume_id, a.generation_version, a.attempts
                """, (rs, row) -> new Claim(
                rs.getObject("id", UUID.class),
                rs.getObject("device_id", UUID.class),
                rs.getObject("job_id", UUID.class),
                rs.getObject("resume_id", UUID.class),
                rs.getInt("generation_version"),
                rs.getInt("attempts"),
                leaseToken,
                null, null, null, null),
                leaseToken, applicationId, generationVersion, MAX_ATTEMPTS);
        if (claimed.isEmpty()) {
            jdbc.update("""
                    UPDATE applications SET status = 'FAILED',
                        error_code = 'GENERATION_ATTEMPTS_EXHAUSTED',
                        error_message = 'Automatic retries were exhausted. Request an allowed regeneration later.',
                        lease_until = NULL, lease_token = NULL, updated_at = now()
                    WHERE id = ? AND generation_version = ? AND attempts >= ?
                      AND (status = 'QUEUED' OR (status = 'PROCESSING' AND lease_until < now()))
                    """, applicationId, generationVersion, MAX_ATTEMPTS);
            return null;
        }
        Claim key = claimed.getFirst();
        List<Claim> inputs = jdbc.query("""
                SELECT j.title, j.company, j.description, r.sanitized_text
                FROM jobs j CROSS JOIN resumes r
                WHERE j.id = ? AND r.id = ? AND r.device_id = ?
                """, (rs, row) -> key.withInputs(
                rs.getString("title"), rs.getString("company"),
                rs.getString("description"), rs.getString("sanitized_text")),
                key.jobId(), key.resumeId(), key.deviceId());
        if (inputs.isEmpty()) {
            jdbc.update("""
                    UPDATE applications SET status = 'FAILED', error_code = 'GENERATION_INPUT_MISSING',
                        error_message = 'The saved job or resume snapshot is unavailable.',
                        lease_until = NULL, lease_token = NULL, updated_at = now()
                    WHERE id = ? AND lease_token = ?
                    """, applicationId, leaseToken);
            return null;
        }
        return inputs.getFirst();
    }

    boolean persistReady(
            Claim claim, GeminiClient.GeneratedApplication generated, String previewObjectKey) {
        List<String> decisionStates = jdbc.query("""
                SELECT status FROM decisions
                WHERE device_id = ? AND job_id = ?
                FOR UPDATE
                """, (rs, row) -> rs.getString(1), claim.deviceId(), claim.jobId());
        if (decisionStates.isEmpty() || !"SAVED".equals(decisionStates.getFirst())) {
            jdbc.update("""
                    UPDATE applications SET status = 'CANCELLED', error_code = 'DECISION_WITHDRAWN',
                        error_message = 'The saved decision was withdrawn before completion.',
                        lease_until = NULL, lease_token = NULL, updated_at = now()
                    WHERE id = ? AND generation_version = ? AND status = 'PROCESSING'
                      AND lease_token = ?
                    """, claim.id(), claim.generationVersion(), claim.leaseToken());
            return false;
        }
        List<UUID> currentLease = jdbc.query("""
                SELECT id FROM applications
                WHERE id = ? AND generation_version = ? AND status = 'PROCESSING'
                  AND lease_token = ?
                FOR UPDATE
                """, (rs, row) -> rs.getObject(1, UUID.class),
                claim.id(), claim.generationVersion(), claim.leaseToken());
        if (currentLease.isEmpty()) return false;

        String resumeJson;
        try {
            resumeJson = objectMapper.writeValueAsString(generated.resumeContent());
        } catch (Exception exception) {
            throw new IllegalStateException("Could not encode generated resume content.", exception);
        }
        int ownsLease = jdbc.update("""
                INSERT INTO application_versions(
                    application_id, version, resume_content, cover_letter, preview_object_key, is_demo)
                SELECT id, generation_version, ?::jsonb, ?, ?, ?
                FROM applications WHERE id = ? AND generation_version = ?
                  AND status = 'PROCESSING' AND lease_token = ?
                  AND EXISTS (
                      SELECT 1 FROM decisions d
                      WHERE d.device_id = applications.device_id
                        AND d.job_id = applications.job_id AND d.status = 'SAVED'
                  )
                ON CONFLICT (application_id, version) DO NOTHING
                """, resumeJson, generated.coverLetter(), previewObjectKey, generator.demoMode(),
                claim.id(), claim.generationVersion(), claim.leaseToken());
        if (ownsLease != 1) {
            return false;
        }
        int updated = jdbc.update("""
                UPDATE applications SET status = 'READY', error_code = NULL,
                    error_message = NULL, lease_until = NULL, lease_token = NULL, updated_at = now()
                WHERE id = ? AND generation_version = ? AND lease_token = ?
                """, claim.id(), claim.generationVersion(), claim.leaseToken());
        return updated == 1;
    }

    void recordFailure(Claim claim, String code, String message, boolean quota, boolean retryable) {
        String status = quota ? "QUOTA_BLOCKED"
                : retryable && claim.attempt() < MAX_ATTEMPTS ? "QUEUED" : "FAILED";
        jdbc.update("""
                UPDATE applications SET status = ?, error_code = ?, error_message = ?,
                    lease_until = NULL, lease_token = NULL, updated_at = now()
                WHERE id = ? AND generation_version = ? AND lease_token = ?
                """, status, bounded(code, 60), bounded(message, 500),
                claim.id(), claim.generationVersion(), claim.leaseToken());
    }

    public int recoverStale() {
        return transactions.execute(status -> recoverStaleLocked());
    }

    private int recoverStaleLocked() {
        List<StaleApplication> stale = jdbc.query("""
                SELECT id, generation_version, attempts
                FROM applications
                WHERE status = 'PROCESSING' AND lease_until < now()
                ORDER BY lease_until
                LIMIT ?
                FOR UPDATE SKIP LOCKED
                """, (rs, row) -> new StaleApplication(
                rs.getObject("id", UUID.class), rs.getInt("generation_version"), rs.getInt("attempts")),
                MAX_RECOVERY_BATCH);
        int recovered = 0;
        for (StaleApplication item : stale) {
            if (item.attempts() >= MAX_ATTEMPTS) {
                jdbc.update("""
                        UPDATE applications SET status = 'FAILED',
                            error_code = 'GENERATION_ATTEMPTS_EXHAUSTED',
                            error_message = 'Automatic retries were exhausted. Request an allowed regeneration later.',
                            lease_until = NULL, lease_token = NULL, updated_at = now()
                        WHERE id = ?
                        """, item.id());
            } else {
                jdbc.update("""
                        UPDATE applications SET status = 'QUEUED', lease_until = NULL,
                            lease_token = NULL, updated_at = now()
                        WHERE id = ?
                        """, item.id());
                insertOutbox(item.id(), item.generationVersion());
                recovered++;
            }
        }
        return recovered;
    }

    private void insertOutbox(UUID applicationId, int generationVersion) {
        try {
            jdbc.update("""
                    INSERT INTO outbox(event_type, aggregate_id, payload)
                    VALUES ('APPLICATION_GENERATION_REQUESTED', ?, ?::jsonb)
                    """, applicationId,
                    objectMapper.writeValueAsString(Map.of(
                            "applicationId", applicationId.toString(),
                            "generationVersion", generationVersion)));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not queue recovery event.", exception);
        }

    }

    private static String bounded(String value, int maximum) {
        if (value == null) return null;
        return value.length() <= maximum ? value : value.substring(0, maximum);
    }

    private static final int PROMPT_TOKEN_RESERVE = 1024;
    private static final int MAX_OUTPUT_TOKENS = 4096;
    private record StaleApplication(UUID id, int generationVersion, int attempts) {}

    record Claim(
            UUID id, UUID deviceId, UUID jobId, UUID resumeId, int generationVersion,
            int attempt, UUID leaseToken, String jobTitle, String company,
            String jobDescription, String resumeText) {
        Claim withInputs(String title, String companyName, String description, String sanitizedText) {
            return new Claim(id, deviceId, jobId, resumeId, generationVersion, attempt, leaseToken,
                    title, companyName, description, sanitizedText);
        }
    }

    public static class RetryGenerationException extends RuntimeException {
        public RetryGenerationException(String message) { super(message); }
    }
}
