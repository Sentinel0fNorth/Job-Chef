package com.jobchef.shared.service;

import com.jobchef.shared.api.ApiModels.ResumeRequest;
import com.jobchef.shared.api.ApiModels.ResumeResponse;
import com.jobchef.shared.config.BackendProperties;
import com.jobchef.shared.error.ApiException;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class ResumeService {
    private static final Logger LOG = LoggerFactory.getLogger(ResumeService.class);
    private final JdbcTemplate jdbc;
    private final BackendProperties properties;
    private final PreviewArtifactStore artifacts;

    public ResumeService(JdbcTemplate jdbc, BackendProperties properties, PreviewArtifactStore artifacts) {
        this.jdbc = jdbc;
        this.properties = properties;
        this.artifacts = artifacts;
    }

    @Transactional
    public ResumeResponse add(UUID deviceId, ResumeRequest request) {
        requireTextLimit(request.sanitizedText());
        jdbc.queryForObject("SELECT id FROM devices WHERE id = ? FOR UPDATE", UUID.class, deviceId);
        List<StoredResume> existing = jdbc.query("""
                SELECT id, version, display_name, sanitized_text, active, created_at
                FROM resumes WHERE device_id = ? AND client_key = ?
                """, (rs, row) -> new StoredResume(
                new ResumeResponse(
                        rs.getObject("id", UUID.class),
                        rs.getInt("version"),
                        rs.getString("display_name"),
                        rs.getBoolean("active"),
                        rs.getTimestamp("created_at").toInstant()),
                rs.getString("display_name"),
                rs.getString("sanitized_text")), deviceId, request.clientKey());
        if (!existing.isEmpty()) {
            StoredResume prior = existing.getFirst();
            if (!prior.displayName().equals(request.displayName().trim())
                    || !prior.sanitizedText().equals(request.sanitizedText())) {
                throw new ApiException(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_REUSED",
                        "This resume upload key was already used for different content.");
            }
            return prior.response();
        }

        Integer nextVersion = jdbc.queryForObject(
                "SELECT COALESCE(MAX(version), 0) + 1 FROM resumes WHERE device_id = ?",
                Integer.class, deviceId);
        if (request.active()) {
            jdbc.update("UPDATE resumes SET active = FALSE WHERE device_id = ?", deviceId);
        }
        ResumeResponse created = jdbc.queryForObject("""
                INSERT INTO resumes(device_id, client_key, version, display_name, sanitized_text, active)
                VALUES (?, ?, ?, ?, ?, ?)
                RETURNING id, version, display_name, active, created_at
                """, ResumeService::mapResume,
                deviceId, request.clientKey(), nextVersion, request.displayName().trim(),
                request.sanitizedText(), request.active());
        if (request.active()) queueWaitingApplications(deviceId, created.id(), created.version());
        return created;
    }

    public List<ResumeResponse> list(UUID deviceId) {
        return jdbc.query("""
                SELECT id, version, display_name, active, created_at FROM resumes
                WHERE device_id = ? ORDER BY version DESC
                """, ResumeService::mapResume, deviceId);
    }

    @Transactional
    public void activate(UUID deviceId, UUID resumeId) {
        jdbc.queryForObject("SELECT id FROM devices WHERE id = ? FOR UPDATE", UUID.class, deviceId);
        List<Integer> owned = jdbc.query(
                "SELECT 1 FROM resumes WHERE id = ? AND device_id = ?",
                (rs, row) -> rs.getInt(1), resumeId, deviceId);
        if (owned.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "RESUME_NOT_FOUND",
                    "This resume does not belong to the registered device.");
        }
        jdbc.update("UPDATE resumes SET active = FALSE WHERE device_id = ? AND active = TRUE", deviceId);
        jdbc.update("UPDATE resumes SET active = TRUE WHERE device_id = ? AND id = ?", deviceId, resumeId);
        int version = jdbc.queryForObject("SELECT version FROM resumes WHERE id = ?", Integer.class, resumeId);
        queueWaitingApplications(deviceId, resumeId, version);
    }

    @Transactional
    public void delete(UUID deviceId, UUID resumeId) {
        jdbc.queryForObject("SELECT id FROM devices WHERE id = ? FOR UPDATE", UUID.class, deviceId);
        List<Boolean> owned = jdbc.query(
                "SELECT active FROM resumes WHERE id = ? AND device_id = ? FOR UPDATE",
                (rs, row) -> rs.getBoolean(1), resumeId, deviceId);
        if (owned.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "RESUME_NOT_FOUND",
                    "This resume does not belong to the registered device.");
        }

        List<UUID> applicationIds = jdbc.query(
                "SELECT id FROM applications WHERE resume_id = ? FOR UPDATE",
                (rs, row) -> rs.getObject(1, UUID.class), resumeId);
        List<UUID> savedApplicationIds = jdbc.query("""
                SELECT a.id FROM applications a
                JOIN decisions d ON d.device_id = a.device_id AND d.job_id = a.job_id
                WHERE a.resume_id = ? AND d.status = 'SAVED'
                FOR UPDATE OF a
                """, (rs, row) -> rs.getObject(1, UUID.class), resumeId);
        List<String> objectKeys = new java.util.ArrayList<>();
        for (UUID applicationId : applicationIds) {
            objectKeys.addAll(jdbc.query("""
                    SELECT preview_object_key FROM application_versions
                    WHERE application_id = ? AND preview_object_key IS NOT NULL
                    """, (rs, row) -> rs.getString(1), applicationId));
        }
        ResumeResponse replacement = jdbc.query("""
                SELECT id, version, display_name, active, created_at FROM resumes
                WHERE device_id = ? AND id <> ? ORDER BY active DESC, version DESC LIMIT 1
                """, ResumeService::mapResume, deviceId, resumeId).stream().findFirst().orElse(null);
        for (UUID applicationId : applicationIds) {
            jdbc.update("DELETE FROM outbox WHERE aggregate_id = ?", applicationId);
        }
        jdbc.update("DELETE FROM application_versions WHERE application_id IN "
                + "(SELECT id FROM applications WHERE resume_id = ?)", resumeId);
        jdbc.update("""
                DELETE FROM applications a WHERE a.resume_id = ?
                    AND NOT EXISTS (
                        SELECT 1 FROM decisions d
                        WHERE d.device_id = a.device_id AND d.job_id = a.job_id AND d.status = 'SAVED'
                    )
                """, resumeId);
        if (replacement == null) {
            jdbc.update("""
                    UPDATE applications SET resume_id = NULL, resume_version = 0,
                        status = 'WAITING_FOR_RESUME', attempts = 0, lease_until = NULL,
                        lease_token = NULL, error_code = NULL, error_message = NULL, updated_at = now()
                    WHERE resume_id = ? AND EXISTS (
                        SELECT 1 FROM decisions d
                        WHERE d.device_id = applications.device_id AND d.job_id = applications.job_id
                            AND d.status = 'SAVED'
                    )
                    """, resumeId);
        } else {
            jdbc.update("""
                    UPDATE applications SET resume_id = ?, resume_version = ?, status = 'QUEUED',
                        attempts = 0, lease_until = NULL, lease_token = NULL,
                        error_code = NULL, error_message = NULL, updated_at = now()
                    WHERE resume_id = ? AND EXISTS (
                        SELECT 1 FROM decisions d
                        WHERE d.device_id = applications.device_id AND d.job_id = applications.job_id
                            AND d.status = 'SAVED'
                    )
                    """, replacement.id(), replacement.version(), resumeId);
            for (UUID applicationId : savedApplicationIds) {
                int generationVersion = jdbc.queryForObject(
                        "SELECT generation_version FROM applications WHERE id = ?", Integer.class, applicationId);
                insertOutbox(applicationId, generationVersion);
            }
        }
        jdbc.update("DELETE FROM resumes WHERE device_id = ? AND id = ?", deviceId, resumeId);
        deleteArtifactsAfterCommit(objectKeys);

        if (owned.getFirst()) {
            if (replacement != null) {
                jdbc.update("UPDATE resumes SET active = FALSE WHERE device_id = ?", deviceId);
                jdbc.update("UPDATE resumes SET active = TRUE WHERE id = ?", replacement.id());
                queueWaitingApplications(deviceId, replacement.id(), replacement.version());
            }
        }
    }

    private void deleteArtifactsAfterCommit(List<String> objectKeys) {
        if (objectKeys.isEmpty()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                for (String objectKey : objectKeys) {
                    try {
                        artifacts.delete(objectKey);
                    } catch (RuntimeException exception) {
                        LOG.error("Could not delete private preview artifact {} after resume deletion.",
                                objectKey, exception);
                    }
                }
            }
        });
    }

    private void requireTextLimit(String text) {
        if (text == null || text.isBlank() || text.length() > properties.limits().maxResumeCharacters()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "RESUME_TEXT_LIMIT",
                    "The extracted resume text is empty or exceeds the supported character limit.");
        }
        if (ResumePrivacyGuard.containsObviousContactData(text)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "RESUME_CONTACT_DATA",
                    "Remove apparent contact details on-device, review the sanitized text, and upload it again.");
        }
    }

    private void queueWaitingApplications(UUID deviceId, UUID resumeId, int resumeVersion) {
        while (true) {
            List<WaitingApplication> waiting = jdbc.query("""
                    SELECT id, generation_version FROM applications
                    WHERE device_id = ? AND status = 'WAITING_FOR_RESUME'
                    ORDER BY created_at LIMIT 20
                    FOR UPDATE SKIP LOCKED
                    """, (rs, row) -> new WaitingApplication(
                    rs.getObject("id", UUID.class), rs.getInt("generation_version")), deviceId);
            if (waiting.isEmpty()) return;
            for (WaitingApplication application : waiting) {
                jdbc.update("""
                        UPDATE applications SET resume_id = ?, resume_version = ?, status = 'QUEUED',
                            updated_at = now()
                        WHERE id = ? AND status = 'WAITING_FOR_RESUME'
                        """, resumeId, resumeVersion, application.id());
                insertOutbox(application.id(), application.generationVersion());
            }
        }
    }

    private void insertOutbox(UUID applicationId, int generationVersion) {
        jdbc.update("""
                INSERT INTO outbox(event_type, aggregate_id, payload)
                VALUES ('APPLICATION_GENERATION_REQUESTED', ?,
                        jsonb_build_object('applicationId', ?, 'generationVersion', ?))
                """, applicationId, applicationId.toString(), generationVersion);
    }

    private static ResumeResponse mapResume(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
        return new ResumeResponse(
                rs.getObject("id", UUID.class),
                rs.getInt("version"),
                rs.getString("display_name"),
                rs.getBoolean("active"),
                rs.getTimestamp("created_at").toInstant());
    }

    private record StoredResume(ResumeResponse response, String displayName, String sanitizedText) {}
    private record WaitingApplication(UUID id, int generationVersion) {}
}
