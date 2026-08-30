package com.praxstack.redis;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Streaming RESP2 parser.
 *
 * <p>Parses a stream of RESP arrays of bulk strings (the form used for
 * client-to-server commands). Handles pipelined commands — callers invoke
 * {@link #next()} repeatedly until it returns {@code null}.
 *
 * <p>The grammar accepted:
 * <pre>
 *   array       ::= "*" LEN CRLF element*
 *   element     ::= bulkString
 *   bulkString  ::= "$" LEN CRLF &lt;bytes&gt; CRLF
 *                 | "$-1" CRLF   (null bulk)
 *   LEN         ::= positive or negative integer, ASCII digits
 * </pre>
 */
public final class RespParser {

    /** Maximum bulk-string payload size (512 KiB). Conservative default for DoS resistance. */
    static final int DEFAULT_MAX_BULK_STRING_BYTES = 512 * 1024;

    private static final byte CR = '\r';
    private static final byte LF = '\n';

    private final InputStream in;
    private final int maxBulkStringBytes;

    public RespParser(InputStream in) {
        this(in, DEFAULT_MAX_BULK_STRING_BYTES);
    }

    /** Package-private for tests that need a smaller limit without allocating large buffers. */
    RespParser(InputStream in, int maxBulkStringBytes) {
        this.in = in;
        this.maxBulkStringBytes = maxBulkStringBytes;
    }

    /**
     * Parse the next command (array of bulk strings) from the stream.
     *
     * @return list of arguments, or {@code null} at end-of-stream
     */
    public List<String> next() throws IOException {
        int first = in.read();
        if (first == -1) return null;
        if (first != '*') {
            throw new IOException("Expected '*' to start RESP array, got: " + (char) first);
        }
        int count = readInteger();
        if (count < 0) {
            return List.of();
        }
        List<String> args = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            args.add(readBulkString());
        }
        return args;
    }

    private String readBulkString() throws IOException {
        int marker = in.read();
        if (marker != '$') {
            throw new IOException("Expected '$' for bulk string, got: " + (char) marker);
        }
        int len = readInteger();
        if (len < 0) return null;
        if (len > maxBulkStringBytes) {
            throw new IOException("Bulk string length " + len + " exceeds maximum " + maxBulkStringBytes);
        }
        byte[] buf = readNBytes(len);
        expectCrlf();
        return new String(buf, StandardCharsets.UTF_8);
    }

    private int readInteger() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(8);
        int b;
        while ((b = in.read()) != -1 && b != CR) {
            bos.write(b);
        }
        if (b != CR) throw new IOException("Unexpected EOF reading integer");
        int lf = in.read();
        if (lf != LF) throw new IOException("Expected LF after CR");
        return Integer.parseInt(bos.toString(StandardCharsets.US_ASCII));
    }

    private byte[] readNBytes(int n) throws IOException {
        byte[] out = new byte[n];
        int read = 0;
        while (read < n) {
            int r = in.read(out, read, n - read);
            if (r == -1) throw new IOException("Unexpected EOF reading " + n + " bytes");
            read += r;
        }
        return out;
    }

    private void expectCrlf() throws IOException {
        int cr = in.read();
        int lf = in.read();
        if (cr != CR || lf != LF) {
            throw new IOException("Expected CRLF");
        }
    }
}
