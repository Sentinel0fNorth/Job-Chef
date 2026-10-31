package com.jobchef.worker.generation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "jobchef.mode", havingValue = "demo")
public class FixtureApplicationGenerator implements ApplicationGenerator {
    private static final Set<String> SKILLS = Set.of(
            "Java", "Kotlin", "Python", "TypeScript", "JavaScript", "Spring",
            "Android", "React", "PostgreSQL", "Redis", "Docker", "Kubernetes", "SQL");

    @Override
    public GeminiClient.GeneratedApplication generate(
            String jobTitle, String company, String jobText, String sanitizedResume) {
        List<String> skills = new ArrayList<>();
        for (String skill : SKILLS) {
            if (sanitizedResume.toLowerCase(Locale.ROOT).contains(skill.toLowerCase(Locale.ROOT))) {
                skills.add(skill);
            }
        }
        skills.sort(String::compareTo);
        var resume = new GeminiClient.GeneratedResume(
                jobTitle,
                "Local demonstration draft for " + company
                        + ". Review and edit against the original resume before use.",
                List.copyOf(skills),
                List.of(),
                List.of());
        return new GeminiClient.GeneratedApplication(
                resume,
                "Local demo cover letter for " + company + ". This is synthetic demonstration output, not AI-generated content.");
    }

    @Override
    public boolean demoMode() {
        return true;
    }
}
