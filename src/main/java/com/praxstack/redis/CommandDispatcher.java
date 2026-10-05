package com.praxstack.redis;

import java.util.ArrayList;
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
        handlers.put("DECR", this::handleDecr);
        handlers.put("INCRBY", this::handleIncrBy);
        handlers.put("DECRBY", this::handleDecrBy);
        handlers.put("EXISTS", this::handleExists);
        handlers.put("COMMAND", args -> RespEncoder.emptyArray());
        handlers.put("CONFIG", args -> RespEncoder.emptyArray());
        handlers.put("DBSIZE", args -> RespEncoder.integer(store.size()));
        handlers.put("MGET", this::handleMget);
        handlers.put("MSET", this::handleMset);
        handlers.put("EXPIRE", this::handleExpire);
        handlers.put("PEXPIRE", this::handlePexpire);
        handlers.put("TTL", this::handleTtl);
        handlers.put("PTTL", this::handlePttl);
        handlers.put("PERSIST", this::handlePersist);
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
     * SET key value [NX|XX] [EX seconds|PX milliseconds] — options in any order.
     */
    private byte[] handleSet(List<String> args) {
        if (args.size() < 3) return RespEncoder.error("ERR wrong number of arguments for 'set'");
        String key = args.get(1);
        String value = args.get(2);
        boolean nx = false;
        boolean xx = false;
        Long ttlMillis = null;
        for (int i = 3; i < args.size(); i++) {
            String opt = args.get(i).toUpperCase(Locale.ROOT);
            switch (opt) {
                case "NX" -> nx = true;
                case "XX" -> xx = true;
                case "EX" -> {
                    if (i + 1 >= args.size()) {
                        return RespEncoder.error("ERR syntax error");
                    }
                    ttlMillis = Long.parseLong(args.get(++i)) * 1000L;
                }
                case "PX" -> {
                    if (i + 1 >= args.size()) {
                        return RespEncoder.error("ERR syntax error");
                    }
                    ttlMillis = Long.parseLong(args.get(++i));
                }
                default -> {
                    return RespEncoder.error("ERR syntax error");
                }
            }
        }
        if (nx && xx) {
            return RespEncoder.error("ERR syntax error");
        }
        boolean applied = store.setConditional(key, value, ttlMillis, nx, xx);
        return applied ? RespEncoder.ok() : RespEncoder.nullBulk();
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

    private byte[] handleDecr(List<String> args) {
        if (args.size() < 2) return RespEncoder.error("ERR wrong number of arguments for 'decr'");
        return RespEncoder.integer(store.incrBy(args.get(1), -1L));
    }

    private byte[] handleIncrBy(List<String> args) {
        if (args.size() != 3) return RespEncoder.error("ERR wrong number of arguments for 'incrby'");
        return RespEncoder.integer(store.incrBy(args.get(1), Long.parseLong(args.get(2))));
    }

    private byte[] handleDecrBy(List<String> args) {
        if (args.size() != 3) return RespEncoder.error("ERR wrong number of arguments for 'decrby'");
        return RespEncoder.integer(store.incrBy(args.get(1), Math.negateExact(Long.parseLong(args.get(2)))));
    }

    private byte[] handleExists(List<String> args) {
        if (args.size() < 2) return RespEncoder.error("ERR wrong number of arguments for 'exists'");
        int count = 0;
        for (int i = 1; i < args.size(); i++) {
            if (store.exists(args.get(i))) count++;
        }
        return RespEncoder.integer(count);
    }

    private byte[] handleMget(List<String> args) {
        if (args.size() < 2) return RespEncoder.error("ERR wrong number of arguments for 'mget'");
        ArrayList<String> values = new ArrayList<>(args.size() - 1);
        for (int i = 1; i < args.size(); i++) {
            values.add(store.get(args.get(i)).orElse(null));
        }
        return RespEncoder.bulkArray(values);
    }

    private byte[] handleMset(List<String> args) {
        if (args.size() < 3 || ((args.size() - 1) % 2) != 0) {
            return RespEncoder.error("ERR wrong number of arguments for 'mset'");
        }
        for (int i = 1; i < args.size(); i += 2) {
            store.set(args.get(i), args.get(i + 1));
        }
        return RespEncoder.ok();
    }

    private byte[] handleExpire(List<String> args) {
        if (args.size() != 3) return RespEncoder.error("ERR wrong number of arguments for 'expire'");
        long seconds = Long.parseLong(args.get(2));
        boolean ok = store.expireMillis(args.get(1), Math.multiplyExact(seconds, 1000L));
        return RespEncoder.integer(ok ? 1 : 0);
    }

    private byte[] handlePexpire(List<String> args) {
        if (args.size() != 3) return RespEncoder.error("ERR wrong number of arguments for 'pexpire'");
        boolean ok = store.expireMillis(args.get(1), Long.parseLong(args.get(2)));
        return RespEncoder.integer(ok ? 1 : 0);
    }

    private byte[] handleTtl(List<String> args) {
        if (args.size() != 2) return RespEncoder.error("ERR wrong number of arguments for 'ttl'");
        return RespEncoder.integer(store.ttlSeconds(args.get(1)));
    }

    private byte[] handlePttl(List<String> args) {
        if (args.size() != 2) return RespEncoder.error("ERR wrong number of arguments for 'pttl'");
        return RespEncoder.integer(store.pttl(args.get(1)));
    }

    private byte[] handlePersist(List<String> args) {
        if (args.size() != 2) return RespEncoder.error("ERR wrong number of arguments for 'persist'");
        return RespEncoder.integer(store.persist(args.get(1)) ? 1 : 0);
    }
}
