package com.jobchef.shared.service;

import tools.jackson.databind.ObjectMapper;
import com.jobchef.shared.api.ApiModels.ApplicationResponse;
import com.jobchef.shared.config.BackendProperties;
import com.jobchef.shared.error.ApiException;
import java.util.List;
import java.util.Map;
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
public class ApplicationService {
    private static final Logger LOG = LoggerFactory.getLogger(ApplicationService.class);
    private final JdbcTemplate jdbc;
    private final BackendProperties properties;
    private final ObjectMapper objectMapper;
    private final PreviewArtifactStore artifacts;

    public ApplicationService(
            JdbcTemplate jdbc, BackendProperties properties, ObjectMapper objectMapper,
            PreviewArtifactStore artifacts) {
        this.jdbc = jdbc;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.artifacts = artifacts;
    }

    public ApplicationResponse byJob(UUID deviceId, UUID jobId) {
        return read("""
                WHERE a.device_id = ? AND a.job_id = ?
                """, deviceId, jobId);
    }

    public ApplicationResponse byId(UUID deviceId, UUID applicationId) {
        return read("WHERE a.device_id = ? AND a.id = ?", deviceId, applicationId);
    }

    private ApplicationResponse read(String predicate, Object... args) {
        List<ApplicationResponse> responses = jdbc.query("""
                SELECT a.id, a.job_id, a.resume_id, a.resume_version, a.status,
                       a.generation_version, a.error_code, a.error_message,
                       v.resume_content, v.cover_letter, v.is_demo,
                       v.preview_object_key,
                       (v.preview_object_key IS NOT NULL
                           AND v.created_at > now() - interval '90 days') AS preview_available,
                       a.updated_at
                FROM applications a
                LEFT JOIN application_versions v
                    ON v.application_id = a.id AND v.version = a.generation_version
                """ + predicate + " LIMIT 1",
                (rs, row) -> new ApplicationResponse(
                        rs.getObject("id", UUID.class),
                        rs.getObject("job_id", UUID.class),
                        rs.getObject("resume_id", UUID.class),
                        rs.getObject("resume_version", Integer.class),
                        rs.getString("status"),
                        rs.getInt("generation_version"),
                        rs.getString("error_code"),
                        rs.getString("error_message"),
                        rs.getString("resume_content") == null
                                ? null : parseJson(rs.getString("resume_content")),
                        rs.getString("cover_letter"),
                        rs.getBoolean("is_demo"),
                        rs.getBoolean("preview_available"),
                        rs.getTimestamp("updated_at").toInstant()),
                args);
        if (responses.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "APPLICATION_NOT_FOUND",
                    "This application does not belong to the registered device or does not exist.");
        }
        return responses.getFirst();
    }

    @Transactional
    public void regenerate(UUID deviceId, UUID jobId) {
        record Current(UUID id, String status, int generationVersion) {}
        List<Current> current = jdbc.query("""
                SELECT id, status, generation_version FROM applications
                WHERE device_id = ? AND job_id = ? FOR UPDATE
                """, (rs, row) -> new Current(
                rs.getObject("id", UUID.class), rs.getString("status"), rs.getInt("generation_version")),
                deviceId, jobId);
        if (current.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "APPLICATION_NOT_FOUND",
                    "Save this job and finish its first generation before regenerating.");
        }
        Current application = current.getFirst();
        if (!"READY".equals(application.status()) && !"FAILED".equals(application.status())) {
            throw new ApiException(HttpStatus.CONFLICT, "APPLICATION_NOT_REGENERABLE",
                    "This application is not in a state that can be regenerated.");
        }
        if (application.generationVersion() >= 100) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "GENERATION_VERSION_LIMIT",
                    "The maximum number of application versions has been reached.");
        }
        jdbc.update("DELETE FROM application_regeneration_usage WHERE usage_day < CURRENT_DATE");
        int perJobReservation = jdbc.update("""
                INSERT INTO application_regeneration_usage(device_id, job_id, usage_day, regeneration_count)
                VALUES (?, ?, CURRENT_DATE, 1)
                ON CONFLICT (device_id, job_id, usage_day) DO UPDATE
                    SET regeneration_count = application_regeneration_usage.regeneration_count + 1
                WHERE application_regeneration_usage.regeneration_count < ?
                """, deviceId, jobId, properties.limits().maxRegenerationsPerApplicationPerDay());
        if (perJobReservation != 1) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "REGENERATION_DAILY_LIMIT",
                    "This saved job has already used its daily regeneration allowance.");
        }
        var reservation = jdbc.query("""
                UPDATE applications
                SET generation_version = generation_version + 1,
                    status = 'QUEUED',
                    error_code = NULL,
                    error_message = NULL,
                    attempts = 0,
                    lease_until = NULL,
                    updated_at = now()
                WHERE id = ?
                RETURNING generation_version
                """, (rs, row) -> rs.getInt(1), application.id());
        if (reservation.isEmpty()) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "REGENERATION_DAILY_LIMIT",
                    "This saved job has already used its daily regeneration allowance.");
        }
        int globalCount = jdbc.update("""
                INSERT INTO quota_usage(device_id, usage_day, regeneration_count)
                VALUES (?, CURRENT_DATE, 1)
                ON CONFLICT (device_id, usage_day) DO UPDATE
                    SET regeneration_count = quota_usage.regeneration_count + 1
                WHERE quota_usage.regeneration_count < ?
                """, deviceId, properties.limits().maxGlobalGenerationsPerDay());
        if (globalCount != 1) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "GLOBAL_GENERATION_LIMIT",
                    "The configured daily generation allowance has been reached.");
        }
        insertOutbox(application.id(), reservation.getFirst());
    }

    @Transactional
    public void delete(UUID deviceId, UUID jobId) {
        jdbc.query("""
                SELECT status FROM decisions WHERE device_id = ? AND job_id = ? FOR UPDATE
                """, (rs, row) -> rs.getString(1), deviceId, jobId);
        jdbc.query("""
                SELECT id FROM applications WHERE device_id = ? AND job_id = ? FOR UPDATE
                """, (rs, row) -> rs.getObject(1, UUID.class), deviceId, jobId);
        List<String> objectKeys = jdbc.query("""
                SELECT v.preview_object_key
                FROM applications a JOIN application_versions v ON v.application_id = a.id
                WHERE a.device_id = ? AND a.job_id = ? AND v.preview_object_key IS NOT NULL
                """, (rs, row) -> rs.getString(1), deviceId, jobId);
        jdbc.update("""
                DELETE FROM outbox
                WHERE aggregate_id IN (
                    SELECT id FROM applications WHERE device_id = ? AND job_id = ?
                )
                """, deviceId, jobId);
        int deleted = jdbc.update(
                "DELETE FROM applications WHERE device_id = ? AND job_id = ?", deviceId, jobId);
        if (deleted != 1) {
            throw new ApiException(HttpStatus.NOT_FOUND, "APPLICATION_NOT_FOUND",
                    "This application does not belong to the registered device or does not exist.");
        }
        jdbc.update("DELETE FROM decisions WHERE device_id = ? AND job_id = ?", deviceId, jobId);
        deleteArtifactsAfterCommit(objectKeys);
    }

    public String previewObjectKey(UUID deviceId, UUID jobId) {
        List<String> keys = jdbc.query("""
                SELECT v.preview_object_key
                FROM applications a JOIN application_versions v
                  ON v.application_id = a.id AND v.version = a.generation_version
                WHERE a.device_id = ? AND a.job_id = ? AND a.status = 'READY'
                  AND v.preview_object_key IS NOT NULL
                  AND v.created_at > now() - interval '90 days'
                """, (rs, row) -> rs.getString(1), deviceId, jobId);
        if (keys.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "PREVIEW_NOT_FOUND",
                    "A sanitized preview is not available for this application.");
        }
        return keys.getFirst();
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
                        LOG.error("Could not delete private preview artifact {} after application deletion.",
                                objectKey, exception);
                    }
                }
            }
        });
    }

    private Object parseJson(String json) {
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception exception) {
            throw new IllegalStateException("Stored application output is not valid JSON.", exception);
        }
    }

    private void insertOutbox(UUID applicationId, int version) {
        try {
            String payload = objectMapper.writeValueAsString(
                    Map.of("applicationId", applicationId.toString(), "generationVersion", version));
            jdbc.update("""
                    INSERT INTO outbox(event_type, aggregate_id, payload)
                    VALUES ('APPLICATION_GENERATION_REQUESTED', ?, ?::jsonb)
                    """, applicationId, payload);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not serialize the generation event.", exception);
        }
    }
}
