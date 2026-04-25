package com.praxstack.redis;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dispatches parsed RESP commands to their handlers.
 *
 * <p>Handler registration uses a {@link ConcurrentHashMap} keyed by
 * UPPERCASE command name, matching Redis' case-insensitive command parsing.
 */
public final class CommandDispatcher {

    @FunctionalInterface
    interface CommandHandler {
        byte[] handle(List<String> args);
    }

    private final Store store;
    private final Map<String, CommandHandler> handlers = new ConcurrentHashMap<>();

    public CommandDispatcher(Store store) {
        this.store = store;
        registerBuiltins();
    }

    private void registerBuiltins() {
        handlers.put("PING", this::handlePing);
        handlers.put("ECHO", this::handleEcho);
        handlers.put("SET", this::handleSet);
        handlers.put("GET", this::handleGet);
        handlers.put("DEL", this::handleDel);
        handlers.put("INCR", this::handleIncr);
        handlers.put("EXISTS", this::handleExists);
        handlers.put("COMMAND", args -> RespEncoder.emptyArray());
        handlers.put("CONFIG", args -> RespEncoder.emptyArray());
        handlers.put("DBSIZE", args -> RespEncoder.integer(store.size()));
    }

    public byte[] dispatch(List<String> args) {
        if (args == null || args.isEmpty()) {
            return RespEncoder.error("ERR empty command");
        }
        String cmd = args.get(0).toUpperCase(Locale.ROOT);
        CommandHandler h = handlers.get(cmd);
        if (h == null) {
            return RespEncoder.error("ERR unknown command '" + args.get(0) + "'");
        }
        try {
            return h.handle(args);
        } catch (NumberFormatException nfe) {
            return RespEncoder.error("ERR value is not an integer or out of range");
        } catch (RuntimeException ex) {
            return RespEncoder.error("ERR " + ex.getMessage());
        }
    }

    // ─── handlers ────────────────────────────────────────────────────────

    private byte[] handlePing(List<String> args) {
        if (args.size() == 1) return RespEncoder.simpleString("PONG");
        return RespEncoder.bulkString(args.get(1));
    }

    private byte[] handleEcho(List<String> args) {
        if (args.size() < 2) return RespEncoder.error("ERR wrong number of arguments for 'echo'");
        return RespEncoder.bulkString(args.get(1));
    }

    /**
     * SET key value [PX milliseconds]
     */
    private byte[] handleSet(List<String> args) {
        if (args.size() < 3) return RespEncoder.error("ERR wrong number of arguments for 'set'");
        String key = args.get(1);
        String value = args.get(2);
        if (args.size() >= 5 && "PX".equalsIgnoreCase(args.get(3))) {
            long ttl = Long.parseLong(args.get(4));
            store.setWithTtlMillis(key, value, ttl);
        } else if (args.size() >= 5 && "EX".equalsIgnoreCase(args.get(3))) {
            long ttl = Long.parseLong(args.get(4)) * 1000L;
            store.setWithTtlMillis(key, value, ttl);
        } else {
            store.set(key, value);
        }
        return RespEncoder.ok();
    }

    private byte[] handleGet(List<String> args) {
        if (args.size() < 2) return RespEncoder.error("ERR wrong number of arguments for 'get'");
        Optional<String> v = store.get(args.get(1));
        return v.map(RespEncoder::bulkString).orElse(RespEncoder.nullBulk());
    }

    private byte[] handleDel(List<String> args) {
        if (args.size() < 2) return RespEncoder.error("ERR wrong number of arguments for 'del'");
        int removed = 0;
        for (int i = 1; i < args.size(); i++) {
            if (store.delete(args.get(i))) removed++;
        }
        return RespEncoder.integer(removed);
    }

    private byte[] handleIncr(List<String> args) {
        if (args.size() < 2) return RespEncoder.error("ERR wrong number of arguments for 'incr'");
        long v = store.incr(args.get(1));
        return RespEncoder.integer(v);
    }

    private byte[] handleExists(List<String> args) {
        if (args.size() < 2) return RespEncoder.error("ERR wrong number of arguments for 'exists'");
        int count = 0;
        for (int i = 1; i < args.size(); i++) {
            if (store.exists(args.get(i))) count++;
        }
        return RespEncoder.integer(count);
    }
}
