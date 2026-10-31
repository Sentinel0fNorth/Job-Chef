package com.jobchef.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobchef.shared.service.DeviceAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class ApiSecurityConfiguration {
    @Bean
    @Order(1)
    @Profile("live")
    SecurityFilterChain internalSecurity(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/api/v1/internal/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                .oauth2ResourceServer(resource -> resource.jwt(Customizer.withDefaults()))
                .build();
    }

    @Bean
    @Order(1)
    @Profile("demo")
    SecurityFilterChain localInternalSecurity(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/api/v1/internal/**")
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain deviceSecurity(
            HttpSecurity http, JdbcTemplate jdbc, ObjectMapper objectMapper) throws Exception {
        DeviceAuthenticationFilter filter = new DeviceAuthenticationFilter(jdbc, objectMapper);
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health/**", "/api/v1/auth/register").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
