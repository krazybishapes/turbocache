package config;

import policy.EvictionPolicy;
import policy.ExpirationStrategy;
import policy.LoadingMode;
import policy.WritePolicy;

public class CacheConfig {
    private int maxSize;
    private long ttl;
    private long refreshDuration;
    private ExpirationStrategy expirationStrategy;
    private WritePolicy writePolicy;
    private LoadingMode loadingMode;
    private EvictionPolicy evictionPolicy;

    // Getters and setters
    public int getMaxSize() { return maxSize; }
    public void setMaxSize(int maxSize) { this.maxSize = maxSize; }
    public long getTtl() { return ttl; }
    public void setTtl(long ttl) { this.ttl = ttl; }
    public long getRefreshDuration() { return refreshDuration; }
    public void setRefreshDuration(long refreshDuration) { this.refreshDuration = refreshDuration; }
    public ExpirationStrategy getExpirationStrategy() { return expirationStrategy; }
    public void setExpirationStrategy(ExpirationStrategy expirationStrategy) { this.expirationStrategy = expirationStrategy; }
    public WritePolicy getWritePolicy() { return writePolicy; }
    public void setWritePolicy(WritePolicy writePolicy) { this.writePolicy = writePolicy; }
    public LoadingMode getLoadingMode() { return loadingMode; }
    public void setLoadingMode(LoadingMode loadingMode) { this.loadingMode = loadingMode; }
    public EvictionPolicy getEvictionPolicy() { return evictionPolicy; }
    public void setEvictionPolicy(EvictionPolicy evictionPolicy) { this.evictionPolicy = evictionPolicy;}
}
