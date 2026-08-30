# Operations — redis-server-java

## Prerequisites

- Java 17 (Temurin recommended)
- Maven 3.8+

## Build

```bash
mvn clean package
```

Produces `target/redis-server-java-1.0.0.jar` (see `pom.xml`).

## Run

```bash
# Default port 6379
java -jar target/redis-server-java-1.0.0.jar

# Custom port
java -jar target/redis-server-java-1.0.0.jar 6380
```

Entry point: `Server.main()` (`src/main/java/com/praxstack/redis/Server.java:139-150`).

## Test

```bash
mvn test          # full suite (45 tests)
mvn test -q       # quiet
mvn -B test       # batch mode (CI)
mvn test -Dtest=StoreTest   # single class
```

## CI

GitHub Actions workflow: `.github/workflows/ci.yml`

- Triggers: push to `main`, pull requests to `main`
- Runner: `ubuntu-latest`, Java 17 Temurin, Maven cache
- Command: `mvn -B test`

## Health check

```bash
redis-cli -p 6379 PING
# PONG
```

## Benchmark (optional)

```bash
redis-benchmark -p 6379 -n 50000 -c 100 -t set,get,incr -q
```

Documented results in root `README.md`.

## Cloud Agent install

`scripts/cloud-agent-install.sh` runs `mvn clean package -DskipTests` for environment bootstrap. CI is the authoritative test gate.

## Configuration knobs (code-level)

| Constant | Location | Default |
|----------|----------|---------|
| Port | `Server.DEFAULT_PORT` | 6379 |
| Worker threads | `Server.DEFAULT_WORKER_THREADS` | 100 |
| Queue multiplier | `Server.DEFAULT_QUEUE_CAPACITY_MULTIPLIER` | 2 |
| Bulk string max | `RespParser.DEFAULT_MAX_BULK_STRING_BYTES` | 512 KiB |
| Expiry sweep interval | `ExpiryManager.SWEEP_INTERVAL_MS` | 100 ms |

No external config file — change via constructor args or constants for now.

## Graceful shutdown

Send SIGTERM; shutdown hook calls `Server.close()`. In-flight handlers get up to 5 seconds to drain.

## Troubleshooting

| Symptom | Likely cause | Check |
|---------|--------------|-------|
| Connection reset under load | Pool saturated | Server logs `worker pool saturated` |
| Client disconnect on large payload | Bulk > 512 KiB | `RespParser` IOException |
| Stale key count in `DBSIZE` | Expired keys not yet swept | `ExpiryManager` sweep; `Store.purgeExpired()` |
