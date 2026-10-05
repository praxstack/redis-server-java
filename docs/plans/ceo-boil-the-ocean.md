# CEO boil-the-ocean — redis-server-java

Program plan for turning the RESP2 prototype into a Redis-compatible service people can actually operate. Generated 2026-10-05 during the autonomous overnight run.

**Rule:** items in **Incomplete** stay open on purpose. Do not mark them done until there is code, tests, and a merge to `main`.

## Shipped this run (complete)

| Item | Issue | Evidence |
|------|-------|----------|
| MGET / MSET | #6 | `CommandDispatcher` + TCP integration |
| SET NX/XX any-order EX/PX | #7 | `Store.setConditional` |
| EXPIRE / PEXPIRE / TTL / PTTL / PERSIST | #8 | Store TTL API |
| DECR / INCRBY / DECRBY | #9 | `Store.incrBy` |
| APPEND / STRLEN (UTF-8 bytes) | #10 | dispatcher tests |
| GETDEL | #11 | atomic get+delete |
| FLUSHDB / live DBSIZE | #12 | expired keys excluded |
| TYPE | #13 | `string` / `none` |
| INFO | #14 | server + keyspace bulk |
| QUIT + idle SO_TIMEOUT | #15 | integration EOF |
| KEYS glob | #16 | `*` / `?` over live keys |
| `environment.json` `start` | #17 | terminals can launch |
| prax-mode skill | #18 | `.cursor/skills/prax/prax-mode/` |

## Incomplete — high impact (do not treat as done)

These are the ocean. Each is a future issue-sized slice.

### 1. SCAN cursor iteration (replace KEYS in production)
KEYS is O(N) and blocks a worker. Implement `SCAN cursor [MATCH glob] [COUNT n]` with a stable cursor over `ConcurrentHashMap` iteration. Required before any large-keyspace demo.

### 2. List types (LPUSH / RPUSH / LPOP / RPOP / LLEN / LRANGE)
Strings-only store cannot model queues. Needs a `StoredValue` type tag (or separate maps) and `TYPE` already returns `string`/`none` — extend to `list` without breaking existing tests.

### 3. Hash types (HSET / HGET / HDEL / HGETALL / HEXISTS)
Application caches almost always need hashes. Same type-tag work as lists; do not overload string values with JSON.

### 4. Sorted sets (ZADD / ZRANGE / ZRANK / ZREM)
Leaderboards and delayed queues. Skip until lists/hashes exist; sharing the type enum matters.

### 5. RDB snapshot persistence
Process restart currently wipes the dataset. Snapshot to a file on a timer + `SAVE`/`BGSAVE`. Highest ops-risk item: must not stall the accept loop.

### 6. AOF rewrite
Append-only log for durability finer than RDB. Depends on a command journal in `CommandDispatcher`. Do not fake this with "write the hashmap to disk on every SET".

### 7. AUTH and requirepass
Open TCP on 6379 is fine for a lab, not for a shared Cloud Agent. `AUTH password` plus optional `--requirepass`. All other commands `-NOAUTH` until authenticated (per connection in `ClientHandler`).

### 8. MULTI / EXEC / DISCARD transactions
Queue commands per connection, execute atomically against the store. Interacts badly with thread-per-client if we later add WATCH.

### 9. WATCH / optimistic lock
Needed for correct NX lock recipes beyond SET NX. Depends on #8.

### 10. Pub/Sub (SUBSCRIBE / PUBLISH / PSUBSCRIBE)
Cross-connection fanout. Requires `ClientHandler` to grow a subscription registry and a write path that is not "one command → one response".

### 11. Blocking list pops (BLPOP / BRPOP)
Needs timed waits on list heads without occupying the whole 100-thread pool forever — or a separate waiter structure. Do not implement with `Thread.sleep` on a worker.

### 12. Replication (PSYNC / replicaof)
Single-node only today. Replica offset + backlog is a multi-week design; keep it on this list so we do not pretend INFO `role:master` is real.

### 13. NIO / Netty accept path
Thread-per-connection with 100 workers is the teaching model. A Netty (or Java NIO) event loop is the scale path after we have persistence and AUTH. Rejected earlier as premature — still the right long-term accept loop.

### 14. Metrics beyond INFO stub
`connected_clients` and `total_commands_processed` are currently zeros. Wire atomics from `Server` / `CommandDispatcher` and expose Prometheus text on an optional admin port.

### 15. RESP3 hello and maps
Parser only accepts arrays of bulk strings. `HELLO 3` and map types are required for modern redis-py / lettuce defaults.

### 16. ACL users
Finer than requirepass. After AUTH (#7).

## Execution notes

- One GitHub issue + one `prax/issue-N-slug-<suffix>` branch per slice.
- `CommandDispatcher.java` and `Store.java` are single-owner files; do not parallelize two command PRs against them.
- `mvn -B test` is the merge gate. GitHub App cannot open PRs (403); push the branch and fast-forward `main` only after tests.
