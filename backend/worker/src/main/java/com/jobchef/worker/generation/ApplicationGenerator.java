package com.jobchef.worker.generation;

public interface ApplicationGenerator {
    default String parseResume(String sanitizedResume) {
        return sanitizedResume;
    }

    GeminiClient.GeneratedApplication generate(
            String jobTitle, String company, String jobText, String sanitizedResume);

    boolean demoMode();
}
