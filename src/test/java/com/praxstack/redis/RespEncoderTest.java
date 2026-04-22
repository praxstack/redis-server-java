package com.praxstack.redis;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RespEncoderTest {

    @Test
    void encodesOkSimpleString() {
        assertArrayEquals("+OK\r\n".getBytes(StandardCharsets.UTF_8), RespEncoder.ok());
    }

    @Test
    void encodesBulkString() {
        assertArrayEquals("$3\r\nbar\r\n".getBytes(StandardCharsets.UTF_8),
                RespEncoder.bulkString("bar"));
    }

    @Test
    void encodesNullBulkForNullInput() {
        assertArrayEquals("$-1\r\n".getBytes(StandardCharsets.UTF_8),
                RespEncoder.bulkString(null));
    }

    @Test
    void encodesIntegerPositive() {
        assertArrayEquals(":42\r\n".getBytes(StandardCharsets.UTF_8),
                RespEncoder.integer(42));
    }

    @Test
    void encodesIntegerNegative() {
        assertArrayEquals(":-7\r\n".getBytes(StandardCharsets.UTF_8),
                RespEncoder.integer(-7));
    }

    @Test
    void encodesError() {
        assertArrayEquals("-ERR bad\r\n".getBytes(StandardCharsets.UTF_8),
                RespEncoder.error("ERR bad"));
    }

    @Test
    void encodesEmptyArrayForCommandIntrospection() {
        assertEquals("*0\r\n", new String(RespEncoder.emptyArray(), StandardCharsets.UTF_8));
    }
}
