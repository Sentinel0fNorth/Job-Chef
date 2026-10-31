package com.jobchef.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jobchef")
public record BackendProperties(
        Limits limits) {

    public record Limits(
            int maxSearchResults,
            int maxNewApplicationsPerDay,
            int maxRegenerationsPerApplicationPerDay,
            int maxGlobalGenerationsPerDay,
            int maxResumeCharacters,
            int maxJobDescriptionCharacters,
            int maxGenerationInputCharacters,
            int maxGeneratedCharacters,
            int maxConcurrentGenerations,
            int maxOutboxBatchSize,
            int maxGlobalTokensPerDay) {}
}
