# ConcurrentHashMap Store

The in-memory database is a `ConcurrentHashMap<String, StoredValue>` inside `Store.java`.

## Why ConcurrentHashMap?

- **Read-heavy workload** — `GET` is the hot path; `get()` is lock-free at the bucket level.
- **Multi-writer** — Many connections increment or set keys concurrently without a global `synchronized` block.

## Operations

| Method | Mechanism | Thread safety |
|--------|-----------|---------------|
| `get` | `map.get` + lazy expiry remove | Safe |
| `set` | `map.put` | Safe |
| `setWithTtlMillis` | `map.put` with expiry record | Safe |
| `incr` | `map.compute` | Atomic RMW |
| `delete` | `map.remove` | Safe |
| `exists` | delegates to `get` | Safe |
| `size` | `map.size()` | Approximate; may include not-yet-swept expired keys |
| `purgeExpired` | iterate + conditional remove | Safe |

## Atomic INCR

`INCR` must be correct when 16+ threads hammer the same key:

```java
map.compute(key, (k, existing) -> {
    long base = 0L;
    long expiresAt = StoredValue.NO_EXPIRY;
    if (existing != null && !existing.isExpired()) {
        base = Long.parseLong(existing.value());
        expiresAt = existing.expiresAt();
    }
    long next = Math.addExact(base, 1L);
    return new StoredValue(Long.toString(next), expiresAt);
});
```

`compute` ensures the read-modify-write is atomic for a single key.

## Contention test

`StoreTest.incrIsAtomicUnderContention`:

- 16 threads × 500 increments = **8000** final value
- Uses `CountDownLatch` for simultaneous start

Also verified end-to-end: `ServerIntegrationTest.concurrentIncrConverges`.

## Lazy delete on read

Expired entries use `map.remove(key, v)` — remove only if value unchanged — to avoid deleting a re-inserted key (`Store.java:31-33`).

## Limitations (prototype scope)

- Single database (no `SELECT`)
- No persistence (RDB/AOF)
- `size()` does not subtract expired keys until sweep — document in `StoreTest.sizeIncludesUnexpiredOnly`
