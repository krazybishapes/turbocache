package cache;

import java.io.Serial;
import java.io.Serializable;

public class CacheEntry<V> implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    final V value;
    final long expiryTime;

    CacheEntry(V value, long expiryTime) {
        this.value = value;
        this.expiryTime = expiryTime;
    }

    public V getValue() {
        return this.value;
    }

    public long getExpiryTime() {
        return this.expiryTime;
    }
}
