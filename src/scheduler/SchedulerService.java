package scheduler;

import cache.InMemoryCache;
import loader.BackStoreDataLoader;

import java.util.function.Function;

public interface SchedulerService<K,V> {

    void scheduleCleanup(K key, InMemoryCache<K,V> cache);
    void scheduleRefresh(K key, InMemoryCache<K,V> cache, BackStoreDataLoader<K,V> dataLoader);
}
