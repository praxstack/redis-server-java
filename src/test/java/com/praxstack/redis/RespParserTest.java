package com.praxstack.redis;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RespParserTest {

    private RespParser parser(String input) {
        return new RespParser(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void parsesSimplePingCommand() throws IOException {
        List<String> args = parser("*1\r\n$4\r\nPING\r\n").next();
        assertEquals(List.of("PING"), args);
    }

    @Test
    void parsesEchoWithArgument() throws IOException {
        List<String> args = parser("*2\r\n$4\r\nECHO\r\n$5\r\nhello\r\n").next();
        assertEquals(List.of("ECHO", "hello"), args);
    }

    @Test
    void parsesSetWithPxOption() throws IOException {
        List<String> args = parser("*5\r\n$3\r\nSET\r\n$3\r\nfoo\r\n$3\r\nbar\r\n$2\r\nPX\r\n$3\r\n100\r\n").next();
        assertEquals(List.of("SET", "foo", "bar", "PX", "100"), args);
    }

    @Test
    void returnsNullAtEndOfStream() throws IOException {
        assertNull(parser("").next());
    }

    @Test
    void parsesPipelinedCommandsSequentially() throws IOException {
        RespParser p = parser("*1\r\n$4\r\nPING\r\n*1\r\n$4\r\nPING\r\n");
        assertEquals(List.of("PING"), p.next());
        assertEquals(List.of("PING"), p.next());
        assertNull(p.next());
    }

    @Test
    void rejectsNonArrayInput() {
        assertThrows(IOException.class, () -> parser("+OK\r\n").next());
    }

    @Test
    void parsesUtf8BulkString() throws IOException {
        // "héllo" in UTF-8 is 6 bytes
        String input = "*1\r\n$6\r\nh\u00e9llo\r\n";
        byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
        List<String> args = new RespParser(new ByteArrayInputStream(bytes)).next();
        assertEquals("h\u00e9llo", args.get(0));
    }
}
