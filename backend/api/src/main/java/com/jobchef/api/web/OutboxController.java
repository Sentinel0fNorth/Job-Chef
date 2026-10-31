package com.jobchef.api.web;

import com.jobchef.api.pubsub.OutboxDispatchService;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.jobchef.shared.error.ApiException;

@RestController
@RequestMapping("/api/v1/internal/outbox")
public class OutboxController {
    private final OutboxDispatchService outbox;
    private final String schedulerServiceAccount;

    public OutboxController(
            OutboxDispatchService outbox,
            @Value("${jobchef.scheduler-service-account:}") String schedulerServiceAccount) {
        this.outbox = outbox;
        this.schedulerServiceAccount = schedulerServiceAccount;
    }

    @PostMapping("/dispatch")
    @ResponseStatus(HttpStatus.OK)
    Map<String, Integer> dispatch(@AuthenticationPrincipal Jwt jwt) {
        String caller = jwt == null ? null : jwt.getClaimAsString("email");
        if (schedulerServiceAccount.isBlank() || !schedulerServiceAccount.equals(caller)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "INTERNAL_CALLER_FORBIDDEN",
                    "The caller is not authorized to dispatch the outbox.");
        }
        var result = outbox.dispatch();
        return Map.of("examined", result.examined(), "published", result.published(), "failed", result.failed());
    }
}
