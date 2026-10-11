# redis-server-java setup (Mac, verified 2026-10-04)

Stack: Java 17 source level, Maven, JUnit5/Mockito. No env vars or secrets needed. Verified with OpenJDK 25.0.2 and Maven 3.10.0.

## Build + test (one step)
    mvn -B clean package
Result: BUILD SUCCESS, 45 tests, 0 failures, 0 errors, 0 skipped. Produces target/redis-server-java-1.0.0.jar.
(Only warning: "location of system modules is not set in conjunction with -source 17" because pom uses source/target 17 rather than release.)
Tests only: `mvn -B test`.

## Run
    java -jar target/redis-server-java-1.0.0.jar          # port 6379
    java -jar target/redis-server-java-1.0.0.jar 6390     # custom port
Stop with Ctrl-C / SIGTERM (graceful shutdown hook).

## Smoke (redis-cli is not installed; use nc, or `brew install redis` for redis-cli)
    printf '*1\r\n$4\r\nPING\r\n*3\r\n$3\r\nSET\r\n$1\r\nk\r\n$1\r\nv\r\n*2\r\n$3\r\nGET\r\n$1\r\nk\r\n' | nc -w 2 127.0.0.1 6390
Observed: +PONG, +OK, $1 v; INCR returned :1; ECHO hi OK; unknown command returned -ERR unknown command 'NOPE'; SET EX 1 key read back as $-1 after 1.5s (expiry works).

## Known issues
- README claims 54 tests; actual count is 45 (13+9+6+10+7 @Test methods). Docs drift (P3).
- README states 150k+ ops/sec; not benchmarked here.
- When testing, a macOS firewall prompt may appear.
- CI uses JDK 17 (temurin); local JDK 25 also builds fine.
