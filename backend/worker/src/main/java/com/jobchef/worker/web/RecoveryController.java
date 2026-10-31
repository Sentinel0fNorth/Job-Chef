package com.jobchef.worker.web;

import com.jobchef.shared.error.ApiException;
import com.jobchef.worker.generation.GenerationWorkService;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/recovery")
public class RecoveryController {
    private final GenerationWorkService work;
    private final Environment environment;
    private final String schedulerServiceAccount;

    public RecoveryController(
            GenerationWorkService work,
            Environment environment,
            @Value("${jobchef.scheduler-service-account:}") String schedulerServiceAccount) {
        this.work = work;
        this.environment = environment;
        this.schedulerServiceAccount = schedulerServiceAccount;
    }

    @PostMapping
    Map<String, Integer> recover(@AuthenticationPrincipal Jwt jwt) {
        if (!isDemo()) {
            String email = jwt == null ? null : jwt.getClaimAsString("email");
            if (schedulerServiceAccount.isBlank() || !schedulerServiceAccount.equals(email)) {
                throw new ApiException(HttpStatus.FORBIDDEN, "INTERNAL_CALLER_FORBIDDEN",
                        "The caller is not authorized to recover stale generation leases.");
            }
        }
        return Map.of("requeued", work.recoverStale());
    }

    private boolean isDemo() {
        return java.util.Arrays.asList(environment.getActiveProfiles()).contains("demo");
    }
}
