package com.praxstack.redis;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Background active-expiry sweeper.
 *
 * <p>Redis uses a hybrid eviction model: keys are lazy-checked on read, but a
 * periodic background job also sweeps expired keys so memory doesn't balloon
 * when keys are written-but-never-read. This class implements the background
 * half via {@link ScheduledExecutorService}.
 */
public final class ExpiryManager implements AutoCloseable {

    private static final Logger LOG = Logger.getLogger(ExpiryManager.class.getName());
    private static final long SWEEP_INTERVAL_MS = 100L;

    private final Store store;
    private final ScheduledExecutorService scheduler;

    public ExpiryManager(Store store) {
        this.store = store;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "redis-expiry-sweep");
            t.setDaemon(true);
            return t;
        });
    }

    public void start() {
        scheduler.scheduleAtFixedRate(this::sweepSafe,
                SWEEP_INTERVAL_MS, SWEEP_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private void sweepSafe() {
        try {
            int purged = store.purgeExpired();
            if (purged > 0) {
                LOG.log(Level.FINE, () -> "expiry sweep purged " + purged + " keys");
            }
        } catch (RuntimeException ex) {
            LOG.log(Level.WARNING, "expiry sweep failed", ex);
        }
    }

    @Override
    public void close() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            scheduler.shutdownNow();
        }
    }
}
