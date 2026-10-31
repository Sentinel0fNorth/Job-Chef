package com.jobchef.shared.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.jobchef.shared.service.AdzunaProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({BackendProperties.class, AdzunaProperties.class})
public class SharedConfiguration {}
