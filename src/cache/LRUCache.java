package cache;

import policy.EvictionPolicy;

import java.util.LinkedHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

public class LRUCache <K,V>{
    private final Logger logger = Logger.getLogger(LRUCache.class.getName());
    /**
     * LinkedHashMap to store the key and value pair for LRU eviction policy.
     */
    private LinkedHashMap<K, V> lruCache = null;

    private final int capacity;
    private final EvictionPolicy evictionPolicy;

    private final Lock lock = new ReentrantLock();

    public LRUCache(int capacity, EvictionPolicy evictionPolicy) {
        this.capacity = capacity;
        this.evictionPolicy = evictionPolicy;
        this.lruCache = new LinkedHashMap<K, V>(capacity, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(java.util.Map.Entry<K, V> eldest) {
                return size() > capacity;
            }
        };
    }

    public K evictIfRequired(K key, V value){
        K evictedKey = null;


            if (!lruCache.containsKey(key) && lruCache.size() >= capacity) {
                try{
                lock.lock();
                evictedKey = lruCache.keySet().iterator().next();
                lruCache.remove(evictedKey);
                } finally {
                    lock.unlock();
                }
                logger.info(evictedKey + " is evicted from LRU cache");
            }
            lruCache.put(key, value);



        return evictedKey;
    }

    public void put(K key, CacheEntry<V> entry) {
        if(entry != null && System.currentTimeMillis() < entry.expiryTime){
            try{
                lock.lock();
                lruCache.put(key, entry.value);
            }finally {
                lock.unlock();
            }
        }
    }

    public void remove(K key){
        try{
            lock.lock();
            lruCache.remove(key);
        }finally {
            lock.unlock();
        }
    }

    public void clearCache(){
        try{
            lock.lock();
            lruCache.clear();
        }finally {
            lock.unlock();
        }
    }

}
