package loader;

import backingstore.InMemoryBackingStore;
import policy.LoadingMode;
import policy.WritePolicy;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

public class BackStoreDataLoader<K,V> {

    private final LoadingMode loadingMode;
    private final WritePolicy writePolicy;

    InMemoryBackingStore<K, V> backingStore = new InMemoryBackingStore<>();
    private final transient ExecutorService asyncLoader = Executors.newSingleThreadExecutor();

    Function<K, V> asyncLoaderFunction = key -> {
        System.out.println("Loading data for key from back store in async mode: " + key);
        return backingStore.load(key);
    };

    public BackStoreDataLoader(LoadingMode loadingMode, WritePolicy writePolicy) {
        this.loadingMode = loadingMode;
        this.writePolicy = writePolicy;
    }

    public V load(K key) {
        if (loadingMode == LoadingMode.ASYNC) {
            return loadAsync(key);
        } else if (loadingMode == LoadingMode.SYNC) {
            return loadSync(key);
        }
        return null;
    }

    private V loadAsync(K key){
        if(loadingMode == LoadingMode.ASYNC) {
            CompletableFuture<V> future = CompletableFuture.supplyAsync(() -> asyncLoaderFunction.apply(key), asyncLoader);
            try {
                return future.get();
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private V loadSync(K key){
        if(loadingMode == LoadingMode.SYNC) {
            return backingStore.load(key);
        }
        return null;
    }

    public void save(K key, V value) {
        if(this.writePolicy.equals(WritePolicy.WRITE_THROUGH)){
            backingStore.save(key, value);
        } else if(this.writePolicy.equals(WritePolicy.WRITE_BACK)){
            CompletableFuture.runAsync(() -> {
                backingStore.save(key, value);
            }, asyncLoader);
        } else {
            throw new IllegalArgumentException("Invalid write policy");
        }

    }
}
