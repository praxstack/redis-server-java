# redis-server-java: Understanding (audit 2026-10-04, HEAD e73b5cb)

## Purpose and maturity
A teaching/portfolio RESP2 Redis clone in Java 17 (Maven, zero runtime deps). About 540 LOC main, 550 LOC tests, 26 commits (2026-08-2x to 08-30), single author. Prototype quality: core works, protocol edge cases do not.

## Authoritative vs historical docs
- Authoritative: src/, pom.xml, .github/workflows/ci.yml.
- README.md: marketing plus design rationale; contains stale/unverifiable numbers (see drift).
- docs/enterprise/{ARCHITECTURE,OPERATIONS}.md and CONCEPTS/*: derived docs with hard-coded line numbers (stale) and a test count (stale).
- plans/001-005 + plans/README.md: historical /improve output (commit cfe2170); all marked DONE in the index, but each plan file still has unchecked "Done criteria" boxes (16 `[ ]`).
- dev-skills-setup/*, .cursor/*, openspec/config.yaml, scripts/cloud-agent-install.sh: agent-tooling scaffolding unrelated to the server. openspec has only config.yaml, no specs or changes. No SPEC/PROGRESS/HANDOFF/CHANGELOG files exist.

## Module map (src/main/java/com/praxstack/redis)
- Server (162 lines): ServerSocket on the given port, daemon acceptor thread, ThreadPoolExecutor(100 core=max, ArrayBlockingQueue 200, AbortPolicy); excess connections closed. main(args[0]=port), shutdown hook calls close().
- ClientHandler: per-connection loop `parser.next()` -> `dispatcher.dispatch()` -> write/flush.
- RespParser: streaming; accepts only `*N` arrays of `$` bulk strings; 512 KiB bulk cap.
- RespEncoder: stateless static encoders (simple string, error, int, bulk, null bulk, empty array).
- CommandDispatcher: ConcurrentHashMap name -> handler. PING, ECHO, SET (PX/EX only), GET, DEL, INCR, EXISTS, DBSIZE; COMMAND and CONFIG are stubs returning `*0`.
- Store: ConcurrentHashMap<String, StoredValue>; lazy expiry on get; INCR via compute() preserving TTL; purgeExpired.
- StoredValue: record(value, expiresAt epoch ms, -1 = none).
- ExpiryManager: single daemon scheduled thread, 100 ms sweep of the entire map (O(n) per sweep, not sampled).

## Flow
accept -> submit ClientHandler -> parse RESP array -> uppercase command -> handler -> byte[] -> socket. Errors: NumberFormatException -> "ERR value is not an integer"; other RuntimeException -> "ERR <message>".

## Data / state
In-memory only. No persistence (no RDB/AOF), no config file. Only config: port argument; thread count and queue fixed at 100/200.

## Build / run / test
- `mvn clean package` -> target/redis-server-java-1.0.0.jar; `java -jar ... [port]`.
- `mvn -B test`: I ran it, exit 0, 45 tests (13+7+9+6+10), 0 failures (log: mvn-test.log).
- CI: one workflow (ubuntu, temurin 17, `mvn -B test`) on push/PR to main. Last 3 runs all success. No lint, coverage, package, or dependency-update job.

## Git / GitHub
26 commits, one author, 2026-08-2x to 2026-08-30; nothing since. 5 PRs, all merged. 0 open issues. 5 remote branches (prax/*-d404), none with commits ahead of origin/main (stale, safe to delete; the docs even describe branch hygiene).

## Roadmap vs reality (checks done)
1. README "54 tests across 5 suites": FALSE, actual 45 (per-suite counts match: 9/7/10/13/6). OPERATIONS.md says 45 (correct) -> docs contradict each other.
2. "Drop-in compatible with redis-cli": mostly true for array commands; inline commands (telnet/nc, `PING\r\n`) close the connection (probed). redis-cli sends COMMAND DOCS; stub `*0` works.
3. "150k+ ops/sec" benchmark: unverifiable, no script; redis-benchmark not run.
4. Plan 001 (README StoreTest drift): README now claims StoreTest exists (true) but the test count is wrong.
5. Plan 002 StoreTest incl. 16x500 INCR contention: present (StoreTest 10 tests). Confirmed.
6. Plan 003 parser size limit: present (RespParser:44,72) for bulk length.
7. Plan 004 CI: present, passing.
8. Plan 005 backpressure: implemented (Server:66-79, saturation test in ServerIntegrationTest). Confirmed, probed in test log "worker pool saturated".
9. Graceful shutdown "drain in-flight": close() does shutdown+awaitTermination 5s, but idle client sockets are never closed, so it always waits the full 5 s while any client is connected, then shutdownNow (interrupt cannot unblock socket read). Partially true.
10. "Lock-free reads / atomic INCR": true (CHM.get / compute).
11. ARCHITECTURE/OPERATIONS line refs: stale, e.g. Server.main is at Server.java:152-162, docs say 139-150.
12. README table "SET key value [PX | EX]": true, but other options are silently ignored (see findings). "CONFIG GET" stub is actually any CONFIG.
13. plans/README says "4 test classes, no StoreTest" baseline, historical, fine.

## Risk areas (probed on a local 127.0.0.1 server, stopped)
- Parser and protocol edge cases (null bulk elements, malformed headers, inline commands).
- SET: `NX/XX/GET/KEEPTTL` etc. return +OK and are ignored; `PX -5` stores an already-expired key and returns OK (real Redis returns an error); very large `EX` values overflow the TTL arithmetic.
- INCR overflow returns `-ERR long overflow` (Redis: "ERR increment or decrement would overflow"). DBSIZE counts expired-not-yet-swept keys.
- Binary safety: values are decoded as UTF-8 String; non-UTF-8 payloads are corrupted; bulk length counts bytes and re-encode may differ.
- Sweep is O(n) full scan every 100 ms: latency/CPU at large key counts.

_Security findings from this audit are kept privately by the owner._
