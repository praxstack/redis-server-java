# Plan 005: Bound accept queue / backpressure policy

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat cfe2170..HEAD -- src/main/java/com/praxstack/redis/Server.java`

## Status

- **Priority**: P2
- **Effort**: M
- **Risk**: MED
- **Depends on**: plans/002-add-store-unit-tests.md (recommended)
- **Category**: perf
- **Planned at**: commit `cfe2170`, 2026-08-29

## Why this matters

`Server` uses `Executors.newFixedThreadPool(workerThreads)` (`Server.java:50-54`). The default `FixedThreadPool` uses an unbounded `LinkedBlockingQueue` for pending tasks. Under connection floods, `accept()` keeps enqueueing `ClientHandler` runnables even when all 100 workers are busy, which can grow the queue without bound and OOM the JVM. A bounded queue with a defined rejection policy (drop, block accept, or close socket) gives predictable resource use.

## Current state

```java
// Server.java acceptLoop
while (running.get()) {
    Socket client = serverSocket.accept();
    client.setTcpNoDelay(true);
    workers.submit(new ClientHandler(client, dispatcher));  // unbounded queue
}
```

- Default pool size: 100 (`DEFAULT_WORKER_THREADS`)
- Integration test `concurrentClientsAreHandledIndependently` uses 20 clients — well under limit

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Full tests | `mvn test -q` | exit 0 |
| Manual load (optional) | start server, open many connections | observe bounded memory |

## Steps

### Step 1 — Replace default executor with bounded queue

In `Server.java`, replace `Executors.newFixedThreadPool` with explicit construction:

```java
int queueCapacity = 200; // document: 2× worker threads
BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(queueCapacity);
this.workers = new ThreadPoolExecutor(
    workerThreads, workerThreads,
    0L, TimeUnit.MILLISECONDS,
    queue,
    r -> { Thread t = new Thread(r, "redis-worker"); t.setDaemon(true); return t; },
    new ThreadPoolExecutor.AbortPolicy()  // or CallerRunsPolicy — document choice
);
```

### Step 2 — Handle rejection in acceptLoop

Wrap `workers.submit(...)` in try/catch for `RejectedExecutionException`:

- Log at WARNING
- Close the rejected `Socket` immediately
- Continue accept loop

Do not block `accept()` indefinitely.

### Step 3 — Add unit or integration test

Add `ServerIntegrationTest.rejectsConnectionsWhenPoolSaturated` OR a package-visible test hook:

- Start `Server(0, 2)` with small pool
- Open 2 connections that block (e.g., send partial command, don't read)
- Attempt 3rd connection — expect quick close or connection reset
- Assert server still accepts after blocked clients disconnect

If flaky, use a `CountDownLatch` inside a test-only `ClientHandler` factory — only if necessary; prefer black-box socket test first.

### Step 4 — Document in README

Add row to Key Engineering Decisions table describing bounded queue + reject policy.

### Step 5 — Verify

```bash
mvn test -q
```

## STOP conditions

- If `ThreadPoolExecutor` with bounded queue already exists, stop and only add tests/docs.
- If rejection test is too flaky on CI, stop and report — propose `@Tag("slow")` or load test in separate job.

## Out of scope

- Migrating to NIO / epoll
- Changing worker thread default from 100

## Done criteria

- [ ] Worker queue has documented finite capacity
- [ ] Rejected connections are closed, not queued unboundedly
- [ ] At least one automated test covers saturation behavior
- [ ] `mvn test -q` passes
