package com.praxstack.redis;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CommandDispatcherTest {

    private String dispatch(CommandDispatcher d, String... args) {
        return new String(d.dispatch(List.of(args)), StandardCharsets.UTF_8);
    }

    @Test
    void pingReturnsPong() {
        CommandDispatcher d = new CommandDispatcher(new Store());
        assertEquals("+PONG\r\n", dispatch(d, "PING"));
    }

    @Test
    void pingWithMessageEchoesBulk() {
        CommandDispatcher d = new CommandDispatcher(new Store());
        assertEquals("$5\r\nhello\r\n", dispatch(d, "PING", "hello"));
    }

    @Test
    void echoReturnsArgumentAsBulk() {
        CommandDispatcher d = new CommandDispatcher(new Store());
        assertEquals("$3\r\nhey\r\n", dispatch(d, "ECHO", "hey"));
    }

    @Test
    void setThenGetReturnsValue() {
        CommandDispatcher d = new CommandDispatcher(new Store());
        assertEquals("+OK\r\n", dispatch(d, "SET", "k", "v"));
        assertEquals("$1\r\nv\r\n", dispatch(d, "GET", "k"));
    }

    @Test
    void getMissingReturnsNullBulk() {
        CommandDispatcher d = new CommandDispatcher(new Store());
        assertEquals("$-1\r\n", dispatch(d, "GET", "missing"));
    }

    @Test
    void delReturnsCountOfRemovedKeys() {
        CommandDispatcher d = new CommandDispatcher(new Store());
        dispatch(d, "SET", "a", "1");
        dispatch(d, "SET", "b", "2");
        assertEquals(":2\r\n", dispatch(d, "DEL", "a", "b", "c"));
    }

    @Test
    void existsCountsPresentKeys() {
        CommandDispatcher d = new CommandDispatcher(new Store());
        dispatch(d, "SET", "a", "1");
        assertEquals(":1\r\n", dispatch(d, "EXISTS", "a", "b"));
    }

    @Test
    void incrIncrementsAndReturnsNewValue() {
        CommandDispatcher d = new CommandDispatcher(new Store());
        assertEquals(":1\r\n", dispatch(d, "INCR", "counter"));
        assertEquals(":2\r\n", dispatch(d, "INCR", "counter"));
    }

    @Test
    void decrAndIncrByStep() {
        CommandDispatcher d = new CommandDispatcher(new Store());
        assertEquals(":-1\r\n", dispatch(d, "DECR", "c"));
        assertEquals(":9\r\n", dispatch(d, "INCRBY", "c", "10"));
        assertEquals(":4\r\n", dispatch(d, "DECRBY", "c", "5"));
    }

    @Test
    void appendAndStrlenUseUtf8ByteLength() {
        CommandDispatcher d = new CommandDispatcher(new Store());
        assertEquals(":0\r\n", dispatch(d, "STRLEN", "k"));
        assertEquals(":5\r\n", dispatch(d, "APPEND", "k", "hello"));
        assertEquals(":7\r\n", dispatch(d, "APPEND", "k", "é"));
        assertEquals(":7\r\n", dispatch(d, "STRLEN", "k"));
        assertEquals("$7\r\nhelloé\r\n", dispatch(d, "GET", "k"));
    }

    @Test
    void unknownCommandReturnsError() {
        CommandDispatcher d = new CommandDispatcher(new Store());
        String out = dispatch(d, "FOOBAR");
        assertTrue(out.startsWith("-ERR unknown command"), out);
    }

    @Test
    void setWithPxExpiresKey() throws InterruptedException {
        CommandDispatcher d = new CommandDispatcher(new Store());
        dispatch(d, "SET", "k", "v", "PX", "30");
        Thread.sleep(80L);
        assertEquals("$-1\r\n", dispatch(d, "GET", "k"));
    }

    @Test
    void setNxSucceedsOnlyWhenMissing() {
        CommandDispatcher d = new CommandDispatcher(new Store());
        assertEquals("+OK\r\n", dispatch(d, "SET", "k", "a", "NX"));
        assertEquals("$-1\r\n", dispatch(d, "SET", "k", "b", "NX"));
        assertEquals("$1\r\na\r\n", dispatch(d, "GET", "k"));
    }

    @Test
    void setXxSucceedsOnlyWhenPresent() {
        CommandDispatcher d = new CommandDispatcher(new Store());
        assertEquals("$-1\r\n", dispatch(d, "SET", "k", "a", "XX"));
        dispatch(d, "SET", "k", "a");
        assertEquals("+OK\r\n", dispatch(d, "SET", "k", "b", "XX", "PX", "5000"));
        assertEquals("$1\r\nb\r\n", dispatch(d, "GET", "k"));
    }

    @Test
    void setNxAndXxTogetherIsSyntaxError() {
        CommandDispatcher d = new CommandDispatcher(new Store());
        String out = dispatch(d, "SET", "k", "v", "NX", "XX");
        assertTrue(out.startsWith("-ERR syntax error"), out);
    }

    @Test
    void setNxOnExpiredKeySucceeds() throws InterruptedException {
        CommandDispatcher d = new CommandDispatcher(new Store());
        dispatch(d, "SET", "k", "old", "PX", "30");
        Thread.sleep(80L);
        assertEquals("+OK\r\n", dispatch(d, "SET", "k", "new", "NX"));
        assertEquals("$3\r\nnew\r\n", dispatch(d, "GET", "k"));
    }

    @Test
    void commandIntrospectionReturnsEmptyArray() {
        CommandDispatcher d = new CommandDispatcher(new Store());
        assertEquals("*0\r\n", dispatch(d, "COMMAND"));
        assertEquals("*0\r\n", dispatch(d, "CONFIG", "GET", "maxmemory"));
    }

    @Test
    void commandNamesAreCaseInsensitive() {
        CommandDispatcher d = new CommandDispatcher(new Store());
        assertEquals("+PONG\r\n", dispatch(d, "ping"));
        assertEquals("+PONG\r\n", dispatch(d, "Ping"));
    }

    @Test
    void incrOnBadValueReturnsTypedError() {
        CommandDispatcher d = new CommandDispatcher(new Store());
        dispatch(d, "SET", "k", "notanumber");
        assertEquals("-ERR value is not an integer or out of range\r\n", dispatch(d, "INCR", "k"));
    }

    @Test
    void msetThenMgetReturnsValuesAndNils() {
        CommandDispatcher d = new CommandDispatcher(new Store());
        assertEquals("+OK\r\n", dispatch(d, "MSET", "a", "1", "b", "2"));
        assertEquals("*3\r\n$1\r\n1\r\n$1\r\n2\r\n$-1\r\n", dispatch(d, "MGET", "a", "b", "missing"));
    }

    @Test
    void msetRejectsOddArity() {
        CommandDispatcher d = new CommandDispatcher(new Store());
        String out = dispatch(d, "MSET", "a", "1", "b");
        assertTrue(out.startsWith("-ERR wrong number of arguments"), out);
    }

    @Test
    void mgetRequiresAtLeastOneKey() {
        CommandDispatcher d = new CommandDispatcher(new Store());
        String out = dispatch(d, "MGET");
        assertTrue(out.startsWith("-ERR wrong number of arguments"), out);
    }

    @Test
    void expireAndTtlRoundTrip() throws InterruptedException {
        CommandDispatcher d = new CommandDispatcher(new Store());
        assertEquals(":0\r\n", dispatch(d, "EXPIRE", "missing", "10"));
        dispatch(d, "SET", "k", "v");
        assertEquals(":1\r\n", dispatch(d, "PEXPIRE", "k", "200"));
        String pttl = dispatch(d, "PTTL", "k");
        assertTrue(pttl.startsWith(":"), pttl);
        long remaining = Long.parseLong(pttl.substring(1).trim());
        assertTrue(remaining > 0 && remaining <= 200, pttl);
        assertEquals(":1\r\n", dispatch(d, "PERSIST", "k"));
        assertEquals(":-1\r\n", dispatch(d, "TTL", "k"));
        assertEquals(":0\r\n", dispatch(d, "PERSIST", "k"));
        dispatch(d, "PEXPIRE", "k", "40");
        Thread.sleep(80L);
        assertEquals(":-2\r\n", dispatch(d, "TTL", "k"));
    }

    @Test
    void getDelReturnsValueAndRemovesKey() {
        CommandDispatcher d = new CommandDispatcher(new Store());
        dispatch(d, "SET", "k", "v");
        assertEquals("$1\r\nv\r\n", dispatch(d, "GETDEL", "k"));
        assertEquals("$-1\r\n", dispatch(d, "GET", "k"));
        assertEquals("$-1\r\n", dispatch(d, "GETDEL", "k"));
    }

    @Test
    void flushDbClearsKeysAndDbsizeIgnoresExpired() throws InterruptedException {
        CommandDispatcher d = new CommandDispatcher(new Store());
        dispatch(d, "SET", "a", "1");
        dispatch(d, "SET", "b", "2", "PX", "30");
        assertEquals(":2\r\n", dispatch(d, "DBSIZE"));
        Thread.sleep(80L);
        assertEquals(":1\r\n", dispatch(d, "DBSIZE"));
        assertEquals("+OK\r\n", dispatch(d, "FLUSHDB"));
        assertEquals(":0\r\n", dispatch(d, "DBSIZE"));
        dispatch(d, "SET", "c", "3");
        assertEquals("+OK\r\n", dispatch(d, "FLUSHALL"));
        assertEquals(":0\r\n", dispatch(d, "DBSIZE"));
    }

    @Test
    void typeReturnsStringOrNone() throws InterruptedException {
        CommandDispatcher d = new CommandDispatcher(new Store());
        assertEquals("+none\r\n", dispatch(d, "TYPE", "k"));
        dispatch(d, "SET", "k", "v");
        assertEquals("+string\r\n", dispatch(d, "TYPE", "k"));
        dispatch(d, "PEXPIRE", "k", "30");
        Thread.sleep(80L);
        assertEquals("+none\r\n", dispatch(d, "TYPE", "k"));
    }
}
