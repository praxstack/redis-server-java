# Architecture — redis-server-java

Enterprise-oriented overview of the RESP2-compatible Redis server implemented in Java 17.

## System context

```
Client (redis-cli, benchmark) ──TCP/RESP2──► Server ──► Store (ConcurrentHashMap)
                                                │
                                                └──► ExpiryManager (scheduled sweep)
```

The server is a **blocking I/O, thread-per-connection** design. Each accepted socket is handed to a worker from a bounded `ThreadPoolExecutor`. Workers run `ClientHandler`, which parses RESP2 commands, dispatches them, and writes encoded responses.

## Subsystems

| Subsystem | Source | Responsibility |
|-----------|--------|----------------|
| Accept loop | `src/main/java/com/praxstack/redis/Server.java` | `ServerSocket.accept()`, backpressure via bounded queue + `AbortPolicy` |
| Connection handler | `src/main/java/com/praxstack/redis/ClientHandler.java` | Per-connection read/dispatch/write loop |
| Protocol parser | `src/main/java/com/praxstack/redis/RespParser.java` | Streaming RESP2 array-of-bulk-strings; 512 KiB bulk cap |
| Protocol encoder | `src/main/java/com/praxstack/redis/RespEncoder.java` | Stateless RESP2 response bytes |
| Command routing | `src/main/java/com/praxstack/redis/CommandDispatcher.java` | Case-insensitive handler map |
| Key-value store | `src/main/java/com/praxstack/redis/Store.java` | `ConcurrentHashMap`, lazy expiry, atomic `INCR` |
| Value record | `src/main/java/com/praxstack/redis/StoredValue.java` | Immutable `(value, expiresAt)` |
| TTL sweeper | `src/main/java/com/praxstack/redis/ExpiryManager.java` | 100 ms periodic `purgeExpired()` |

## Request path

1. **Accept** — `Server.acceptLoop()` blocks on `ServerSocket.accept()` (`Server.java:84-110`).
2. **Submit** — `ClientHandler` runnable submitted to `ThreadPoolExecutor`; on saturation, socket closed (`Server.java:89-101`).
3. **Parse** — `RespParser.next()` reads one command array per call; supports pipelining (`ClientHandler.java:33-38`, `RespParser.java:42-57`).
4. **Dispatch** — `CommandDispatcher.dispatch()` uppercases command name, invokes handler (`CommandDispatcher.java:43-58`).
5. **Store** — Handlers call `Store` methods; `GET` performs lazy expiry (`Store.java:27-36`).
6. **Encode** — Handler returns `byte[]` from `RespEncoder`; flushed to socket (`ClientHandler.java:36-38`).

## Concurrency model

- **Reads** — `ConcurrentHashMap.get()` without global locks (`Store.java:28`).
- **Writes** — `put()` for `SET`; `compute()` for atomic `INCR` (`Store.java:52-64`).
- **Expiry** — Hybrid: lazy on `GET`, active sweep every 100 ms (`ExpiryManager.java:35-36`, `Store.java:80-89`).
- **Workers** — Default 100 threads, queue capacity `2 × workerThreads` (`Server.java:36-38`, `Server.java:55-72`).

## Security hardening

- **Bulk string cap** — `RespParser` rejects lengths above 512 KiB before allocation (`RespParser.java:28-29`, `RespParser.java:72-75`).
- **Backpressure** — Saturated pool closes new connections instead of unbounded queue growth (`Server.java:89-101`).

## Shutdown

`Server.close()` sets `running` false, closes `ServerSocket`, stops `ExpiryManager`, `shutdown()` + `awaitTermination(5s)` on workers (`Server.java:118-137`). JVM shutdown hook registered in `main()`.

## Test coverage map

| Area | Primary tests |
|------|----------------|
| Store semantics | `StoreTest.java` |
| Command handlers | `CommandDispatcherTest.java` |
| Parser/encoder | `RespParserTest.java`, `RespEncoderTest.java` |
| End-to-end TCP | `ServerIntegrationTest.java` |

See [CONCEPTS/](CONCEPTS/) for teach-style deep dives on each primitive.
