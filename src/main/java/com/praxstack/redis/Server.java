package com.praxstack.redis;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * RESP2-compatible Redis server.
 *
 * <p>Uses a thread-per-client model backed by a bounded {@link ExecutorService}
 * to prevent resource exhaustion under load. Backing store is a
 * {@link java.util.concurrent.ConcurrentHashMap} with hybrid TTL eviction.
 *
 * <pre>
 *   Client --TCP--> ServerSocket --accept()--> ExecutorService (fixed pool)
 *                                                     |
 *                                                     v
 *                                               ClientHandler
 *                                                     |
 *                                RespParser -&gt; CommandDispatcher -&gt; Store
 *                                                                    ^
 *                                                         ExpiryManager (sweeps)
 * </pre>
 */
public final class Server implements AutoCloseable {

    private static final Logger LOG = Logger.getLogger(Server.class.getName());
    private static final int DEFAULT_PORT = 6379;
    private static final int DEFAULT_WORKER_THREADS = 100;

    private final int port;
    private final ExecutorService workers;
    private final Store store;
    private final ExpiryManager expiryManager;
    private final CommandDispatcher dispatcher;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private ServerSocket serverSocket;
    private Thread acceptorThread;

    public Server(int port, int workerThreads) {
        this.port = port;
        this.workers = Executors.newFixedThreadPool(workerThreads, r -> {
            Thread t = new Thread(r, "redis-worker");
            t.setDaemon(true);
            return t;
        });
        this.store = new Store();
        this.expiryManager = new ExpiryManager(store);
        this.dispatcher = new CommandDispatcher(store);
    }

    public Server(int port) {
        this(port, DEFAULT_WORKER_THREADS);
    }

    /** Bind the port and start accepting clients on a background thread. */
    public void start() throws IOException {
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("Server already running");
        }
        this.serverSocket = new ServerSocket();
        this.serverSocket.setReuseAddress(true);
        this.serverSocket.bind(new InetSocketAddress(port));
        expiryManager.start();
        acceptorThread = new Thread(this::acceptLoop, "redis-acceptor");
        acceptorThread.setDaemon(true);
        acceptorThread.start();
        LOG.info(() -> "Redis server listening on port " + port);
    }

    /** The actual bound port (useful for tests when port=0). */
    public int boundPort() {
        return serverSocket == null ? -1 : serverSocket.getLocalPort();
    }

    private void acceptLoop() {
        while (running.get()) {
            try {
                Socket client = serverSocket.accept();
                client.setTcpNoDelay(true);
                workers.submit(new ClientHandler(client, dispatcher));
            } catch (IOException ex) {
                if (running.get()) {
                    LOG.log(Level.WARNING, "accept() failed", ex);
                }
            }
        }
    }

    /** Graceful shutdown: stop accepting, drain workers, close sockets. */
    @Override
    public void close() {
        if (!running.compareAndSet(true, false)) {
            return;
        }
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException ignored) {
        }
        expiryManager.close();
        workers.shutdown();
        try {
            if (!workers.awaitTermination(5, TimeUnit.SECONDS)) {
                workers.shutdownNow();
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            workers.shutdownNow();
        }
        LOG.info("Redis server stopped");
    }

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        Server server = new Server(port);
        Runtime.getRuntime().addShutdownHook(new Thread(server::close, "redis-shutdown-hook"));
        server.start();
        // block main thread
        try {
            Thread.currentThread().join();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
