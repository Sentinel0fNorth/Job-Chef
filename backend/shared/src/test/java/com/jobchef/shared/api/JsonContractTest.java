package com.jobchef.shared.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jobchef.shared.api.ApiModels.ApplicationResponse;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class JsonContractTest {
    private final JsonMapper mapper = JsonMapper.builder()
            .changeDefaultPropertyInclusion(inclusion -> inclusion
                    .withValueInclusion(JsonInclude.Include.NON_NULL))
            .build();

    @Test
    void preservesNullableResumeAndIsoTimestampInWaitingApplication() {
        Instant updatedAt = Instant.parse("2026-11-01T08:00:00Z");
        var application = new ApplicationResponse(
                UUID.randomUUID(), UUID.randomUUID(), null, null, "WAITING_FOR_RESUME", 1,
                null, null, null, null, false, false, updatedAt);

        String json = mapper.writeValueAsString(application);

        assertFalse(json.contains("\"resumeVersion\""));
        assertTrue(json.contains("\"updatedAt\":\"2026-11-01T08:00:00Z\""));
        assertEquals(application, mapper.readValue(json, ApplicationResponse.class));
    }

    @Test
    void preservesStructuredGeneratedContentAndCoverLetter() {
        var application = new ApplicationResponse(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 2, "READY", 1,
                null, null, Map.of("headline", "Developer", "summary", "Builds software"),
                "A reviewed draft.", false, true, Instant.parse("2026-11-01T08:00:00Z"));

        String json = mapper.writeValueAsString(application);
        var restored = mapper.readValue(json, ApplicationResponse.class);

        assertEquals(application, restored);
        assertTrue(mapper.readTree(json).path("resumeContent").isObject());
    }
}
