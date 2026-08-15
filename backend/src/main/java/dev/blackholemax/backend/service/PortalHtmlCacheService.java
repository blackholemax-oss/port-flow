package dev.blackholemax.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 门户 HTML 的 Redis 缓存：
 * 大模型生成的 HTML 会先缓存到 Redis（key = portal:html:{userId}:{slug}），
 * 便于用户在保存前反复预览/编辑，避免重复调用 LLM。
 *
 * Redis 不可用时自动降级（仅记日志），不阻断生成流程。
 */
@Service
public class PortalHtmlCacheService {

    private static final Logger log = LoggerFactory.getLogger(PortalHtmlCacheService.class);
    private static final String KEY_PREFIX = "portal:html:";
    private static final Duration TTL = Duration.ofMinutes(30);

    private final StringRedisTemplate redisTemplate;

    public PortalHtmlCacheService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /** 缓存生成的 HTML，TTL 30 分钟 */
    public void cache(long userId, String slug, String html) {
        try {
            redisTemplate.opsForValue().set(key(userId, slug), html, TTL);
            log.info("门户 HTML 已缓存到 Redis：{}", key(userId, slug));
        } catch (Exception e) {
            log.warn("Redis 缓存写入失败，跳过缓存：{}", e.getMessage());
        }
    }

    /** 读取缓存的 HTML，Redis 不可用时返回 null */
    public String getCached(long userId, String slug) {
        try {
            return redisTemplate.opsForValue().get(key(userId, slug));
        } catch (Exception e) {
            log.warn("Redis 缓存读取失败：{}", e.getMessage());
            return null;
        }
    }

    /** 清除缓存（保存后调用，避免脏数据） */
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
