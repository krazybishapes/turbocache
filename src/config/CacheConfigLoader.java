package config;

import policy.EvictionPolicy;
import policy.ExpirationStrategy;
import policy.LoadingMode;
import policy.WritePolicy;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class CacheConfigLoader {

    public static CacheConfig loadConfig(String filePath) throws IOException {
        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream(filePath)) {
            properties.load(input);
        }

        CacheConfig config = new CacheConfig();
        config.setMaxSize(Integer.parseInt(properties.getProperty("cache.maxSize")));
        config.setTtl(Long.parseLong(properties.getProperty("cache.ttl")));
        config.setRefreshDuration(Long.parseLong(properties.getProperty("cache.refreshDuration")));
        config.setExpirationStrategy(ExpirationStrategy.valueOf(properties.getProperty("cache.expirationStrategy")));
        config.setWritePolicy(WritePolicy.valueOf(properties.getProperty("cache.writePolicy")));
        config.setLoadingMode(LoadingMode.valueOf(properties.getProperty("cache.loadingMode")));
        config.setEvictionPolicy(EvictionPolicy.valueOf(properties.getProperty("cache.evictionPolicy")));

        return config;
    }
}
