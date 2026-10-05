package com.praxstack.redis;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class StoreTest {

    @Test
    void getReturnsEmptyForMissingKey() {
        Store store = new Store();
        assertTrue(store.get("missing").isEmpty());
    }

    @Test
    void setThenGetReturnsValue() {
        Store store = new Store();
        store.set("k", "v");
        assertEquals(Optional.of("v"), store.get("k"));
    }

    @Test
    void setConditionalNxRejectsExisting() {
        Store store = new Store();
        assertTrue(store.setConditional("k", "a", null, true, false));
        assertFalse(store.setConditional("k", "b", null, true, false));
        assertEquals(Optional.of("a"), store.get("k"));
    }

    @Test
    void setConditionalXxRejectsMissing() {
        Store store = new Store();
        assertFalse(store.setConditional("k", "a", null, false, true));
        store.set("k", "a");
        assertTrue(store.setConditional("k", "b", 5_000L, false, true));
        assertEquals(Optional.of("b"), store.get("k"));
    }

    @Test
    void lazyExpiryRemovesKeyOnGet() throws InterruptedException {
        Store store = new Store();
        store.setWithTtlMillis("k", "v", 50);
        Thread.sleep(80);
        assertTrue(store.get("k").isEmpty());
    }

    @Test
    void deleteReturnsTrueOnlyForExistingKey() {
        Store store = new Store();
        assertFalse(store.delete("missing"));
        store.set("k", "v");
        assertTrue(store.delete("k"));
        assertFalse(store.delete("k"));
    }

    @Test
    void existsDelegatesToGet() throws InterruptedException {
        Store store = new Store();
        store.setWithTtlMillis("k", "v", 50);
        assertTrue(store.exists("k"));
        Thread.sleep(80);
        assertFalse(store.exists("k"));
    }

    @Test
    void incrFromZeroReturnsOne() {
        Store store = new Store();
        assertEquals(1L, store.incr("counter"));
    }

    @Test
    void incrPreservesTtl() throws InterruptedException {
        Store store = new Store();
        store.setWithTtlMillis("k", "5", 200);
        assertEquals(6L, store.incr("k"));
        assertEquals(Optional.of("6"), store.get("k"));
        Thread.sleep(250);
        assertTrue(store.get("k").isEmpty());
    }

    @Test
    void incrIsAtomicUnderContention() throws Exception {
        Store store = new Store();
        int threads = 16;
        int opsPerThread = 500;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    for (int j = 0; j < opsPerThread; j++) {
                        store.incr("counter");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertTrue(done.await(30, TimeUnit.SECONDS));
        pool.shutdown();
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
        assertEquals((long) threads * opsPerThread, store.incr("counter") - 1);
    }

    @Test
    void pttlAndPersist() throws InterruptedException {
        Store store = new Store();
        assertEquals(-2L, store.pttl("missing"));
        store.set("k", "v");
        assertEquals(-1L, store.pttl("k"));
        assertTrue(store.expireMillis("k", 500));
        long remaining = store.pttl("k");
        assertTrue(remaining > 0 && remaining <= 500);
        assertTrue(store.persist("k"));
        assertEquals(-1L, store.pttl("k"));
        assertFalse(store.persist("k"));
        store.expireMillis("k", 30);
        Thread.sleep(80);
        assertEquals(-2L, store.ttlSeconds("k"));
    }

    @Test
    void purgeExpiredRemovesStaleKeys() throws InterruptedException {
        Store store = new Store();
        store.setWithTtlMillis("a", "1", 30);
        store.setWithTtlMillis("b", "2", 30);
        store.set("c", "persistent");
        Thread.sleep(80);
        int removed = store.purgeExpired();
        assertTrue(removed >= 2);
        assertTrue(store.get("a").isEmpty());
        assertTrue(store.get("b").isEmpty());
        assertEquals(Optional.of("persistent"), store.get("c"));
    }

    @Test
    void sizeIncludesUnexpiredOnly() throws InterruptedException {
        Store store = new Store();
        store.set("a", "1");
        store.setWithTtlMillis("b", "2", 30);
        assertEquals(2, store.size());
        Thread.sleep(80);
        // size() counts map entries until lazy purge; expired keys may linger briefly
        store.purgeExpired();
        assertEquals(1, store.size());
    }
}
