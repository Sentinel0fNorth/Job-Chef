package com.jobchef.shared.service;

import java.util.List;

public interface JobProvider {
    List<ExternalJob> search(
            String country, String role, String location, String keywords, int limit);

    default String attribution() {
        return "Job listings provided by Adzuna.";
    }

    record ExternalJob(
            String providerId,
            String title,
            String company,
            String location,
            String description,
            Double salaryMin,
            Double salaryMax,
            String currency,
            String redirectUrl) {}
}
