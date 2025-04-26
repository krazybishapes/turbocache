package cache;

import config.CacheConfig;
import loader.BackStoreDataLoader;
import scheduler.SchedulerService;
import scheduler.SchedulerServiceImpl;

import java.io.Serial;
import java.io.Serializable;
import java.util.logging.Logger;

public class TurboCache <K,V> implements Serializable , Cache<K,V> {
    private final Logger logger = Logger.getLogger(TurboCache.class.getName());

    @Serial
    private static final long serialVersionUID = 1L;

    private final InMemoryCache<K,V> cache;
    private final LRUCache<K,V> lruCache;
    private final SchedulerService<K,V> scheduler;
    private final BackStoreDataLoader<K,V> dataLoader;

    private final long ttl;

    public TurboCache(CacheConfig config) {

        this.ttl = config.getTtl();

        this.cache = new InMemoryCache<>(config.getMaxSize(), config.getExpirationStrategy());
        this.lruCache = new LRUCache<>(config.getMaxSize(), config.getEvictionPolicy());

        this.scheduler = new SchedulerServiceImpl<>(config.getTtl(), config.getRefreshDuration());
        this.dataLoader = new BackStoreDataLoader<>(config.getLoadingMode(), config.getWritePolicy());
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

        //STEP1 : Evict if required
        K evictedKey = lruCache.evictIfRequired(key, value);

        //STEP2: Remove from cache if evicted from LRU cache
        if(evictedKey != null)  cache.remove(evictedKey);

        //STEP2: Update cache
        cache.put(key, value, ttl);

        //STEP3: Update backing store based on write policy
        dataLoader.save(key, value);

        //STEP4: Schedule cleaning expired keys from cache
        scheduler.scheduleCleanup(key, cache);

        //STEP5: Schedule refresh for the key on regular interval
        scheduler.scheduleRefresh(key, cache, dataLoader);
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
        if(entry == null){
            //when there is any cache miss, get it from back store
            V value = dataLoader.load(key);
            if(value != null){
                put(key, value);
            }
            return value;
        }
        if(System.currentTimeMillis() < entry.getExpiryTime()){
            //change the key's last access order
            lruCache.put(key, entry);
            return entry.value;
        }else{
            // Entry is expired, remove it from cache
            cache.remove(key);
        }
        return null;
    }


    @Override
    public void remove(K key) {
        cache.remove(key);
        lruCache.remove(key);
    }

    @Override
    public void clearCache() {
        cache.clearCache();
        lruCache.clearCache();
    }

    @Override
    public int size() {
        return cache.size();
    }




}
