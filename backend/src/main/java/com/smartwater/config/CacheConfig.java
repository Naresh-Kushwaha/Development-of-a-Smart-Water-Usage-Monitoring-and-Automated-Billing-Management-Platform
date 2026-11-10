package com.smartwater.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartwater.web.Dtos.CompareRes;
import com.smartwater.web.Dtos.DashboardRes;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    /** Redis cache with readable JSON values and a short TTL per cache. */
    @Bean
    @ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true")
    CacheManager redisCacheManager(RedisConnectionFactory factory, ObjectMapper mapper) {
        RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
                .prefixCacheNameWith("smartwater::")
                .disableCachingNullValues();
        return RedisCacheManager.builder(factory)
                .cacheDefaults(base.entryTtl(Duration.ofMinutes(1)))
                .withCacheConfiguration("dashboard", typed(base, mapper, DashboardRes.class, Duration.ofSeconds(60)))
                .withCacheConfiguration("compare", typed(base, mapper, CompareRes.class, Duration.ofMinutes(5)))
                .build();
    }

    /** Redis switched off: @Cacheable simply calls the method every time. */
    @Bean
    @ConditionalOnProperty(name = "app.redis.enabled", havingValue = "false", matchIfMissing = true)
    CacheManager noCacheManager() {
        return new NoOpCacheManager();
    }

    private static <T> RedisCacheConfiguration typed(RedisCacheConfiguration base, ObjectMapper mapper,
                                                     Class<T> type, Duration ttl) {
        return base.entryTtl(ttl)
                .serializeValuesWith(SerializationPair.fromSerializer(new Jackson2JsonRedisSerializer<>(mapper, type)));
    }
}
