# Plan 002: Add Store unit tests including INCR contention

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat cfe2170..HEAD -- src/main/java/com/praxstack/redis/Store.java src/test/`

## Status

- **Priority**: P1
- **Effort**: M
- **Risk**: LOW
- **Depends on**: none
- **Category**: tests
- **Planned at**: commit `cfe2170`, 2026-08-29

## Why this matters

`Store` holds all key/value semantics (lazy expiry, atomic INCR, delete). It is tested only indirectly via `CommandDispatcherTest` and integration tests. Direct unit tests give faster feedback on TTL edge cases and INCR atomicity without spinning up TCP. README originally promised these tests; this plan delivers them.

## Current state

- `src/main/java/com/praxstack/redis/Store.java` — ConcurrentHashMap-backed store with `get`, `set`, `setWithTtlMillis`, `incr`, `exists`, `delete`, `size`, `purgeExpired`.
- `src/main/java/com/praxstack/redis/StoredValue.java` — immutable value + expiry record.
- Test pattern exemplar: `src/test/java/com/praxstack/redis/CommandDispatcherTest.java` — JUnit 5, package-private test class, no Spring.

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Run Store tests only | `mvn test -Dtest=StoreTest -q` | exit 0, all pass |
| Full suite | `mvn test -q` | exit 0 |

## Steps

### Step 1 — Create `StoreTest.java`

Create `src/test/java/com/praxstack/redis/StoreTest.java` following the style of `CommandDispatcherTest`:

Required test methods (minimum):

1. `getReturnsEmptyForMissingKey`
2. `setThenGetReturnsValue`
3. `lazyExpiryRemovesKeyOnGet` — set TTL via `setWithTtlMillis(key, val, 50)`, sleep 80ms, assert `get` empty
4. `deleteReturnsTrueOnlyForExistingKey`
5. `existsDelegatesToGet` — expired key returns false
6. `incrFromZeroReturnsOne`
7. `incrPreservesTtl` — set key with TTL, incr, verify value increments and key still expires
8. `incrIsAtomicUnderContention` — 16 threads × 500 `incr` calls on same key; assert final value == 8000
9. `purgeExpiredRemovesStaleKeys` — insert expired entries, call `purgeExpired()`, assert removed count
10. `sizeIncludesUnexpiredOnly` — document current behavior OR assert after purge; if `size()` counts expired keys, add a comment referencing Plan 005/keyspace module

For `incrIsAtomicUnderContention`, use `Executors.newFixedThreadPool(16)` and `CountDownLatch` to start threads simultaneously (pattern similar to `ServerIntegrationTest#concurrentIncrConverges` but calling `store.incr` directly).

### Step 2 — Run tests

```bash
mvn test -Dtest=StoreTest -q
mvn test -q
```

### Step 3 — Update README (optional cross-plan)

If Plan 001 is not done, add `StoreTest` back to README test inventory accurately. If Plan 001 already removed false claims, update README to list the new class.

## STOP conditions

- If `StoreTest.java` already exists with ≥8 tests, stop and report — only add missing cases.
- If `incrIsAtomicUnderContention` fails intermittently, stop — investigate Store.compute logic before weakening the test.

## Out of scope

- Changing `Store` implementation (unless a test reveals a definite bug — then stop and file a separate bug plan)
- Refactoring ExpiryManager

## Done criteria

- [ ] `StoreTest.java` exists with ≥10 test methods
- [ ] `incrIsAtomicUnderContention` passes reliably (run `mvn test -Dtest=StoreTest#incrIsAtomicUnderContention` three times)
- [ ] `mvn test -q` passes
