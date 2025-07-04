package cache;

public interface Cache<K,V> {

    void put(K key, V value);
    V get(K key);
    void remove(K key);
    public void clearCache();
    int size();
}
