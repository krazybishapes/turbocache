package cache;

import backingstore.BackingStore;
import config.CacheConfig;
import policy.EvictionPolicy;
import policy.ExpirationStrategy;
import policy.LoadingMode;
import policy.WritePolicy;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;

public class TurboCache <K,V> implements Serializable, Cache<K,V> {
    private final Logger logger = Logger.getLogger(TurboCache.class.getName());
    /**
     * cache.Cache to store Key & Value pair,CacheEntry class to hold the value and its expiry time.
     * This is used to store the data in memory and fetching data in O(1) time complexity.
     * Concurrent Hashmap is used for thread safety.
     */
    private final ConcurrentHashMap<K, CacheEntry<V>> cache;
    /**
     * LinkedHashMap to store the key and value pair for LRU eviction policy.
     */
    private LinkedHashMap<K, V> lruCache = null;
    /**
     * Backing store to store the data in persistent storage. Data would be fetched from backing store
     * if not found in cache and also written to backing store if write policy is set to WRITE_THROUGH.
     */
    private final BackingStore<K, V> backingStore;
    /**
     * Write policy to determine how the data should be written to the backing store.
     * WRITE_THROUGH - write data to backing store immediately- This is implemented only
     * WRITE_BACK - write data to backing store when cache is full
     * WRITE_AROUND - write data to backing store when data is not found in cache
     * WRITE_BEHIND - write data to backing store in background
     * WRITE_THROUGH is the default policy.
     */
    private final WritePolicy writePolicy;

    /**
     * Maximum size of the cache.
     */
    private final int maxSize;
    /**
     * Time to live for the cache entry in milliseconds.
     */
    private final long ttl;

    /**
     * Scheduler to remove the expired entries from the cache.
     */
    private transient ScheduledExecutorService cleaner = null;

    /**
     * Executor service to load data from backing store asynchronously.
     */
    private transient ExecutorService asyncLoader = null;

    /**
     * Refresh duration for the cache entry in milliseconds.
     */
    private final long refreshDuration;

    /**
     * Eviction policy to determine how the data should be evicted from the cache.
     */
    private final EvictionPolicy evictionPolicy;

    /**
     * Expiration strategy to determine how the data should be expired from the cache.
     */
    private ExpirationStrategy expirationStrategy;

    /**
     * Callback function to be called when the cache entry is expired.
     */
    private transient final Consumer<K> expiryCallback;

    /**
     * Async loader function to load data from backing store asynchronously.
     */
    private transient final Function<K, V> asyncLoaderFunction;

    /**
     * Loading mode to determine how the data should be loaded from the backing store.
     */
    private final LoadingMode loadingMode;

