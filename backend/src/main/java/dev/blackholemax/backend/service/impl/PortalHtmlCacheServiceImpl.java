package dev.blackholemax.backend.service.impl;

import dev.blackholemax.backend.service.PortalHtmlCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 门户 HTML 的 Redis 缓存服务实现。
 */
@Service
public class PortalHtmlCacheServiceImpl implements PortalHtmlCacheService {

    private static final Logger log = LoggerFactory.getLogger(PortalHtmlCacheServiceImpl.class);
    private static final String KEY_PREFIX = "portal:html:";
    private static final Duration TTL = Duration.ofMinutes(30);

    private final StringRedisTemplate redisTemplate;

    public PortalHtmlCacheServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void cache(long userId, String slug, String html) {
        try {
            redisTemplate.opsForValue().set(key(userId, slug), html, TTL);
            log.info("门户 HTML 已缓存到 Redis：{}", key(userId, slug));
        } catch (Exception e) {
            log.warn("Redis 缓存写入失败，跳过缓存：{}", e.getMessage());
        }
    }

    @Override
    public String getCached(long userId, String slug) {
        try {
            return redisTemplate.opsForValue().get(key(userId, slug));
        } catch (Exception e) {
            log.warn("Redis 缓存读取失败：{}", e.getMessage());
            return null;
        }
    }

    @Override
    public void evict(long userId, String slug) {
        try {
            redisTemplate.delete(key(userId, slug));
        } catch (Exception e) {
            log.warn("Redis 缓存清除失败：{}", e.getMessage());
        }
    }

    private String key(long userId, String slug) {
        return KEY_PREFIX + userId + ":" + (slug != null ? slug : "draft");
    }
}
