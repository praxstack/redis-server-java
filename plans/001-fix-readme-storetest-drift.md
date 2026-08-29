# Plan 001: Fix README StoreTest documentation drift

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat cfe2170..HEAD -- README.md`
> If README.md changed since this plan was written, compare the "Current state"
> excerpts against the live file before proceeding.

## Status

- **Priority**: P1
- **Effort**: S
- **Risk**: LOW
- **Depends on**: none
- **Category**: docs
- **Planned at**: commit `cfe2170`, 2026-08-29

## Why this matters

The README claims a `StoreTest` class with 10 tests (including `incrIsAtomicUnderContention`) exists, but the repository only has four test classes and no `StoreTest.java`. This misleads contributors and breaks trust in the documented verification story. Fixing the docs aligns README with reality until Plan 002 adds the missing tests.

## Current state

- `README.md` — project documentation; lines 75 and 157 reference non-existent `StoreTest`.
- Existing test classes:
  - `src/test/java/com/praxstack/redis/CommandDispatcherTest.java`
  - `src/test/java/com/praxstack/redis/RespEncoderTest.java`
  - `src/test/java/com/praxstack/redis/RespParserTest.java`
  - `src/test/java/com/praxstack/redis/ServerIntegrationTest.java`

Excerpt from `README.md` (approximate lines 75 and 157):

```markdown
| 2 | **Lock-free reads via `ConcurrentHashMap`** | ... (verified by `StoreTest#incrIsAtomicUnderContention`). |
...
- `StoreTest` — 10 tests (including a 16-thread × 500-ops contention test for `INCR`)
```

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Tests | `mvn test -q` | exit 0 |
| Verify no StoreTest | `find src/test -name 'StoreTest.java'` | no output |

## Steps

### Step 1 — Update engineering decisions table

In `README.md`, change the `ConcurrentHashMap` row to reference the actual test that covers INCR contention:

- Replace `StoreTest#incrIsAtomicUnderContention` with `ServerIntegrationTest#concurrentIncrConverges`.

### Step 2 — Update test inventory section

Replace the `StoreTest` bullet with accurate inventory:

```markdown
- `CommandDispatcherTest` — command handler unit tests (SET/GET/INCR/PX expiry, case insensitivity)
- `RespParserTest` — RESP2 array/bulk parsing edge cases
- `RespEncoderTest` — wire encoding correctness
- `ServerIntegrationTest` — TCP integration tests including concurrent INCR convergence
```

Add a note: "Dedicated `Store` unit tests are planned (see `plans/002-add-store-unit-tests.md`)."

### Step 3 — Verify

```bash
grep -n 'StoreTest' README.md   # expect: no matches (or only in the planned-tests note if you kept the plan reference)
mvn test -q                     # exit 0
```

## STOP conditions

- If `StoreTest.java` already exists when you start, stop and report — Plan 002 may have landed; reconcile README to reference the real class instead.

## Out of scope

- Creating `StoreTest.java` (Plan 002)
- Changing source code under `src/main/`

## Done criteria

- [ ] README contains no false claims about `StoreTest` existing today
- [ ] INCR contention reference points to `ServerIntegrationTest#concurrentIncrConverges`
- [ ] `mvn test -q` passes
