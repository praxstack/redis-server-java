package com.praxstack.redis;

import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

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
     * Conditional SET used by {@code SET} NX/XX. Returns {@code true} if the
     * write was applied.
     *
     * @param ttlMillis {@code null} means no expiry (or replace without TTL)
     * @param nx        only set if the key is missing or expired
     * @param xx        only set if the key currently exists
     */
    public boolean setConditional(String key, String value, Long ttlMillis, boolean nx, boolean xx) {
        StoredValue neu = ttlMillis == null
                ? StoredValue.of(value)
                : StoredValue.withTtlMillis(value, ttlMillis);
        if (!nx && !xx) {
            map.put(key, neu);
            return true;
        }
        boolean[] applied = {false};
        map.compute(key, (k, existing) -> {
            boolean present = existing != null && !existing.isExpired();
            if (nx && present) {
                return existing;
            }
            if (xx && !present) {
                return existing;
            }
            applied[0] = true;
            return neu;
        });
        return applied[0];
    }

    /**
     * Atomically increment the integer value of a key.
     *
     * @return the new value
     * @throws NumberFormatException if the existing value is not an integer
     */
    public long incr(String key) {
        return incrBy(key, 1L);
    }

    /**
     * Atomically add {@code delta} to the integer value of a key.
     *
     * @return the new value
     * @throws NumberFormatException if the existing value is not an integer
     * @throws ArithmeticException   if the result overflows a signed 64-bit integer
     */
    public long incrBy(String key, long delta) {
        StoredValue updated = map.compute(key, (k, existing) -> {
            long base = 0L;
            long expiresAt = StoredValue.NO_EXPIRY;
            if (existing != null && !existing.isExpired()) {
                base = Long.parseLong(existing.value());
                expiresAt = existing.expiresAt();
            }
            long next = Math.addExact(base, delta);
            return new StoredValue(Long.toString(next), expiresAt);
        });
        return Long.parseLong(updated.value());
    }

    /**
     * Remaining TTL in milliseconds. {@code -2} if missing/expired, {@code -1} if
     * the key exists with no timeout.
     */
    public long pttl(String key) {
        StoredValue v = map.get(key);
        if (v == null) return -2L;
        if (v.isExpired()) {
            map.remove(key, v);
            return -2L;
        }
        if (!v.hasExpiry()) return -1L;
        return Math.max(0L, v.expiresAt() - Instant.now().toEpochMilli());
    }

    public long ttlSeconds(String key) {
        long ms = pttl(key);
        if (ms < 0) return ms;
        return ms / 1000L;
    }

    /**
     * Set a TTL on an existing key. Returns {@code false} if the key is missing.
     * A non-positive TTL deletes the key (Redis EXPIRE ≤ 0 behavior).
     */
    public boolean expireMillis(String key, long ttlMillis) {
        StoredValue v = map.get(key);
        if (v == null || v.isExpired()) {
            if (v != null) map.remove(key, v);
            return false;
        }
        if (ttlMillis <= 0) {
            map.remove(key, v);
            return true;
        }
        return map.replace(key, v, StoredValue.withTtlMillis(v.value(), ttlMillis));
    }

    /** Remove expiry from a key. Returns {@code false} if missing or already persistent. */
    public boolean persist(String key) {
        StoredValue v = map.get(key);
        if (v == null || v.isExpired() || !v.hasExpiry()) {
            if (v != null && v.isExpired()) map.remove(key, v);
            return false;
        }
        return map.replace(key, v, StoredValue.of(v.value()));
    }

    /**
     * Append {@code suffix} to the key (creating it if missing). Returns the new
     * UTF-8 byte length. Preserves an existing TTL.
     */
    public int append(String key, String suffix) {
        StoredValue updated = map.compute(key, (k, existing) -> {
            if (existing == null || existing.isExpired()) {
                return StoredValue.of(suffix);
            }
            return new StoredValue(existing.value() + suffix, existing.expiresAt());
        });
        return updated.value().getBytes(StandardCharsets.UTF_8).length;
    }

    public int strlen(String key) {
        return get(key).map(s -> s.getBytes(StandardCharsets.UTF_8).length).orElse(0);
    }

    /** Atomic GET + DEL. Empty if missing or expired. */
    public Optional<String> getDel(String key) {
        StoredValue v = map.remove(key);
        if (v == null || v.isExpired()) return Optional.empty();
        return Optional.of(v.value());
    }

    public boolean exists(String key) {
        return get(key).isPresent();
    }

    public boolean delete(String key) {
        StoredValue removed = map.remove(key);
        return removed != null && !removed.isExpired();
    }

    public int size() {
        return liveSize();
    }

    /** Count of keys that have not expired. */
    public int liveSize() {
        long now = Instant.now().toEpochMilli();
        int n = 0;
        for (StoredValue v : map.values()) {
            if (!v.isExpired(now)) n++;
        }
        return n;
    }

    public void flush() {
        map.clear();
    }

    /** Redis TYPE: {@code string} if present, {@code none} if missing/expired. */
    public String type(String key) {
        return exists(key) ? "string" : "none";
    }

    /** Glob match (`*` / `?`) over live keys. */
    public List<String> keys(String pattern) {
        Pattern compiled = Pattern.compile(globToRegex(pattern == null ? "" : pattern));
        List<String> out = new ArrayList<>();
        long now = Instant.now().toEpochMilli();
        for (Map.Entry<String, StoredValue> e : map.entrySet()) {
            if (e.getValue().isExpired(now)) continue;
            if (compiled.matcher(e.getKey()).matches()) {
                out.add(e.getKey());
            }
        }
        return out;
    }

    static String globToRegex(String pattern) {
        StringBuilder re = new StringBuilder();
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            switch (c) {
                case '*' -> re.append(".*");
                case '?' -> re.append('.');
                default -> {
                    if ("\\.[]{}()+-^$|".indexOf(c) >= 0) {
                        re.append('\\');
                    }
                    re.append(c);
                }
            }
        }
        return re.toString();
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
