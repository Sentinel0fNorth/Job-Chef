package com.jobchef.shared.service;

import java.util.UUID;

public record DeviceRequestContext(UUID deviceId, String tokenHash) {}
