package com.praxstack.redis;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * RESP2 response encoder.
 *
 * <p>Pure function, no state. Every encoder method returns raw bytes ready to
 * be written to the socket.
 */
public final class RespEncoder {

    private static final byte[] CRLF = new byte[]{'\r', '\n'};
    private static final byte[] NULL_BULK = "$-1\r\n".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] OK = "+OK\r\n".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] EMPTY_ARRAY = "*0\r\n".getBytes(StandardCharsets.US_ASCII);

    private RespEncoder() {}

    public static byte[] ok() {
        return OK;
    }

    public static byte[] nullBulk() {
        return NULL_BULK;
    }

    public static byte[] emptyArray() {
        return EMPTY_ARRAY;
    }

    /**
     * RESP array of already-encoded elements (each element includes its own CRLF framing).
     */
    public static byte[] array(List<byte[]> elements) {
        if (elements == null || elements.isEmpty()) {
            return EMPTY_ARRAY;
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            bos.write(("*" + elements.size() + "\r\n").getBytes(StandardCharsets.US_ASCII));
            for (byte[] el : elements) {
                bos.write(el);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bos.toByteArray();
    }

    /** RESP array of bulk strings; {@code null} entries become null bulk ({@code $-1}). */
    public static byte[] bulkArray(List<String> values) {
        if (values == null || values.isEmpty()) {
            return EMPTY_ARRAY;
        }
        List<byte[]> encoded = new ArrayList<>(values.size());
        for (String v : values) {
            encoded.add(v == null ? NULL_BULK : bulkString(v));
        }
        return array(encoded);
    }

    /** {@code +<msg>\r\n} — simple string. */
    public static byte[] simpleString(String msg) {
        return ("+" + msg + "\r\n").getBytes(StandardCharsets.UTF_8);
    }

    /** {@code -<err>\r\n} — error. */
    public static byte[] error(String msg) {
        return ("-" + msg + "\r\n").getBytes(StandardCharsets.UTF_8);
    }

    /** {@code :<n>\r\n} — integer. */
    public static byte[] integer(long n) {
        return (":" + n + "\r\n").getBytes(StandardCharsets.US_ASCII);
    }

    /** {@code $<len>\r\n<bytes>\r\n} — bulk string. Null-safe. */
    public static byte[] bulkString(String s) {
        if (s == null) return NULL_BULK;
        byte[] data = s.getBytes(StandardCharsets.UTF_8);
        byte[] prefix = ("$" + data.length + "\r\n").getBytes(StandardCharsets.US_ASCII);
        byte[] out = new byte[prefix.length + data.length + CRLF.length];
        System.arraycopy(prefix, 0, out, 0, prefix.length);
        System.arraycopy(data, 0, out, prefix.length, data.length);
        System.arraycopy(CRLF, 0, out, prefix.length + data.length, CRLF.length);
        return out;
    }
}
