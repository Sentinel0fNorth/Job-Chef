package com.jobchef.worker.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FixtureApplicationGeneratorTest {
    private final FixtureApplicationGenerator generator = new FixtureApplicationGenerator();

    @Test
    void demoOutputUsesOnlySkillsFoundInTheSanitizedResumeAndIsMarkedAsDemo() {
        var result = generator.generate(
                "Android Engineer", "Synthetic Co", "Kotlin and Android",
                "Kotlin and Android experience. Java is not listed.");

        assertTrue(generator.demoMode());
        assertEquals(java.util.List.of("Android", "Kotlin"), result.resumeContent().skills());
        assertTrue(result.coverLetter().contains("synthetic demonstration output"));
        assertFalse(result.resumeContent().summary().contains("years of experience"));
    }
}
