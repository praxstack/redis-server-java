# TTL Eviction (Hybrid Model)

Redis keys can expire. This implementation uses **lazy + active** eviction.

## Stored value

`StoredValue` is a record `(String value, long expiresAt)` (`StoredValue.java`).

- `expiresAt == -1` → persistent key (`StoredValue.NO_EXPIRY`)
- `withTtlMillis(value, ttl)` sets `expiresAt = now + ttl` (`StoredValue.java:32-34`)

## Lazy expiry (read path)

On every `Store.get()`:

1. Fetch from `ConcurrentHashMap`
2. If `StoredValue.isExpired()`, remove entry and return empty
3. Otherwise return value

(`Store.java:27-36`)

Correctness: clients never see expired data on `GET`, even before background sweep.

## Active expiry (background)

`ExpiryManager` runs `store.purgeExpired()` every **100 ms** on a single daemon thread (`ExpiryManager.java:20-36`).

`purgeExpired()` iterates entries, removes those past `expiresAt` (`Store.java:80-89`).

Purpose: keys written but never read would otherwise linger in memory until touched.

## SET with TTL

`CommandDispatcher.handleSet` supports `PX` (ms) and `EX` (seconds) (`CommandDispatcher.java:80-88`).

`Store.setWithTtlMillis` stores absolute expiry (`Store.java:42-44`).

## INCR preserves TTL

`incr` uses `compute()` and copies `expiresAt` from existing value (`Store.java:52-64`). Test: `StoreTest.incrPreservesTtl`.

## Trade-offs

| Approach | Pros | Cons |
|----------|------|------|
| Lazy only | Zero background CPU | Memory leak for write-only keys |
| Active only | Bounded memory | Stale reads between sweeps |
| Hybrid (this repo) | Correct reads + bounded memory | Sweep thread + iterator cost |

## Tests

- `StoreTest.lazyExpiryRemovesKeyOnGet`
- `StoreTest.purgeExpiredRemovesStaleKeys`
- `CommandDispatcherTest.setWithPxExpiresKey`
- `ServerIntegrationTest.pxExpiryRemovesKey`
