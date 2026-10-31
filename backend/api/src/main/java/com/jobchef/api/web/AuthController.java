package com.jobchef.api.web;

import com.jobchef.shared.api.ApiModels.DeviceCredentials;
import com.jobchef.shared.api.ApiModels.DeviceRegistration;
import com.jobchef.shared.service.DeviceIdentityService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final DeviceIdentityService identity;

    public AuthController(DeviceIdentityService identity) {
        this.identity = identity;
    }

    @PostMapping("/register")
    DeviceCredentials register(@Valid @RequestBody DeviceRegistration request) {
        return identity.register(request.enrollmentCode());
    }

    @PostMapping("/refresh")
    DeviceCredentials refresh(HttpServletRequest request) {
        var context = RequestContext.device(request);
        return identity.refresh(context.deviceId(), context.tokenHash());
    }

    @PostMapping("/revoke")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void revoke(HttpServletRequest request) {
        var context = RequestContext.device(request);
        identity.revoke(context.deviceId(), context.tokenHash());
    }
}
