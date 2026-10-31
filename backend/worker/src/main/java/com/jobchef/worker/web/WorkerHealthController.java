package com.jobchef.worker.web;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WorkerHealthController {
    @GetMapping("/api/v1/health")
    Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
