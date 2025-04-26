import backingstore.InMemoryBackingStore;
import cache.TurboCache;
import config.CacheConfig;
import config.CacheConfigLoader;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;


public class Main {
    public static void main(String[] args) throws InterruptedException, IOException {
        // Initialize backing store
        InMemoryBackingStore<String, String> backingStore = new InMemoryBackingStore<>();
        // Define the expiry callback (Consumer)
        Consumer<String> expiryCallback = key -> System.out.println("Key expired: " + key);

        // Define the async loader function (Function)
        Function<String, String> asyncLoaderFunction = key -> {
            // Simulate loading data asynchronously
            System.out.println("Loading data for key from backstore in async mode: " + key);
            return backingStore.load(key);
        };

        // Configure cache
        CacheConfig config = CacheConfigLoader.loadConfig("src/config.properties");

        // Initialize cache
        TurboCache<String, String> cache = new TurboCache<>(config, backingStore, expiryCallback, asyncLoaderFunction);

        // Test Case 1: Add and Retrieve
        System.out.println("Test Case 1: Add and Retrieve");
        cache.put("key1", "value1");
        System.out.println("Expected: value1, Actual: " + cache.get("key1"));

        // Test Case 2: Retrieve Non-existent Key
        System.out.println("\nTest Case 2: Retrieve Non-existent Key");
        System.out.println("Expected: null, Actual: " + cache.get("keyX"));

        // Test Case 3: Update Existing Key
        System.out.println("\nTest Case 3: Update Existing Key");
        cache.put("key1", "value2");
        System.out.println("Expected: value2, Actual: " + cache.get("key1"));

        // Test Case 4: Remove Key
        System.out.println("\nTest Case 4: Remove Key");
        cache.remove("key1");
        System.out.println("Expected: null, Actual: " + cache.get("key1"));



        // Test Case 5: Evict on Capacity
        System.out.println("\nTest Case 5: Evict on Capacity");
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        cache.put("key3", "value3"); // This should evict key1
        //Key1 is evicted, still it will be fetched from back store
        System.out.println("Expected: value1, Actual: " + cache.get("key1"));
        System.out.println("Expected: value2, Actual: " + cache.get("key2"));
        System.out.println("Expected: value3, Actual: " + cache.get("key3"));


        //Test case 6: TTL Expiration
        System.out.println("Waiting for 3 seconds to allow TTL to expire...");
        System.out.println(cache.get("key3")); // Should not be null now
        TimeUnit.SECONDS.sleep(3);
        System.out.println("After expiration: Expected: null, Actual: " + cache.get("key3"));

        //Test case 7: Concurrency
        testConcurrency(cache);

        //Test case 8: Refresh Scheduler
        testRefreshScheduler(cache, backingStore, config);
    }

    private static void testRefreshScheduler(TurboCache<String, String> cache,
                                             InMemoryBackingStore<String, String> backingStore,
                                             CacheConfig config) throws InterruptedException {

        // Add an entry to the cache
        cache.put("key1", "initialValue");
        System.out.println("Before refresh: Expected: initialValue, Actual: " + cache.get("key1"));

        // Update the value in the backing store
        backingStore.save("key1", "updatedValue");

        // Wait for the refresh duration to pass
        TimeUnit.MILLISECONDS.sleep(config.getRefreshDuration() + 100);

        // Verify the cache is updated
        String actual = cache.get("key1");
        System.out.println("After refresh: Expected: updatedValue, Actual: " + actual);

    }

    private static void testConcurrency(TurboCache<String, String> cache) throws InterruptedException {
        // Create a thread pool
        ExecutorService executor = Executors.newFixedThreadPool(10);

        // Concurrent creation and updates
        for (int i = 0; i < 50; i++) {
            final int keyIndex = i;
            executor.submit(() -> {
                String initialValue = "value" + keyIndex;
                cache.put("key" + keyIndex, initialValue);
                //System.out.println("Put key" + keyIndex + ": Expected: " + initialValue + ", Actual: " + cache.get("key" + keyIndex));
            });

            executor.submit(() -> {
                String updatedValue = "updatedValue" + keyIndex;
                cache.put("key" + keyIndex, updatedValue);
               // System.out.println("Update key" + keyIndex + ": Expected: " + updatedValue + ", Actual: " + cache.get("key" + keyIndex));
            });
        }

        // Shutdown executor and wait for tasks to complete
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        // Validate final cache state
        for (int i = 0; i < 50; i++) {
            String expected = "updatedValue" + i; // The last update should be the final value
            String actual = cache.get("key" + i);
            System.out.println("Final state of key" + i + ": Expected: " + expected + ", Actual: " + actual);
        }
    }
}