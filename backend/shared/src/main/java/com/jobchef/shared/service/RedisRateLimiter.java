package com.jobchef.shared.service;

import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import com.jobchef.shared.error.ApiException;

@Service
public class RedisRateLimiter {
    private static final DefaultRedisScript<Long> INCREMENT_WITH_TTL = new DefaultRedisScript<>("""
            local count = redis.call('INCR', KEYS[1])
            if count == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end
            return count
            """, Long.class);
    private static final DefaultRedisScript<Long> ACQUIRE_LEASE = new DefaultRedisScript<>("""
            redis.call('ZREMRANGEBYSCORE', KEYS[1], '-inf', ARGV[1])
            local added = redis.call('ZADD', KEYS[1], 'NX', ARGV[2], ARGV[3])
            local count = redis.call('ZCARD', KEYS[1])
            if count > tonumber(ARGV[4]) then
                redis.call('ZREM', KEYS[1], ARGV[3])
                return 0
            end
            redis.call('PEXPIRE', KEYS[1], ARGV[5])
            return added
            """, Long.class);
    private static final DefaultRedisScript<Long> RELEASE_LEASE = new DefaultRedisScript<>(
            "return redis.call('ZREM', KEYS[1], ARGV[1])", Long.class);

    private final StringRedisTemplate redis;

    public RedisRateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void requireCapacity(String key, long maximum, Duration window) {
        final Long count;
        try {
            count = redis.execute(INCREMENT_WITH_TTL, java.util.List.of(key),
                    Long.toString(window.toSeconds()));
        } catch (RuntimeException ex) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "RATE_LIMIT_UNAVAILABLE",
                    "Rate-limit storage is unavailable; this request was not processed.");
        }
        if (count == null || count > maximum) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED",
                    "The configured request limit has been reached. Try again after the limit window.");
        }
    }

    public boolean acquireLease(String key, String token, int maximum, Duration leaseDuration) {
        try {
            Long acquired = redis.execute(
                    ACQUIRE_LEASE,
                    java.util.List.of(key),
                    Long.toString(System.currentTimeMillis()),
                    Long.toString(System.currentTimeMillis() + leaseDuration.toMillis()),
                    token,
                    Integer.toString(maximum),
                    Long.toString(leaseDuration.toMillis()));
            return acquired != null && acquired == 1L;
        } catch (RuntimeException ex) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "RATE_LIMIT_UNAVAILABLE",
                    "Rate-limit storage is unavailable; this request was not processed.");
        }
    }

    public void releaseLease(String key, String token) {
        try {
            redis.execute(RELEASE_LEASE, java.util.List.of(key), token);
        } catch (RuntimeException ex) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "RATE_LIMIT_UNAVAILABLE",
                    "Rate-limit storage is unavailable; generation capacity could not be released.");
        }
    }
}
