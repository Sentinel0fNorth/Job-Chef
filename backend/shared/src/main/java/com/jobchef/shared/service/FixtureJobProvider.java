package com.jobchef.shared.service;

import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "jobchef.mode", havingValue = "demo")
public class FixtureJobProvider implements JobProvider {
    @Override
    public String attribution() {
        return "Synthetic local demo data; these are not live listings.";
    }

    @Override
    public List<ExternalJob> search(
            String country, String role, String location, String keywords, int limit) {
        List<ExternalJob> fixtures = List.of(
                new ExternalJob("demo-android-01", "Android Software Engineer",
                        "Example Mobility Labs", "Jaipur, India",
                        "Build an Android application using Kotlin, Jetpack Compose, Room, and REST APIs. Hybrid work.",
                        800000.0, 1200000.0, "INR",
                        "https://adzuna.com/"),
                new ExternalJob("demo-backend-02", "Junior Backend Engineer",
                        "Sample Systems", "Remote",
                        "Develop Java and Spring services backed by PostgreSQL and Redis. Remote role.",
                        700000.0, 1100000.0, "INR",
                        "https://adzuna.com/"),
                new ExternalJob("demo-platform-03", "Platform Engineering Intern",
                        "Practice Cloud", "Bengaluru, India",
                        "Learn Docker, Kubernetes, SQL, and cloud deployment with a mentor.",
                        null, null, "INR",
                        "https://adzuna.com/"));
        String roleQuery = role.trim().toLowerCase();
        String keywordQuery = keywords.trim().toLowerCase();
        return fixtures.stream()
                .filter(job -> {
                    String listing = (job.title() + " " + job.description()).toLowerCase();
                    return (roleQuery.isBlank() || listing.contains(roleQuery))
                            && (keywordQuery.isBlank() || listing.contains(keywordQuery));
                })
                .limit(limit)
                .toList();
    }
}
