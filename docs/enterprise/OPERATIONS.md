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

`.cursor/environment.json` must include a `start` field. Cloud Agent **terminals are ignored when `start` is absent**. The current `start` is an idempotent marker so the Redis Server terminal (port 6379) actually launches. The Dockerfile (`eclipse-temurin:17-jdk-jammy`) installs Maven and `redis-tools`; agents that boot from the default image without a finished environment build will not have `mvn` until that image is used.

## Configuration knobs (code-level)

| Constant | Location | Default |
|----------|----------|---------|
| Port | `Server.DEFAULT_PORT` | 6379 |
| Worker threads | `Server.DEFAULT_WORKER_THREADS` | 100 |
| Queue multiplier | `Server.DEFAULT_QUEUE_CAPACITY_MULTIPLIER` | 2 |
| Bulk string max | `RespParser.DEFAULT_MAX_BULK_STRING_BYTES` | 512 KiB |
| Client idle timeout | `Server.DEFAULT_SO_TIMEOUT_MS` | 5 minutes |
| Expiry sweep interval | `ExpiryManager.SWEEP_INTERVAL_MS` | 100 ms |

No external config file — change via constructor args or constants for now.

## Graceful shutdown

Send SIGTERM; shutdown hook calls `Server.close()`. In-flight handlers get up to 5 seconds to drain.

## Git branch hygiene

This repository uses **`main`** as the only production branch. There is no `dev` or `deb` branch.

Merged feature branches (e.g. `prax/super-pro-skills-stack-d404`) remain on `origin` until you delete them. If your local checkout is on a stale branch after a PR merge, switch to `main` and clean up:

```bash
# See where you are
git status
git branch -a

# Move to latest main
git fetch origin
git checkout main
git pull origin main

# Delete a merged local branch (example)
git branch -d prax/super-pro-skills-stack-d404

# Prune remote-tracking refs for deleted upstream branches
git fetch origin --prune

# Optional: delete a merged remote branch (after PR merge)
git push origin --delete prax/super-pro-skills-stack-d404
```

**Cloud Agents** always start from `main` at the latest commit. If you see an old branch name in agent context, it is historical — run the commands above locally to align your machine.

## Troubleshooting

| Symptom | Likely cause | Check |
|---------|--------------|-------|
| Connection reset under load | Pool saturated | Server logs `worker pool saturated` |
| Client disconnect on large payload | Bulk > 512 KiB | `RespParser` IOException |
| Stale key count in `DBSIZE` | Expired keys not yet swept | `ExpiryManager` sweep; `Store.purgeExpired()` |
| Agent or IDE on stale branch | Local checkout not updated after PR merge | See **Git branch hygiene** above |
