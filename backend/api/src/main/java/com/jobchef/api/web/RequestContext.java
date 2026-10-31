package com.jobchef.api.web;

import com.jobchef.shared.service.DeviceAuthenticationFilter;
import com.jobchef.shared.service.DeviceRequestContext;
import jakarta.servlet.http.HttpServletRequest;

public final class RequestContext {
    private RequestContext() {}

    public static DeviceRequestContext device(HttpServletRequest request) {
        Object value = request.getAttribute(DeviceAuthenticationFilter.DEVICE_ATTRIBUTE);
        if (value instanceof DeviceRequestContext context) return context;
        throw new IllegalStateException("Authenticated device request context is missing.");
    }
}
