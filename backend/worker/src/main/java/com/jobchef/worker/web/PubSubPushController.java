package com.jobchef.worker.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobchef.shared.error.ApiException;
import com.jobchef.worker.generation.GenerationWorkService;
import com.jobchef.worker.generation.GenerationWorkService.RetryGenerationException;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal")
public class PubSubPushController {
    private final GenerationWorkService work;
    private final ObjectMapper objectMapper;
    private final Environment environment;
    private final String pushServiceAccount;

    public PubSubPushController(
            GenerationWorkService work,
            ObjectMapper objectMapper,
            Environment environment,
            @Value("${jobchef.pubsub-push-service-account:}") String pushServiceAccount) {
        this.work = work;
        this.objectMapper = objectMapper;
        this.environment = environment;
        this.pushServiceAccount = pushServiceAccount;
    }

    @PostMapping("/pubsub/generate")
    public void generate(
            @RequestBody PushEnvelope envelope,
            @AuthenticationPrincipal Jwt jwt) {
        verifyCaller(jwt);
        if (envelope.message() == null || envelope.message().data() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PUBSUB_ENVELOPE",
                    "The Pub/Sub push envelope is missing message data.");
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(envelope.message().data());
            GenerationEvent event = objectMapper.readValue(decoded, GenerationEvent.class);
            if (event.applicationId() == null || event.generationVersion() < 1) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_GENERATION_EVENT",
                        "The generation event is invalid.");
            }
            work.process(event.applicationId(), event.generationVersion());
        } catch (IllegalArgumentException | java.io.IOException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_GENERATION_EVENT",
                    "The Pub/Sub message did not contain a valid generation event.");
        } catch (RetryGenerationException exception) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "GENERATION_RETRY",
                    "The generation attempt should be retried by Pub/Sub.");
        }
    }

    private void verifyCaller(Jwt jwt) {
        if (isDemo() && jwt == null) return;
        String email = jwt == null ? null : jwt.getClaimAsString("email");
        if (pushServiceAccount.isBlank() || !pushServiceAccount.equals(email)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "INTERNAL_CALLER_FORBIDDEN",
                    "The caller is not authorized to invoke the generation worker.");
        }
    }

    private boolean isDemo() {
        return java.util.Arrays.asList(environment.getActiveProfiles()).contains("demo");
    }

    public record PushEnvelope(PushMessage message, String subscription) {}
    public record PushMessage(String data, Map<String, String> attributes, String messageId) {}
    public record GenerationEvent(UUID applicationId, int generationVersion) {}
}
