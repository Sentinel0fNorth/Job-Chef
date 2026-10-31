package com.jobchef.shared.service;

import com.jobchef.shared.api.ApiModels.DeviceCredentials;
import com.jobchef.shared.error.ApiException;
import com.jobchef.shared.security.TokenHasher;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeviceIdentityService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final JdbcTemplate jdbc;
    private final RedisRateLimiter limiter;

    public DeviceIdentityService(JdbcTemplate jdbc, RedisRateLimiter limiter) {
        this.jdbc = jdbc;
        this.limiter = limiter;
    }

    @Transactional
    public DeviceCredentials register(String enrollmentCode) {
        limiter.requireCapacity(
                "jobchef:enrollment:registrations:" + java.time.LocalDate.now(java.time.ZoneOffset.UTC),
                20,
                Duration.ofDays(2));
        if (enrollmentCode == null || enrollmentCode.isBlank() || enrollmentCode.length() > 200) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_ENROLLMENT", "A valid enrollment code is required.");
        }
        int consumed = jdbc.update("""
                UPDATE enrollment_codes
                SET remaining_uses = remaining_uses - 1
                WHERE token_hash = ? AND remaining_uses > 0 AND expires_at > now()
                """, TokenHasher.sha256(enrollmentCode));
        if (consumed != 1) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_ENROLLMENT", "The enrollment code is invalid, expired, or exhausted.");
        }

        String token = newAccessToken();
        UUID deviceId = jdbc.queryForObject("""
                INSERT INTO devices(token_hash, token_expires_at)
                VALUES (?, now() + interval '90 days')
                RETURNING id
                """, UUID.class, TokenHasher.sha256(token));
        return new DeviceCredentials(deviceId, token, Instant.now().plusSeconds(90L * 24 * 60 * 60));
    }

    @Transactional
    public DeviceCredentials refresh(UUID deviceId, String tokenHash) {
        String nextToken = newAccessToken();
        int changed = jdbc.update("""
                UPDATE devices SET token_hash = ?, token_expires_at = now() + interval '90 days'
                WHERE id = ? AND token_hash = ? AND revoked_at IS NULL AND token_expires_at > now()
                """, TokenHasher.sha256(nextToken), deviceId, tokenHash);
        if (changed != 1) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "DEVICE_CREDENTIAL_INVALID", "This device credential is invalid or revoked.");
        }
        return new DeviceCredentials(deviceId, nextToken, Instant.now().plusSeconds(90L * 24 * 60 * 60));
    }

    public void revoke(UUID deviceId, String tokenHash) {
        int changed = jdbc.update("""
                UPDATE devices SET revoked_at = now()
                WHERE id = ? AND token_hash = ? AND revoked_at IS NULL
                """, deviceId, tokenHash);
        if (changed != 1) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "DEVICE_CREDENTIAL_INVALID", "This device credential is invalid or revoked.");
        }
    }

    private static String newAccessToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
