package com.jobchef.api.web;

import com.jobchef.shared.api.ApiModels.DecisionRequest;
import com.jobchef.shared.api.ApiModels.SyncRequest;
import com.jobchef.shared.api.ApiModels.SyncResponse;
import com.jobchef.shared.service.DecisionSyncService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/decisions")
public class DecisionController {
    private final DecisionSyncService sync;

    public DecisionController(DecisionSyncService sync) {
        this.sync = sync;
    }

    @PostMapping("/sync")
    SyncResponse sync(@Valid @RequestBody SyncRequest body, HttpServletRequest request) {
        return sync.synchronize(RequestContext.device(request).deviceId(), body.decisions());
    }

    @PostMapping
    SyncResponse single(@Valid @RequestBody DecisionRequest body, HttpServletRequest request) {
        return sync.synchronize(RequestContext.device(request).deviceId(), java.util.List.of(body));
    }
}
