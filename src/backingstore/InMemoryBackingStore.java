package backingstore;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryBackingStore<K, V> implements BackingStore<K, V> {
    private final Map<K, V> store = new ConcurrentHashMap<>();

    @Override
    public V load(K key) {
        return store.get(key);
    }

    @Override
    public void save(K key, V value) {
        store.put(key, value);
    }

    @Override
    public void remove(K key) {
        store.remove(key);
    }

    @Override
    public Map<K, V> getAllEntries() {
        return new ConcurrentHashMap<>(store); // Return a copy to avoid external modification
    }
}
