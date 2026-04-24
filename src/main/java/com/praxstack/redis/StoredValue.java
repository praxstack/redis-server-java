package com.praxstack.redis;

import java.time.Instant;

/**
 * Immutable record representing a stored value with optional absolute expiry.
 *
 * @param value     the stored bytes (typically UTF-8)
 * @param expiresAt epoch-millis at which the value should be considered expired,
 *                  or {@code -1} if the key is persistent.
 */
public record StoredValue(String value, long expiresAt) {

    public static final long NO_EXPIRY = -1L;

    public boolean hasExpiry() {
        return expiresAt != NO_EXPIRY;
    }

    public boolean isExpired(long nowMillis) {
        return hasExpiry() && nowMillis >= expiresAt;
    }

    public boolean isExpired() {
        return isExpired(Instant.now().toEpochMilli());
    }

    public static StoredValue of(String value) {
        return new StoredValue(value, NO_EXPIRY);
    }

    public static StoredValue withTtlMillis(String value, long ttlMillis) {
        return new StoredValue(value, Instant.now().toEpochMilli() + ttlMillis);
    }
}
