package com.praxstack.redis;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe key/value store backed by {@link ConcurrentHashMap}.
 *
 * <p>All reads go through a lazy-expiry check, so expired keys are treated as
 * missing even before the background {@link ExpiryManager} sweeps them. This
 * gives correctness without contending on a global lock.
 *
 * <p>Writes use {@link ConcurrentHashMap#put(Object, Object)} and
 * {@link ConcurrentHashMap#compute(Object, java.util.function.BiFunction)}
 * for atomic read-modify-write semantics (used by {@code INCR}).
 */
public final class Store {

    private final ConcurrentHashMap<String, StoredValue> map = new ConcurrentHashMap<>();

    /**
     * Lazy-expiry aware GET. If the key is present but expired, the entry is
     * removed and {@link Optional#empty()} is returned.
     */
    public Optional<String> get(String key) {
        StoredValue v = map.get(key);
        if (v == null) return Optional.empty();
        if (v.isExpired()) {
            // best-effort cleanup — remove only if still the same entry
            map.remove(key, v);
            return Optional.empty();
        }
        return Optional.of(v.value());
    }

    public void set(String key, String value) {
        map.put(key, StoredValue.of(value));
    }

    public void setWithTtlMillis(String key, String value, long ttlMillis) {
        map.put(key, StoredValue.withTtlMillis(value, ttlMillis));
    }

    /**
     * Atomically increment the integer value of a key.
     *
     * @return the new value
     * @throws NumberFormatException if the existing value is not an integer
     */
    public long incr(String key) {
        StoredValue updated = map.compute(key, (k, existing) -> {
            long base = 0L;
            long expiresAt = StoredValue.NO_EXPIRY;
            if (existing != null && !existing.isExpired()) {
                base = Long.parseLong(existing.value());
                expiresAt = existing.expiresAt();
            }
            long next = Math.addExact(base, 1L);
            return new StoredValue(Long.toString(next), expiresAt);
        });
        return Long.parseLong(updated.value());
    }

    public boolean exists(String key) {
        return get(key).isPresent();
    }

    public boolean delete(String key) {
        StoredValue removed = map.remove(key);
        return removed != null && !removed.isExpired();
    }

    public int size() {
        return map.size();
    }

    /** Used by {@link ExpiryManager} to purge keys that have crossed their TTL. */
    int purgeExpired() {
        long now = Instant.now().toEpochMilli();
        int removed = 0;
        for (Map.Entry<String, StoredValue> entry : map.entrySet()) {
            StoredValue v = entry.getValue();
            if (v.isExpired(now) && map.remove(entry.getKey(), v)) {
                removed++;
            }
        }
        return removed;
    }
}
