package backingstore;

import policy.WritePolicy;

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


    @Override
    public void save(K key, V value, WritePolicy writePolicy) {
        //Only write through is implemented for now
        if (writePolicy.equals(WritePolicy.WRITE_THROUGH)) {
            save(key, value);
        }
    }




}
