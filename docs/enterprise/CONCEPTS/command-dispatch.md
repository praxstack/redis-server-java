# Command Dispatch

`CommandDispatcher` routes parsed RESP arguments to handler functions.

## Registration

Handlers live in `ConcurrentHashMap<String, CommandHandler>` keyed by **uppercase** command name (`CommandDispatcher.java:23`, `CommandDispatcher.java:30-41`).

Built-in commands: `PING`, `ECHO`, `SET`, `GET`, `DEL`, `INCR`, `EXISTS`, `COMMAND`, `CONFIG`, `DBSIZE`.

## Dispatch flow

```java
String cmd = args.get(0).toUpperCase(Locale.ROOT);
CommandHandler h = handlers.get(cmd);
if (h == null) return RespEncoder.error("ERR unknown command ...");
return h.handle(args);
```

(`CommandDispatcher.java:43-58`)

## Error handling

- Empty args → `ERR empty command`
- Unknown command → `ERR unknown command`
- `NumberFormatException` → `ERR value is not an integer or out of range` (e.g. `INCR` on non-integer)
- Other runtime exceptions → `ERR <message>`

## Case insensitivity

`ping`, `Ping`, `PING` all work. Test: `CommandDispatcherTest.commandNamesAreCaseInsensitive`.

## Handler examples

| Command | Handler | Store interaction |
|---------|---------|-------------------|
| SET | `handleSet` | `set` or `setWithTtlMillis` |
| GET | `handleGet` | `get` → bulk or null bulk |
| INCR | `handleIncr` | `incr` → integer response |
| DEL | `handleDel` | `delete` per key, sum count |
| DBSIZE | lambda | `store.size()` |

## Extension point

Add a new command:

1. Implement `CommandHandler` method
2. `handlers.put("MYCMD", this::handleMyCmd)` in `registerBuiltins`
3. Add tests in `CommandDispatcherTest`

No plugin system — explicit registration keeps the prototype simple.
