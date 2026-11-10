package com.smartwater.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Configuration;

/** A Redis outage must never turn into API errors: log it and fall through to the database. */
@Configuration
@Slf4j
public class CacheErrorConfig implements CachingConfigurer {
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override public void handleCacheGetError(RuntimeException e, Cache c, Object key)             { warn("get", c, e); }
            @Override public void handleCachePutError(RuntimeException e, Cache c, Object key, Object v)   { warn("put", c, e); }
            @Override public void handleCacheEvictError(RuntimeException e, Cache c, Object key)           { warn("evict", c, e); }
            @Override public void handleCacheClearError(RuntimeException e, Cache c)                       { warn("clear", c, e); }
            private void warn(String op, Cache c, RuntimeException e) {
                log.warn("Cache {} failed on '{}': {}", op, c == null ? "?" : c.getName(), e.getMessage());
            }
        };
    }
}
