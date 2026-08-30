# Client Handler (Connection Lifecycle)

Each TCP connection gets one `ClientHandler` runnable on a worker thread.

## Lifecycle

```java
try (socket;
     BufferedInputStream in = ...;
     OutputStream out = ...) {
    RespParser parser = new RespParser(in);
    List<String> args;
    while ((args = parser.next()) != null) {
        byte[] response = dispatcher.dispatch(args);
        out.write(response);
        out.flush();
    }
}
```

Source: `ClientHandler.java:27-45`

## Resource management

- Try-with-resources closes socket and streams on exit
- `IOException` on disconnect logged at FINE (normal client close)
- Unexpected `RuntimeException` logged at WARNING

## Pipelining

The `while` loop processes multiple commands per connection without waiting for client reads between commands — matches `redis-benchmark` behavior.

Parser tests: `RespParserTest.parsesPipelinedCommandsSequentially`.

Integration: clients can send `SET` + `GET` in one write (`ServerIntegrationTest.setAndGetOverSocket`).

## Shared dispatcher

All handlers on a server share one `CommandDispatcher` and therefore one `Store` instance (`Server.java:57-58`). This is correct: the store is concurrent.

## Failure modes

| Event | Behavior |
|-------|----------|
| Malformed RESP | `IOException` from parser → handler exits, socket closed |
| Oversized bulk | `IOException` from size check → disconnect |
| Handler error | `-ERR` response returned, connection stays open |

## Not in scope

- TLS termination
- AUTH
- Connection timeouts (OS default)
- Write buffer coalescing beyond `flush()` per command
