package com.jobchef.feature.resume.presentation.vault

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResumeTextSanitizerTest {
    @Test
    fun extractsLocalContactsAndRemovesThemFromReviewedText() {
        val source = """
            Asha Sharma
            email: asha@example.com
            Phone: +91 98765 43210
            Location: Jaipur, Rajasthan
            Kotlin developer with experience building Android applications.
        """.trimIndent()

        val sanitized = ResumeTextSanitizer.extractContacts(source)

        assertEquals("Asha Sharma", sanitized.name)
        assertEquals("asha@example.com", sanitized.email)
        assertEquals("+91 98765 43210", sanitized.phone)
        assertEquals("Jaipur, Rajasthan", sanitized.location)
        assertFalse(sanitized.sanitizedText.contains("Asha Sharma"))
        assertFalse(sanitized.sanitizedText.contains("asha@example.com"))
        assertFalse(sanitized.sanitizedText.contains("+91 98765 43210"))
        assertFalse(sanitized.sanitizedText.contains("Jaipur, Rajasthan"))
        assertTrue(sanitized.sanitizedText.contains("Kotlin developer"))
    }

    @Test
    fun replacesAdditionalEmailAndPhoneMatchesInReviewedText() {
        val source = "Contact one@example.com or two@example.org at 98765 43210 or 87654 32109."

        val sanitized = ResumeTextSanitizer.extractContacts(source).sanitizedText

        assertFalse(sanitized.contains("one@example.com"))
        assertFalse(sanitized.contains("two@example.org"))
        assertFalse(sanitized.contains("98765 43210"))
        assertFalse(sanitized.contains("87654 32109"))
        assertEquals(4, Regex("\\[CONTACT REDACTED\\]").findAll(sanitized).count())
    }
}
