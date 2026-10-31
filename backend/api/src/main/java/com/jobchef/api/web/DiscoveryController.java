package com.jobchef.api.web;

import com.jobchef.shared.api.ApiModels.SearchRequest;
import com.jobchef.shared.api.ApiModels.SearchResponse;
import com.jobchef.shared.service.JobDiscoveryService;
import com.jobchef.shared.service.RedisRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/discovery")
public class DiscoveryController {
    private final JobDiscoveryService discovery;
    private final RedisRateLimiter limiter;

    public DiscoveryController(JobDiscoveryService discovery, RedisRateLimiter limiter) {
        this.discovery = discovery;
        this.limiter = limiter;
    }

    @PostMapping("/search")
    SearchResponse search(@Valid @RequestBody SearchRequest request, HttpServletRequest httpRequest) {
        var device = RequestContext.device(httpRequest);
        limiter.requireCapacity("jobchef:search:device:" + device.deviceId(), 10, Duration.ofMinutes(1));
        limiter.requireCapacity("jobchef:search:global", 100, Duration.ofDays(1));
        return discovery.search(device.deviceId(), request);
    }
}
