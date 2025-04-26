package backingstore;

import policy.WritePolicy;

import java.util.Map;

public interface BackingStore<K, V> {
    V load(K key);
    void save(K key, V value);
    void remove(K key);
    void save(K key, V value, WritePolicy writePolicy);

     Map<K, V> getAllEntries();
}
