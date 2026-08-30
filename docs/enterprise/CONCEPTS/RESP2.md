# RESP2 Protocol

RESP (REdis Serialization Protocol) version 2 is the wire format this server speaks.

## Grammar (client → server)

Commands arrive as **arrays of bulk strings**:

```
*<argc>\r\n
$<len>\r\n<bytes>\r\n   (repeated argc times)
```

Example — `SET foo bar`:

```
*3\r\n$3\r\nSET\r\n$3\r\nfoo\r\n$3\r\nbar\r\n
```

## Implementation

**Parser** — `src/main/java/com/praxstack/redis/RespParser.java`

- `next()` reads one command per call; returns `null` at EOF (`RespParser.java:42-57`)
- Pipelining: caller loops `next()` until `null` (`ClientHandler.java:35-38`)
- Bulk strings: `$<len>\r\n` then `len` bytes then `\r\n` (`RespParser.java:69-81`)
- Null bulk: `$-1\r\n` → Java `null` argument (`RespParser.java:75`)

**Encoder** — `src/main/java/com/praxstack/redis/RespEncoder.java`

| Type | Prefix | Example |
|------|--------|---------|
| Simple string | `+` | `+OK\r\n` |
| Error | `-` | `-ERR ...\r\n` |
| Integer | `:` | `:42\r\n` |
| Bulk string | `$` | `$3\r\nbar\r\n` |
| Null bulk | `$-1` | `$-1\r\n` |
| Array | `*` | `*0\r\n` |

## Security: bulk size limit

Malicious clients can advertise huge bulk lengths. Before allocating `byte[len]`, the parser checks:

```java
if (len > maxBulkStringBytes) throw new IOException(...);
```

Default max: **512 KiB** (`RespParser.DEFAULT_MAX_BULK_STRING_BYTES`).

Tests: `RespParserTest.rejectsOversizedBulkString`, `RespParserTest.acceptsBulkStringAtLimit`.

## Further reading

- [Redis protocol spec](https://redis.io/docs/reference/protocol-spec/)
- Integration tests: `ServerIntegrationTest.java` (real TCP bytes)
