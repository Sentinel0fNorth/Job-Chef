package com.jobchef.core.network.model

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class JsonContractTest {
    private val gson = Gson()

    @Test
    fun decodesWaitingApplicationWithOmittedNullableFields() {
        val application = gson.fromJson(
            """
            {
              "id":"application-1","jobId":"job-1","status":"WAITING_FOR_RESUME",
              "generationVersion":1,"demoMode":false,"previewAvailable":false,
              "updatedAt":"2026-11-01T08:00:00Z"
            }
            """.trimIndent(),
            ApplicationResponseDto::class.java
        )

        assertNull(application.resumeId)
        assertNull(application.resumeVersion)
        assertNull(application.resumeContent)
        assertEquals("2026-11-01T08:00:00Z", application.updatedAt)
        assertFalse(application.previewAvailable)
    }

    @Test
    fun roundTripsStructuredResumeForOfflineStorageAndExport() {
        val resume = ResumeContentDto(
            headline = "Developer",
            summary = "Builds software",
            skills = listOf("Kotlin"),
            experience = listOf(
                ExperienceDto("Example employer", "Developer", "2025-2026", listOf("Built an app"))
            ),
            education = listOf(EducationDto("Degree", "Example college", "2026"))
        )

        assertEquals(resume, gson.fromJson(gson.toJson(resume), ResumeContentDto::class.java))
    }
}
