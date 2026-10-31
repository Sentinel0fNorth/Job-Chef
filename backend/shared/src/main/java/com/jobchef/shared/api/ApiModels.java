package com.jobchef.shared.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ApiModels {
    private ApiModels() {}

    public record DeviceRegistration(String enrollmentCode) {}
    public record DeviceCredentials(UUID deviceId, String accessToken, Instant expiresAt) {}
    public record ResumeRequest(
            @NotBlank @Size(max = 80) String clientKey,
            @NotBlank @Size(max = 120) String displayName,
            @NotBlank @Size(max = 50000) String sanitizedText,
            boolean active) {}
    public record ResumeResponse(UUID id, int version, String displayName, boolean active, Instant createdAt) {}
    public record SearchRequest(
            @NotBlank @Size(max = 120) String role,
            @Size(max = 120) String location,
            @Size(max = 300) String keywords,
            String country,
            String workMode,
            @Min(1) @Max(20) Integer limit) {}
    public record JobResponse(
            UUID id, String title, String company, String location, String description,
            Double salaryMin, Double salaryMax, String currency, String redirectUrl,
            List<String> skills, String workMode, int matchScore, String matchMethod) {}
    public record SearchResponse(List<JobResponse> jobs, String attribution, Instant fetchedAt) {}
    public record DecisionRequest(
            UUID jobId,
            @NotBlank @Size(max = 80) String idempotencyKey,
            @Min(1) long revision,
            @NotNull DecisionStatus status) {}
    public enum DecisionStatus { SAVED, REJECTED, UNSEEN }
    public record SyncRequest(@Size(max = 100) List<DecisionRequest> decisions) {}
    public record SyncResponse(int accepted, int stale, List<String> errors) {}
    public record ApplicationResponse(
            UUID id, UUID jobId, UUID resumeId, Integer resumeVersion, String status, int generationVersion,
            String errorCode, String errorMessage, Object resumeContent,
            String coverLetter, boolean demoMode, boolean previewAvailable, Instant updatedAt) {}
    public record ErrorResponse(String code, String message, String requestId) {}
}
