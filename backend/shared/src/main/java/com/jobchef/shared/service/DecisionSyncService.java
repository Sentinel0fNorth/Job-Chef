package com.jobchef.shared.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobchef.shared.api.ApiModels.DecisionRequest;
import com.jobchef.shared.api.ApiModels.DecisionStatus;
import com.jobchef.shared.api.ApiModels.SyncResponse;
import com.jobchef.shared.config.BackendProperties;
import com.jobchef.shared.error.ApiException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DecisionSyncService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final BackendProperties properties;

    public DecisionSyncService(
            JdbcTemplate jdbc, ObjectMapper objectMapper, BackendProperties properties) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Transactional
    public SyncResponse synchronize(UUID deviceId, List<DecisionRequest> decisions) {
        if (decisions == null || decisions.size() > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_SYNC_BATCH", "A sync batch may contain at most 100 decisions.");
        }
        int accepted = 0;
        int stale = 0;
        List<String> errors = new ArrayList<>();
        for (DecisionRequest decision : decisions) {
            try {
                Result result = apply(deviceId, decision);
                if (result == Result.ACCEPTED) accepted++;
                if (result == Result.STALE) stale++;
            } catch (ApiException exception) {
                String jobId = decision == null || decision.jobId() == null
                        ? "unknown" : decision.jobId().toString();
                errors.add(jobId + ":" + exception.code());
            }
        }
        return new SyncResponse(accepted, stale, List.copyOf(errors));
    }

    private Result apply(UUID deviceId, DecisionRequest decision) {
        if (decision == null || decision.jobId() == null || decision.status() == null
                || decision.idempotencyKey() == null || decision.idempotencyKey().isBlank()
                || decision.revision() < 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_DECISION", "The decision is missing an id, status, idempotency key, or positive revision.");
        }
        var priorKey = jdbc.query("""
                SELECT job_id, status, revision FROM decisions
                WHERE device_id = ? AND idempotency_key = ?
                """, (rs, row) -> new PriorDecision(
                        rs.getObject("job_id", UUID.class),
                        rs.getString("status"),
                        rs.getLong("revision")), deviceId, decision.idempotencyKey());
        if (!priorKey.isEmpty()) {
            PriorDecision prior = priorKey.getFirst();
            if (prior.jobId().equals(decision.jobId())
                    && prior.status().equals(decision.status().name())
                    && prior.revision() == decision.revision()) return Result.ACCEPTED;
            throw new ApiException(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_REUSED",
                    "This idempotency key was already used for a different decision.");
        }
        Integer jobExists = jdbc.queryForObject(
                "SELECT count(*) FROM jobs WHERE id = ?", Integer.class, decision.jobId());
        if (jobExists == null || jobExists != 1) {
            throw new ApiException(HttpStatus.NOT_FOUND, "JOB_NOT_FOUND", "This job is no longer available.");
        }
        var updated = jdbc.query("""
                INSERT INTO decisions(device_id, job_id, status, revision, idempotency_key)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (device_id, job_id) DO UPDATE SET
                    status = EXCLUDED.status,
                    revision = EXCLUDED.revision,
                    idempotency_key = EXCLUDED.idempotency_key,
                    updated_at = now()
                WHERE decisions.revision < EXCLUDED.revision
                RETURNING job_id
                """, (rs, row) -> rs.getObject("job_id", UUID.class),
                deviceId, decision.jobId(), decision.status().name(),
                decision.revision(), decision.idempotencyKey());
        if (updated.isEmpty()) return Result.STALE;

        if (decision.status() == DecisionStatus.SAVED) {
            queueIfEligible(deviceId, decision.jobId());
        } else {
            jdbc.update("""
                    UPDATE applications SET status = 'CANCELLED', updated_at = now(),
                        error_code = 'DECISION_WITHDRAWN',
                        error_message = 'The saved decision was withdrawn before completion.'
                    WHERE device_id = ? AND job_id = ? AND status IN ('WAITING_FOR_RESUME', 'QUEUED')
                    """, deviceId, decision.jobId());
        }
        return Result.ACCEPTED;
    }

    private void queueIfEligible(UUID deviceId, UUID jobId) {
        List<ExistingApplication> existing = jdbc.query("""
                SELECT id, status, generation_version, error_code
                FROM applications WHERE device_id = ? AND job_id = ?
                FOR UPDATE
                """, (rs, row) -> new ExistingApplication(
                rs.getObject("id", UUID.class),
                rs.getString("status"),
                rs.getInt("generation_version"),
                rs.getString("error_code")), deviceId, jobId);
        if (!existing.isEmpty()) {
            requeueExisting(deviceId, existing.getFirst());
            return;
        }

        ResumeRef activeResume = findActiveResume(deviceId);
        UUID resumeId = activeResume == null ? null : activeResume.id();
        int resumeVersion = activeResume == null ? 0 : activeResume.version();
        boolean reserved = reserveDailyApplication(deviceId);
        String initialStatus;
        if (!reserved) {
            initialStatus = "QUOTA_BLOCKED";
        } else if (resumeId == null) {
            initialStatus = "WAITING_FOR_RESUME";
        } else {
            initialStatus = "QUEUED";
        }

        UUID applicationId = jdbc.queryForObject("""
                INSERT INTO applications(
                    device_id, job_id, resume_id, resume_version, status, error_code, error_message)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (device_id, job_id) DO UPDATE SET updated_at = now()
                RETURNING id
                """, UUID.class, deviceId, jobId, resumeId, resumeVersion, initialStatus,
                reserved ? null : "DAILY_APPLICATION_LIMIT",
                reserved ? null : "The daily application limit was reached. Retry after the quota resets.");
        if ("QUEUED".equals(initialStatus)) {
            insertOutbox(applicationId, 1);
        }
    }

    private void requeueExisting(UUID deviceId, ExistingApplication existing) {
        boolean retryDailyApplicationLimit = "QUOTA_BLOCKED".equals(existing.status())
                && "DAILY_APPLICATION_LIMIT".equals(existing.errorCode());
        if (retryDailyApplicationLimit && !reserveDailyApplication(deviceId)) return;
        if (!"CANCELLED".equals(existing.status()) && !"QUOTA_BLOCKED".equals(existing.status())) return;

        ResumeRef resume = findActiveResume(deviceId);
        String nextStatus = resume == null ? "WAITING_FOR_RESUME" : "QUEUED";
        jdbc.update("""
                UPDATE applications
                SET resume_id = ?, resume_version = ?, status = ?, attempts = 0,
                    lease_until = NULL, lease_token = NULL, error_code = NULL,
                    error_message = NULL, updated_at = now()
                WHERE id = ?
                """, resume == null ? null : resume.id(), resume == null ? 0 : resume.version(),
                nextStatus, existing.id());
        if ("QUEUED".equals(nextStatus)) {
            insertOutbox(existing.id(), existing.generationVersion());
        }
    }

    private ResumeRef findActiveResume(UUID deviceId) {
        List<ResumeRef> activeResume = jdbc.query("""
                SELECT id, version FROM resumes WHERE device_id = ? AND active = TRUE
                ORDER BY version DESC LIMIT 1
                """, (rs, row) -> new ResumeRef(
                        rs.getObject("id", UUID.class), rs.getInt("version")), deviceId);
        return activeResume.isEmpty() ? null : activeResume.getFirst();
    }

    private boolean reserveDailyApplication(UUID deviceId) {
        return !jdbc.query("""
                INSERT INTO quota_usage(device_id, usage_day, new_applications)
                VALUES (?, CURRENT_DATE, 1)
                ON CONFLICT (device_id, usage_day) DO UPDATE
                    SET new_applications = quota_usage.new_applications + 1
                    WHERE quota_usage.new_applications < ?
                RETURNING new_applications
                """, (rs, row) -> rs.getInt(1), deviceId,
                properties.limits().maxNewApplicationsPerDay()).isEmpty();
    }

    private void insertOutbox(UUID applicationId, int version) {
        try {
            String payload = objectMapper.writeValueAsString(
                    java.util.Map.of("applicationId", applicationId.toString(), "generationVersion", version));
            jdbc.update("""
                    INSERT INTO outbox(event_type, aggregate_id, payload)
                    VALUES ('APPLICATION_GENERATION_REQUESTED', ?, ?::jsonb)
                    """, applicationId, payload);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not serialize the generation event.", exception);
        }
    }

    private enum Result { ACCEPTED, STALE }
    private record PriorDecision(UUID jobId, String status, long revision) {}
    private record ResumeRef(UUID id, int version) {}
    private record ExistingApplication(UUID id, String status, int generationVersion, String errorCode) {}
}
