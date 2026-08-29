# Plan 003: Add RESP parser bulk-string size limit

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat cfe2170..HEAD -- src/main/java/com/praxstack/redis/RespParser.java`

## Status

- **Priority**: P1
- **Effort**: S
- **Risk**: MED
- **Depends on**: none
- **Category**: security
- **Planned at**: commit `cfe2170`, 2026-08-29

## Why this matters

`RespParser.readBulkString()` parses the length from the wire and allocates `new byte[len]` without bounds (`RespParser.java:64-66`). A malicious client can send `$999999999` and force large allocations or OOM. A configurable max bulk size is standard defensive maintenance for protocol parsers.

## Current state

```java
// RespParser.java ~59-68
private String readBulkString() throws IOException {
    int marker = in.read();
    if (marker != '$') { ... }
    int len = readInteger();
    if (len < 0) return null;
    byte[] buf = readNBytes(len);  // unbounded allocation
    ...
}
```

- `ClientHandler` catches `IOException` and disconnects the client on parse failure.
- Tests: `src/test/java/com/praxstack/redis/RespParserTest.java`

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Parser tests | `mvn test -Dtest=RespParserTest -q` | exit 0 |
| Full suite | `mvn test -q` | exit 0 |

## Steps

### Step 1 — Add constant and validation

In `RespParser.java`:

- Add `private static final int MAX_BULK_STRING_BYTES = 512 * 1024;` (512 KiB — matches conservative Redis client defaults; document in Javadoc).
- After `int len = readInteger();` and null-bulk check, if `len > MAX_BULK_STRING_BYTES`, throw `new IOException("Bulk string length " + len + " exceeds maximum " + MAX_BULK_STRING_BYTES)`.

Optionally expose max via package-private constructor parameter for tests; keep public constructor defaulting to 512 KiB.

### Step 2 — Add tests in `RespParserTest.java`

1. `rejectsOversizedBulkString` — feed a bulk header with len = MAX+1, expect IOException.
2. `acceptsBulkStringAtLimit` — len = MAX (use small MAX via test-only constructor if added, or use 1024 for a dedicated test parser instance).

Follow existing test style: in-memory `ByteArrayInputStream` wrapped as parser input.

### Step 3 — Verify integration still passes

```bash
mvn test -q
```

## STOP conditions

- If a max-size constant already exists, stop and only add missing tests.
- If legitimate clients need larger payloads (documented elsewhere), stop and propose a higher limit with README note.

## Out of scope

- Max array element count limit (separate hardening)
- Changing `CommandDispatcher` behavior

## Done criteria

- [ ] Oversized bulk strings throw `IOException` before allocation
- [ ] New unit tests pass
- [ ] `mvn test -q` passes
