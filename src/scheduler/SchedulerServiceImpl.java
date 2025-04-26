package scheduler;


import cache.CacheEntry;
import cache.InMemoryCache;
import cache.LRUCache;
import loader.BackStoreDataLoader;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;


public class SchedulerServiceImpl<K,V> implements SchedulerService<K, V> {
    private final Logger logger = Logger.getLogger(SchedulerServiceImpl.class.getName());

    private final long ttl;
    private final long refreshDuration;
    /**
     * Scheduler to remove the expired entries from the cache.
     */
    private transient ScheduledExecutorService scheduler;


    public SchedulerServiceImpl(long ttl, long refreshDuration) {
        this.ttl = ttl;
        this.refreshDuration = refreshDuration;
        this.scheduler = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
    }

    @Override
    public void scheduleCleanup(K key, InMemoryCache<K,V> cache) {
        scheduler.schedule(() -> {
            CacheEntry<V> entry = cache.get(key);
            if (entry != null) {
                // Check if the entry is expired
                if (System.currentTimeMillis() > entry.getExpiryTime()) {
                    cache.remove(key);
                    logger.info("Key " + key + " has been removed from the cache due to expiration.");
                }
            }
        }, ttl, TimeUnit.MILLISECONDS);
    }

    @Override
    public void scheduleRefresh(K key, InMemoryCache<K,V> cache, BackStoreDataLoader<K,V> dataLoader) {
        scheduler.scheduleAtFixedRate(() -> {
            CacheEntry<V> entry = cache.get(key);
            if (entry != null) {

                // Fetch data from backing store and update the cache
                V newValue = dataLoader.load(key);
                if (newValue != null && !newValue.equals(entry.getValue())) {
                    cache.put(key, newValue);
                    logger.info("Key " + key + " has been refreshed in the cache.");
                }

            }
        }, refreshDuration, refreshDuration, TimeUnit.MILLISECONDS);
    }

}
