package com.jobchef.shared.service;

import tools.jackson.databind.ObjectMapper;
import com.jobchef.shared.api.ApiModels.ErrorResponse;
import com.jobchef.shared.security.TokenHasher;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class DeviceAuthenticationFilter extends OncePerRequestFilter {
    public static final String DEVICE_ATTRIBUTE = DeviceRequestContext.class.getName();
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public DeviceAuthenticationFilter(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/api/v1/auth/register")
                || path.startsWith("/actuator/health")
                || path.startsWith("/api/v1/internal/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            reject(response, HttpServletResponse.SC_UNAUTHORIZED, "DEVICE_CREDENTIAL_REQUIRED",
                    "A registered device bearer token is required.");
            return;
        }
        String token = authorization.substring("Bearer ".length());
        if (token.isBlank() || token.length() > 100) {
            reject(response, HttpServletResponse.SC_UNAUTHORIZED, "DEVICE_CREDENTIAL_INVALID",
                    "The registered device credential is invalid.");
            return;
        }
        String hash = TokenHasher.sha256(token);
        var matches = jdbc.query("""
                SELECT id FROM devices
                WHERE token_hash = ? AND revoked_at IS NULL AND token_expires_at > now()
                """, (rs, row) -> rs.getObject("id", UUID.class), hash);
        if (matches.size() != 1) {
            reject(response, HttpServletResponse.SC_UNAUTHORIZED, "DEVICE_CREDENTIAL_INVALID",
                    "The registered device credential is invalid or revoked.");
            return;
        }
        UUID deviceId = matches.getFirst();
        var context = new DeviceRequestContext(deviceId, hash);
        request.setAttribute(DEVICE_ATTRIBUTE, context);
        var authentication = new UsernamePasswordAuthenticationToken(deviceId, null, java.util.List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        try {
            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void reject(HttpServletResponse response, int status, String code, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        objectMapper.writeValue(response.getOutputStream(),
                new ErrorResponse(code, message, UUID.randomUUID().toString()));
    }
}
