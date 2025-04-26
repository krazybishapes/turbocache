
# TurboCache

TurboCache is a high-performance, thread-safe in-memory caching library for Java applications. It supports various caching features such as time-to-live (TTL), refresh scheduling, eviction policies, and asynchronous data loading from a backing store.

## Features

- **Thread-Safe**: Designed to handle concurrent access and updates.
- **Time-to-Live (TTL)**: Automatically removes expired entries from the cache.
- **Refresh Scheduler**: Periodically refreshes cache entries by fetching updated data from the backing store.
- **Eviction Policies**: Supports Least Recently Used (LRU) eviction to manage cache size.
- **Asynchronous Loading**: Fetches data from the backing store asynchronously for cache misses.
- **Write-Through**: Automatically writes data to the backing store when added to the cache.

## Installation
Unzip the `turbo-cache.zip` file and can be run as java application

## Config Properties
Available under src/config.properties
```properties
maxSize=100
ttl=60000
refreshDuration=30000
evictionPolicy=LRU
loadingMode=ASYNC
writePolicy=WRITE_THROUGH
expirationStrategy=TTL
```


##Execution
```bash
Run Main.java class as java application
```

