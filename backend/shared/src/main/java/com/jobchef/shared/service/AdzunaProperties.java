package com.jobchef.shared.service;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "adzuna")
public record AdzunaProperties(
        String appId,
        String appKey,
        String defaultCountry,
        List<String> supportedCountries,
        String baseUrl) {}
