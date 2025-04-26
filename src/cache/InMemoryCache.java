package cache;

import policy.EvictionPolicy;
import policy.ExpirationStrategy;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryCache<K,V> {

    /**
     * cache.Cache to store Key & Value pair,CacheEntry class to hold the value and its expiry time.
     * This is used to store the data in memory and fetching data in O(1) time complexity.
     * Concurrent Hashmap is used for thread safety.
     */
    private final ConcurrentHashMap<K, CacheEntry<V>> cache;
    private ExpirationStrategy expirationStrategy = ExpirationStrategy.TTL;

    private final int capacity;

    public InMemoryCache(int capacity, ExpirationStrategy expirationStrategy) {
        this.capacity = capacity;
        this.expirationStrategy = expirationStrategy;
        this.cache = new ConcurrentHashMap<>(capacity);
    }

    public void put(K key, V value) {
        cache.put(key, new CacheEntry<>(value, System.currentTimeMillis()));
    }

    public void put(K key, V value, long ttl) {
        if(this.expirationStrategy.equals(ExpirationStrategy.TTL)){
            cache.put(key, new CacheEntry<>(value, System.currentTimeMillis() + ttl));
        }else if(this.expirationStrategy.equals(ExpirationStrategy.FIXED)){
            cache.put(key, new CacheEntry<>(value,  ttl));
        }

    }

    public CacheEntry<V> get(K key) {
        return cache.get(key);
    }

    public void remove(K key) {
        cache.remove(key);
    }

    public void clearCache() {
        cache.clear();
    }

    public int size() {
        return cache.size();
    }
}