    /**
     * Refresh scheduler to refresh keys from the backing store on a regular interval.
     */
    private transient ScheduledExecutorService refreshScheduler = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());;

    /**
     * Constructor to initialize the cache with the given configuration.
     * @param config CacheConfig object to initialize the cache with the given configuration.
     * @param backingStore BackingStore object to store the data in persistent storage.
     */
    public TurboCache(CacheConfig config, BackingStore<K, V> backingStore,
                      Consumer<K> expiryCallback,
                      Function<K, V> asyncLoaderFunction) {
        //Setting up cache property
        this.maxSize = config.getMaxSize();
        this.ttl = config.getTtl();
        this.refreshDuration = config.getRefreshDuration();
        this.cache = new ConcurrentHashMap<>(maxSize);
        this.evictionPolicy = config.getEvictionPolicy();
        this.backingStore = backingStore;
        this.writePolicy = config.getWritePolicy();
        this.expirationStrategy = config.getExpirationStrategy();
        this.loadingMode = config.getLoadingMode();
        this.expiryCallback = expiryCallback;
        this.asyncLoaderFunction = asyncLoaderFunction;

        //Data structure for LRU eviction policy
        if(this.evictionPolicy == EvictionPolicy.LRU) {
            this.lruCache = new LinkedHashMap<>(maxSize, 0.75f, true) {
                protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                    return size() > maxSize;
                }
            };
        }

        //Setting up scheduler for removing expired entries
        if (config.getExpirationStrategy() == ExpirationStrategy.TTL) {
            this.cleaner = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
        }

        //Setting up loader for loading data from backing store
        if (config.getLoadingMode() == LoadingMode.ASYNC) {
            this.asyncLoader = Executors.newSingleThreadExecutor();
        } else {
            this.asyncLoader = null;
        }
    }


    /**
     * Method to put the key and value in the cache.
     * This method is synchronized to make it thread safe.
     * Adding data to back store
     * Schedule cleanup and refresh for the key.
     * @param key
     * @param value
     */
    @Override
    public void put(K key, V value) {
        synchronized (lruCache) {
            if (lruCache.size() >= maxSize) {
                Iterator<K> iterator = lruCache.keySet().iterator();
                if (iterator.hasNext()) {
                    K eldestKey = iterator.next();
                    lruCache.remove(eldestKey);
                    cache.remove(eldestKey);
                }
            }
            lruCache.put(key, value);
        }

        long expiryTime = System.currentTimeMillis() + ttl;
        cache.put(key, new CacheEntry<>(value, expiryTime));

        //Only write through is implemented for now
        if (writePolicy.equals(WritePolicy.WRITE_THROUGH)) {
            backingStore.save(key, value);
        }

        scheduleCleanup(key, ttl);
        scheduleRefresh(key, refreshDuration);
    }


    /**
     * Method to get the value for the given key from the cache.
     * This method is synchronized to make it thread safe.
     * If the key is not found in the cache, it will be fetched from the backing store.
     * If the key is found in the cache, it will be returned from the cache.
     * If the key is expired, it will be removed from the cache and fetched from the backing store.
     * If the key is not found in the backing store, it will return null.
     * Schedule cleanup at every time it get access.
     * @param key
     * @return
     */

    @Override
    public V get(K key) {

        CacheEntry<V> entry = cache.get(key);
        if(entry != null && System.currentTimeMillis() < entry.expiryTime){
                synchronized (lruCache) {
                    lruCache.put(key, entry.value);
            }
            scheduleCleanup(key, ttl);
            return entry.value;
        }
        // Entry is expired or not found, remove it from cache
        cache.remove(key);

        //when there is any cache miss, get it from back store
        return getFromBackStore(key);
    }


    @Override
    public void remove(K key) {
        cache.remove(key);
        synchronized (lruCache) {
            lruCache.remove(key);
        }
        //backingStore.remove(key);
    }

    @Override
    public void clearCache() {
        cache.clear();
        synchronized (lruCache) {
            lruCache.clear();
        }
    }

    @Override
    public int size() {
        return cache.size();
    }

    private void scheduleCleanup(K key, long ttlMillis) {
        cleaner.schedule(() -> {
            CacheEntry<V> entry = cache.get(key);
            if (entry != null && System.currentTimeMillis() >= entry.expiryTime) {
                cache.remove(key);
                synchronized (lruCache) {
                    lruCache.remove(key);
                }
                expiryCallback.accept(key);
            }
        }, ttlMillis, TimeUnit.MILLISECONDS);
    }

    private void scheduleRefresh(K key, long refreshDuration) {
        cleaner.schedule(() -> {
            CacheEntry<V> entry = cache.get(key);
            if (entry != null) {

                // Fetch data from backing store and update the cache
                V newValue = asyncLoaderFunction.apply(key);
                if (newValue != null && !newValue.equals(entry.value)) {
                    put(key, newValue);
                }

            }
        }, refreshDuration, TimeUnit.MILLISECONDS);
    }

    private V loadAsync(K key) {
        CompletableFuture<V> future = CompletableFuture.supplyAsync(() -> asyncLoaderFunction.apply(key), asyncLoader);
        try {
            return future.get();
        } catch (Exception e) {
            logger.warning("Async loading failed for key: " + key);
            return null;
        }
    }


    private V getFromBackStore(K key) {
        if(loadingMode == LoadingMode.ASYNC) {
            return loadAsync(key);

        }else{
            V value = backingStore.load(key);
            if (value != null) {
                //add to cache
                put(key, value);
            }
            return value;
        }

    }

    public void shutdown() {
        cleaner.shutdown();
    }

}
