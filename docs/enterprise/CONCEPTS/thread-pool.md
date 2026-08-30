# Thread Pool & Backpressure

## Model

`Server` uses **one thread per active connection**, capped by a fixed worker pool.

```
acceptor thread ──accept()──► ThreadPoolExecutor (N workers, bounded queue)
                                      │
                                      ▼
                               ClientHandler (per socket)
```

Source: `src/main/java/com/praxstack/redis/Server.java`

## Why not `new Thread()` per client?

Unbounded thread creation exhausts file descriptors and memory under connection floods. A pool gives **predictable upper bounds** on concurrent handlers.

## Configuration

| Parameter | Default | Code |
|-----------|---------|------|
| Worker threads | 100 | `DEFAULT_WORKER_THREADS` |
| Queue capacity | 2 × workers (200) | `workerThreads * DEFAULT_QUEUE_CAPACITY_MULTIPLIER` |

Construction (`Server.java:55-72`):

```java
BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(queueCapacity);
this.workers = new ThreadPoolExecutor(
    workerThreads, workerThreads,
    0L, TimeUnit.MILLISECONDS,
    queue,
    threadFactory,
    new ThreadPoolExecutor.AbortPolicy());
```

## Backpressure policy

When the queue is full and all workers are busy, `submit()` throws `RejectedExecutionException`. The accept loop:

1. Logs a WARNING with queue size
2. Closes the socket immediately
3. Continues accepting

(`Server.java:89-101`)

This prevents the classic `Executors.newFixedThreadPool` footgun: its internal `LinkedBlockingQueue` is **unbounded**, so tasks pile up until OOM.

## Test

`ServerIntegrationTest.rejectsConnectionsWhenPoolSaturated` uses `Server(0, 1, 1)` — one worker, queue of one — to force rejection on the third connection.

## Shutdown

`workers.shutdown()` then `awaitTermination(5, SECONDS)`; `shutdownNow()` on timeout (`Server.java:127-135`).
