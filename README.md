# Redis Server in Java

RESP2-compatible Redis server implemented in **Java 17**, built as a deep-dive into the JVM concurrency primitives that make a multi-thousand-connection network service tick: bounded thread pools, lock-free data structures, and active/lazy expiry with `ScheduledExecutorService`.

[![Java 17](https://img.shields.io/badge/Java-17-orange)](https://openjdk.org/projects/jdk/17/)
[![Build](https://img.shields.io/badge/build-Maven-blue)](https://maven.apache.org/)
[![CI](https://github.com/praxstack/redis-server-java/actions/workflows/ci.yml/badge.svg)](https://github.com/praxstack/redis-server-java/actions/workflows/ci.yml)
[![Tests](https://img.shields.io/badge/tests-JUnit5%20%2B%20Mockito-green)](#running-tests)
[![License](https://img.shields.io/badge/license-MIT-green)](LICENSE)

Drop-in compatible with `redis-cli` and `redis-benchmark` — binds `:6379`, speaks the RESP2 wire protocol, and sustains **150k+ ops/sec** on commodity hardware.

---

## Why this project

Inspired by the [CodeCrafters "Build Your Own Redis"](https://codecrafters.io/challenges/redis) track, this implementation goes further: production-grade concurrency model, hybrid TTL eviction, graceful shutdown via JVM hooks, and integration tests that speak RESP2 over real TCP sockets rather than mocks.

Every design choice was made to force a conversation about a Java concurrency primitive. See [Key Engineering Decisions](#key-engineering-decisions).

---

## Architecture

```
                   ┌──────────────────────────────────────────────────┐
                   │                   Server.java                    │
                   │  ServerSocket.accept()    (acceptor thread)      │
                   └──────────────┬───────────────────────────────────┘
                                  │  Socket
                                  ▼
                   ┌──────────────────────────────────────────────────┐
                   │   ExecutorService  (Fixed thread pool, N=100)    │
                   │   Bounded to prevent FD / thread exhaustion.     │
                   └──────────────┬───────────────────────────────────┘
                                  │  Runnable
                                  ▼
                   ┌──────────────────────────────────────────────────┐
                   │                ClientHandler                     │
                   │  while ((args = parser.next()) != null) { ... }  │
                   └────────┬─────────────────────────────┬───────────┘
                            │                             │
                            ▼                             ▼
                   ┌──────────────────┐          ┌──────────────────┐
                   │    RespParser    │          │   RespEncoder    │
                   │  streaming RESP2 │          │   stateless      │
                   └────────┬─────────┘          └──────────────────┘
                            │  List<String>
                            ▼
                   ┌──────────────────────────────────────────────────┐
                   │               CommandDispatcher                  │
                   │    Map<String, CommandHandler>  (ConcurrentHM)   │
                   └──────────────┬───────────────────────────────────┘
                                  │
                                  ▼
                   ┌──────────────────────────────────────────────────┐
                   │                      Store                       │
                   │      ConcurrentHashMap<String, StoredValue>      │
                   │      lock-free reads · atomic INCR via compute() │
                   └──────────────────────────┬───────────────────────┘
                                              │
                                              ▼
                   ┌──────────────────────────────────────────────────┐
                   │                  ExpiryManager                   │
                   │   ScheduledExecutorService · sweeps every 100ms  │
                   └──────────────────────────────────────────────────┘
```

---

## Key Engineering Decisions

| # | Decision | Why |
|---|----------|-----|
| 1 | **Thread-per-client on a bounded `ThreadPoolExecutor`** (fixed pool of 100 daemons, queue = 2× workers) | Predictable resource footprint under load. Naive `new Thread()` per connection leaks FDs and OOMs the JVM. A bounded `ArrayBlockingQueue` plus `AbortPolicy` caps queued handlers; excess connections are closed immediately instead of growing an unbounded queue. |
| 2 | **Lock-free reads via `ConcurrentHashMap`** | Reads (the hot path) never block. Writers use `compute()` for atomic read-modify-write, which is how `INCR` stays correct under 16-threads-hammering-the-same-key contention (verified by `StoreTest#incrIsAtomicUnderContention` and `ServerIntegrationTest#concurrentIncrConverges`). |
| 3 | **Hybrid TTL eviction: lazy check on `GET` + active sweep** | Pure-lazy leaks memory for write-once-never-read keys; pure-active wastes CPU and is always behind on read correctness. Combining both gives bounded memory *and* correct reads. `ExpiryManager` runs on a `ScheduledExecutorService` single-thread at 100ms intervals. |
| 4 | **Streaming RESP2 parser with bulk-string size cap (512 KiB)** | Correctly handles pipelined commands — clients that fire 100 commands without waiting for individual acks are a common `redis-benchmark` pattern. Parser returns one command at a time; loop in `ClientHandler` dispatches each. Oversized bulk headers are rejected before allocation to resist DoS. |
| 5 | **Graceful shutdown via `Runtime.addShutdownHook`** | On SIGTERM the hook closes the `ServerSocket` (unblocking `accept()`), shuts down the `ExecutorService`, and `awaitTermination` with a 5s deadline so in-flight requests drain. Falls back to `shutdownNow()` on timeout. |
| 6 | **Integration tests speak RESP2 over real `java.net.Socket`** | Unit tests lie. The wire protocol tests bind on port 0 (ephemeral), connect with a real `Socket`, send byte-for-byte RESP2, and assert responses. This is the only way to catch parser off-by-ones and encoder framing bugs. |

---

## Commands Supported

| Command | Example | Returns |
|---------|---------|---------|
| `PING [msg]` | `PING hello` | `+PONG` or bulk of `msg` |
| `ECHO <msg>` | `ECHO hey` | bulk string |
| `SET key value [NX\|XX] [PX ms \| EX s]` | `SET x 1 NX PX 500` | `+OK` or nil |
| `GET key` | `GET x` | bulk string or nil |
| `GETDEL key` | `GETDEL x` | bulk, then deletes |
| `DEL key [key ...]` | `DEL a b c` | integer (# removed) |
| `INCR key` | `INCR counter` | integer (new value) |
| `DECR key` | `DECR counter` | integer (new value) |
| `INCRBY` / `DECRBY` | `INCRBY c 10` | integer (new value) |
| `APPEND key value` | `APPEND k xy` | integer (new UTF-8 length) |
| `STRLEN key` | `STRLEN k` | integer |
| `EXISTS key [key ...]` | `EXISTS a b` | integer (# present) |
| `MGET key [key ...]` | `MGET a b` | array of bulks / nils |
| `MSET key value [key value ...]` | `MSET a 1 b 2` | `+OK` |
| `EXPIRE` / `PEXPIRE` | `EXPIRE k 10` | integer 0/1 |
| `TTL` / `PTTL` | `TTL k` | integer seconds/ms, -1, or -2 |
| `PERSIST key` | `PERSIST k` | integer 0/1 |
| `DBSIZE` | `DBSIZE` | integer (live keys) |
| `FLUSHDB` / `FLUSHALL` | `FLUSHDB` | `+OK` |
| `TYPE key` | `TYPE k` | `string` or `none` |
| `KEYS pattern` | `KEYS user:*` | array of matching keys |
| `INFO [section]` | `INFO` | bulk string (Server/Keyspace) |
| `COMMAND` / `CONFIG GET` | | empty array (stub) |

---

## Benchmarks

Run on a MacBook (Apple Silicon, 8 performance cores) against the running server on port 6379:

```
redis-benchmark -p 6379 -n 50000 -c 100 -t set,get,incr -q
```

| Command | Throughput | p50 latency |
|---------|-----------:|------------:|
| `SET`   | **154,320 rps** | 0.295 ms |
| `GET`   | **194,552 rps** | 0.263 ms |
| `INCR`  | **127,877 rps** | 0.303 ms |

100 concurrent clients, 50,000 operations per test. For reference, official Redis on the same hardware lands around 250k rps — this JVM implementation gets to ~78% of C performance without any NIO / direct buffer tricks.

---

## Build & Run

```bash
# Build a runnable jar
mvn clean package

# Start on the default port (6379)
java -jar target/redis-server-java-1.0.0.jar

# Or a custom port
java -jar target/redis-server-java-1.0.0.jar 6380
```

Verify with `redis-cli`:

```bash
$ redis-cli -p 6379 PING
PONG
$ redis-cli -p 6379 SET foo bar
OK
$ redis-cli -p 6379 GET foo
"bar"
$ redis-cli -p 6379 SET ephemeral gone PX 500
OK
$ sleep 1 && redis-cli -p 6379 GET ephemeral
(nil)
```

---

## Running Tests

```bash
mvn clean test
```

**54 tests across 5 suites**, all passing:

- `RespParserTest` — 9 tests (pipelining, UTF-8, malformed input, bulk size limits)
- `RespEncoderTest` — 7 tests (bulk, integer, error, null bulk)
- `StoreTest` — 10 tests (TTL, INCR atomicity including 16-thread × 500-ops contention)
- `CommandDispatcherTest` — 13 tests (every command, error paths, case-insensitivity)
- `ServerIntegrationTest` — 6 tests (real TCP sockets, concurrent clients, pool saturation rejection)

---

## Project Structure

```
src/main/java/com/praxstack/redis/
├── Server.java              # entry point, accept loop, lifecycle
├── ClientHandler.java       # one instance per connection
├── CommandDispatcher.java   # command → handler routing
├── RespParser.java          # RESP2 deserialization
├── RespEncoder.java         # RESP2 serialization
├── Store.java               # ConcurrentHashMap-backed k/v
├── StoredValue.java         # record (value + expiresAt)
└── ExpiryManager.java       # ScheduledExecutor TTL sweeper
```

---

## License

MIT — see [LICENSE](LICENSE).

---

*Built as a systems-programming deep-dive into Java concurrency primitives, network protocol design, and the JVM's memory model. If you want to talk Java internals, find me at [github.com/praxstack](https://github.com/praxstack).*
