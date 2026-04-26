package com.praxstack.redis;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles a single client connection: reads RESP commands in a loop, dispatches
 * them, and writes back responses. Runs on a worker from {@link Server}'s pool.
 */
public final class ClientHandler implements Runnable {

    private static final Logger LOG = Logger.getLogger(ClientHandler.class.getName());

    private final Socket socket;
    private final CommandDispatcher dispatcher;

    public ClientHandler(Socket socket, CommandDispatcher dispatcher) {
        this.socket = socket;
        this.dispatcher = dispatcher;
    }

    @Override
    public void run() {
        try (socket;
             BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
             OutputStream out = socket.getOutputStream()) {

            RespParser parser = new RespParser(in);
            List<String> args;
            while ((args = parser.next()) != null) {
                byte[] response = dispatcher.dispatch(args);
                out.write(response);
                out.flush();
            }
        } catch (IOException ex) {
            LOG.log(Level.FINE, "client disconnected: " + ex.getMessage());
        } catch (RuntimeException ex) {
            LOG.log(Level.WARNING, "client handler crashed", ex);
        }
    }
}
