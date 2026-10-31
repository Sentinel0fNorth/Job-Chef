package com.jobchef.shared.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ResumePrivacyGuardTest {
    @Test
    void detectsEmailPhoneAndUnredactedAddress() {
        assertTrue(ResumePrivacyGuard.containsObviousContactData("Email: candidate@example.com"));
        assertTrue(ResumePrivacyGuard.containsObviousContactData("Phone: +91 98765 43210"));
        assertTrue(ResumePrivacyGuard.containsObviousContactData("Address: 10 Main Street"));
    }

    @Test
    void acceptsReviewedRedactionsAndOrdinaryResumeDates() {
        String text = """
                [CONTACT REDACTED]
                Email: [CONTACT REDACTED]
                Phone: [CONTACT REDACTED]
                Location: [CONTACT REDACTED]
                Experience: 2020 - 2024
                """;

        assertFalse(ResumePrivacyGuard.containsObviousContactData(text));
    }
}
