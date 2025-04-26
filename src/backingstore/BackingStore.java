package backingstore;

import java.util.Map;

public interface BackingStore<K, V> {
    V load(K key);
    void save(K key, V value);
    void remove(K key);

     Map<K, V> getAllEntries();
}
