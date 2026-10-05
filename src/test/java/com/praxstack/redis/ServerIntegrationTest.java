package com.praxstack.redis;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end integration tests that speak RESP2 over real TCP sockets.
 * Spins up {@link Server} on an ephemeral port (port 0), connects with a
 * real {@link Socket}, and validates the wire protocol byte-for-byte.
 */
class ServerIntegrationTest {

    private Server server;

    @BeforeEach
    void startServer() throws IOException {
        server = new Server(0, 10);
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.close();
    }

    private String sendAndRead(String command) throws IOException {
        try (Socket s = new Socket("127.0.0.1", server.boundPort())) {
            OutputStream out = s.getOutputStream();
            out.write(command.getBytes(StandardCharsets.UTF_8));
            out.flush();
            s.shutdownOutput();
            return readAll(s.getInputStream());
        }
    }

    private String readAll(InputStream in) throws IOException {
        BufferedInputStream bis = new BufferedInputStream(in);
        byte[] buf = new byte[4096];
        int read;
        StringBuilder sb = new StringBuilder();
        while ((read = bis.read(buf)) != -1) {
            sb.append(new String(buf, 0, read, StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    @Test
    void pingReturnsPongOverSocket() throws IOException {
        String resp = sendAndRead("*1\r\n$4\r\nPING\r\n");
        assertEquals("+PONG\r\n", resp);
    }

    @Test
    void setAndGetOverSocket() throws IOException {
        String resp = sendAndRead(
                "*3\r\n$3\r\nSET\r\n$3\r\nfoo\r\n$3\r\nbar\r\n" +
                "*2\r\n$3\r\nGET\r\n$3\r\nfoo\r\n");
        assertEquals("+OK\r\n$3\r\nbar\r\n", resp);
    }

    @Test
    void msetAndMgetOverSocket() throws IOException {
        String resp = sendAndRead(
                "*5\r\n$4\r\nMSET\r\n$1\r\na\r\n$1\r\n1\r\n$1\r\nb\r\n$1\r\n2\r\n" +
                "*3\r\n$4\r\nMGET\r\n$1\r\na\r\n$1\r\nb\r\n");
        assertEquals("+OK\r\n*2\r\n$1\r\n1\r\n$1\r\n2\r\n", resp);
    }

    @Test
    void infoOverSocketContainsVersion() throws IOException {
        String resp = sendAndRead("*1\r\n$4\r\nINFO\r\n");
        assertTrue(resp.contains("redis_version:1.0.0"), resp);
        assertTrue(resp.contains("db0:keys="), resp);
    }

    @Test
    void quitClosesConnectionAfterOk() throws IOException {
        try (Socket s = new Socket("127.0.0.1", server.boundPort())) {
            s.setSoTimeout(2000);
            OutputStream out = s.getOutputStream();
            out.write("*1\r\n$4\r\nQUIT\r\n".getBytes(StandardCharsets.UTF_8));
            out.flush();
            byte[] buf = new byte[16];
            int n = s.getInputStream().read(buf);
            assertEquals("+OK\r\n", new String(buf, 0, n, StandardCharsets.UTF_8));
            int eof = s.getInputStream().read();
            assertEquals(-1, eof);
        }
    }

    @Test
    void pxExpiryRemovesKey() throws IOException, InterruptedException {
        // SET k v PX 50
        try (Socket s = new Socket("127.0.0.1", server.boundPort())) {
            s.getOutputStream().write(
                    ("*5\r\n$3\r\nSET\r\n$1\r\nk\r\n$1\r\nv\r\n$2\r\nPX\r\n$2\r\n50\r\n")
                            .getBytes(StandardCharsets.UTF_8));
            s.getOutputStream().flush();
            byte[] buf = new byte[16];
            int n = s.getInputStream().read(buf);
            assertEquals("+OK\r\n", new String(buf, 0, n, StandardCharsets.UTF_8));
        }
        Thread.sleep(150L);
        String resp = sendAndRead("*2\r\n$3\r\nGET\r\n$1\r\nk\r\n");
        assertEquals("$-1\r\n", resp);
    }

    @Test
    void concurrentClientsAreHandledIndependently() throws Exception {
        int clients = 20;
        var pool = Executors.newFixedThreadPool(clients);
        List<Future<String>> futures = new ArrayList<>();
        for (int i = 0; i < clients; i++) {
            final int idx = i;
            futures.add(pool.submit(() -> {
                String key = "k" + (idx % 10);
                String cmd = "*3\r\n$3\r\nSET\r\n$" + key.length() + "\r\n" + key + "\r\n$1\r\nv\r\n"
                        + "*2\r\n$3\r\nGET\r\n$" + key.length() + "\r\n" + key + "\r\n";
                return sendAndRead(cmd);
            }));
        }
        for (Future<String> f : futures) {
            String resp = f.get(5, TimeUnit.SECONDS);
            assertEquals("+OK\r\n$1\r\nv\r\n", resp);
        }
        pool.shutdown();
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
    }

    @Test
    void concurrentIncrConverges() throws Exception {
        int clients = 10;
        int incrPerClient = 100;
        var pool = Executors.newFixedThreadPool(clients);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < clients; i++) {
            futures.add(pool.submit(() -> {
                StringBuilder sb = new StringBuilder();
                for (int k = 0; k < incrPerClient; k++) {
                    sb.append("*2\r\n$4\r\nINCR\r\n$7\r\ncounter\r\n");
                }
                try {
                    sendAndRead(sb.toString());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }));
        }
        for (Future<?> f : futures) f.get(10, TimeUnit.SECONDS);
        pool.shutdown();
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));

        String resp = sendAndRead("*2\r\n$3\r\nGET\r\n$7\r\ncounter\r\n");
        String expected = "$" + String.valueOf((long) clients * incrPerClient).length()
                + "\r\n" + ((long) clients * incrPerClient) + "\r\n";
        assertEquals(expected, resp);
    }

    @Test
    void rejectsConnectionsWhenPoolSaturated() throws Exception {
        // 1 worker + queue capacity 1: two busy handlers fill pool+queue; third is rejected
        Server saturated = new Server(0, 1, 1);
        saturated.start();
        try {
            Socket blocker1 = new Socket("127.0.0.1", saturated.boundPort());
            Socket blocker2 = new Socket("127.0.0.1", saturated.boundPort());
            // partial command keeps handlers alive without completing
            blocker1.getOutputStream().write("*1\r\n$4\r\nPING".getBytes(StandardCharsets.UTF_8));
            blocker1.getOutputStream().flush();
            blocker2.getOutputStream().write("*1\r\n$4\r\nPING".getBytes(StandardCharsets.UTF_8));
            blocker2.getOutputStream().flush();

            // give workers time to pick up the blocking sockets
            Thread.sleep(150);

            Socket rejected = new Socket("127.0.0.1", saturated.boundPort());
            rejected.setSoTimeout(500);
            int read = rejected.getInputStream().read();
            assertEquals(-1, read, "rejected connection should be closed by server");

            blocker1.close();
            blocker2.close();
            rejected.close();

            // server recovers after blocked clients disconnect
            Thread.sleep(100);
            String resp = sendAndReadOnPort(saturated.boundPort(), "*1\r\n$4\r\nPING\r\n");
            assertEquals("+PONG\r\n", resp);
        } finally {
            saturated.close();
        }
    }

    private String sendAndReadOnPort(int port, String command) throws IOException {
        try (Socket s = new Socket("127.0.0.1", port)) {
            OutputStream out = s.getOutputStream();
            out.write(command.getBytes(StandardCharsets.UTF_8));
            out.flush();
            s.shutdownOutput();
            return readAll(s.getInputStream());
        }
    }
}
