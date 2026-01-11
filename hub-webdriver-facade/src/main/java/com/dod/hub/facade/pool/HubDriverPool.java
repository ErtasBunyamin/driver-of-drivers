package com.dod.hub.facade.pool;

import com.dod.hub.core.config.HubConfig;
import com.dod.hub.facade.HubFactory;
import com.dod.hub.facade.HubWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Singleton pool for managing reusable HubWebDriver instances.
 * <p>
 * This pool distinguishes drivers based on their unique configuration signature
 * (Provider + Browser + Headless + GridUrl).
 * Drivers are borrowed when needed and returned (after cleanup) when tests
 * finish.
 */
public class HubDriverPool {

    private static final Logger log = LoggerFactory.getLogger(HubDriverPool.class);
    private static final HubDriverPool INSTANCE = new HubDriverPool();
    private static final long DEFAULT_WAIT_TIMEOUT_MS = 30000;

    private final Map<String, BlockingQueue<HubWebDriver>> poolStore = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> activeCounts = new ConcurrentHashMap<>();

    private HubDriverPool() {
    }

    public static HubDriverPool getInstance() {
        return INSTANCE;
    }

    /**
     * Borrows a driver instance from the pool if one matches the configuration.
     * If no matching driver is available in the pool and the maximum active limit
     * is not reached,
     * a new one is created.
     * <p>
     * If the pool is exhausted (active limit reached and pool is empty), this
     * method
     * will WAIT (block) for up to 30 seconds for a driver to become available.
     *
     * @param config The configuration to match (or create with).
     * @return A ready-to-use {@link HubWebDriver} instance.
     * @throws RuntimeException if the wait times out or thread is interrupted.
     */
    public HubWebDriver borrowDriver(HubConfig config) {
        if (!config.isPoolingEnabled()) {
            return HubFactory.create(config);
        }

        String key = generateKey(config);
        java.util.concurrent.BlockingQueue<HubWebDriver> queue = poolStore.computeIfAbsent(key,
                k -> new java.util.concurrent.LinkedBlockingQueue<>());
        AtomicInteger active = activeCounts.computeIfAbsent(key, k -> new AtomicInteger(0));

        HubWebDriver driver = queue.poll();
        if (driver != null) {
            log.debug("Borrowed driver from pool: {}", key);
            return driver;
        }

        int newCount = active.incrementAndGet();
        if (newCount <= config.getPoolMaxActive()) {
            log.debug("Creating new pooled driver: {}", key);
            try {
                return HubFactory.create(config);
            } catch (Exception e) {
                active.decrementAndGet();
                throw e;
            }
        } else {
            active.decrementAndGet();
        }

        log.debug("Pool exhausted for {}, waiting for driver...", key);
        try {
            driver = queue.poll(DEFAULT_WAIT_TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS);
            if (driver != null) {
                log.debug("Borrowed driver from pool after wait: {}", key);
                return driver;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for driver", e);
        }

        throw new RuntimeException("Driver pool exhausted and timed out for key: " + key);
    }

    /**
     * Returns a used driver to the pool for potential reuse.
     * <p>
     * Before returning, this method performs:
     * 1. A robust health check (`getCurrentUrl()`).
     * 2. Comprehensive data cleanup (cookies, session/local storage).
     * <p>
     * If the driver is found to be unhealthy or if cleanup fails, it is destroyed
     * (`quit()`)
     * and removed from the active count, rather than being returned to the pool.
     *
     * @param driver The driver instance to return.
     * @param config The configuration associated with this driver (used for key
     *               generation).
     */
    public void returnDriver(HubWebDriver driver, HubConfig config) {
        if (!config.isPoolingEnabled()) {
            driver.quit();
            return;
        }

        String key = generateKey(config);

        try {
            driver.getCurrentUrl();
            driver.manage().deleteAllCookies();

            try {
                driver.executeScript("window.sessionStorage.clear(); window.localStorage.clear();");
            } catch (Exception ignored) {
            }

            BlockingQueue<HubWebDriver> queue = poolStore.get(key);
            if (queue == null) {
                log.info("Driver returned to empty/cleared pool. Discarding driver: {}", key);
                driver.quit();
                return;
            }

            if (!queue.offer(driver)) {
                log.warn("Failed to return driver to queue (full): {}", key);
                driver.quit();
                AtomicInteger count = activeCounts.get(key);
                if (count != null) {
                    count.decrementAndGet();
                }
            } else {
                log.debug("Returned driver to pool: {}", key);
            }

        } catch (Exception e) {
            log.warn("Driver is unhealthy or disconnected. Discarding from pool. Key: {}, Error: {}", key,
                    e.getMessage());
            try {
                driver.quit();
            } catch (Exception ignored) {
            }
            AtomicInteger count = activeCounts.get(key);
            if (count != null) {
                count.decrementAndGet();
            }
        }
    }

    /**
     * Clear all pooled drivers.
     * <p>
     * This iterates through all queues in the pool, quits every driver, and clears
     * the internal maps.
     * It is typically called on application shutdown.
     */
    public void clear() {
        poolStore.forEach((key, queue) -> {
            HubWebDriver driver;
            while ((driver = queue.poll()) != null) {
                try {
                    driver.quit();
                } catch (Exception ignored) {
                }
            }
        });
        poolStore.clear();
        activeCounts.clear();
    }

    private String generateKey(HubConfig config) {
        return String.format("%s:%s:%s:%s",
                config.getProvider(),
                config.getBrowser(),
                config.isHeadless(),
                config.getGridUrl() == null ? "local" : config.getGridUrl());
    }
}
