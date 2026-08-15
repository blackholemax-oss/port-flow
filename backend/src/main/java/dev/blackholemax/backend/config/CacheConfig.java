package dev.blackholemax.backend.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scheduling.annotation.EnableAsync;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Configuration
@EnableCaching
@EnableAsync
public class CacheConfig {

    /** 二级缓存统一过期时间（L1 Caffeine 与 L2 Redis 保持一致） */
    private static final Duration TTL = Duration.ofMinutes(10);

    private static final List<String> CACHE_NAMES = List.of("pages");

    /**
     * Caffeine（L1 本地）+ Redis（L2 分布式）两级缓存：
     * 展示页高并发读取时先命中本地缓存，Redis 兜底并支撑多实例共享。
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory, ObjectMapper objectMapper) {
        ObjectMapper redisObjectMapper = objectMapper.copy();
        redisObjectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);

        RedisCacheConfiguration redisConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(TTL)
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new GenericJackson2JsonRedisSerializer(redisObjectMapper)));

        RedisCacheManager redisCacheManager = RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(redisConfig)
                .build();

        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
        caffeineCacheManager.setCacheNames(CACHE_NAMES);
        caffeineCacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(TTL));

        return new CacheManager() {
            @Override
            public Cache getCache(String name) {
                Cache l1 = caffeineCacheManager.getCache(name);
                Cache l2 = redisCacheManager.getCache(name);
                return new TwoLevelCache(name, l1, l2);
            }

            @Override
            public Collection<String> getCacheNames() {
                return Set.copyOf(CACHE_NAMES);
            }
        };
    }
}