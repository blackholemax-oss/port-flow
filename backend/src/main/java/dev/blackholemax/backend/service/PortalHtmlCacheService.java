package dev.blackholemax.backend.service;

/**
 * 门户 HTML 的 Redis 缓存服务：
 * 大模型生成的 HTML 先缓存到 Redis（key = portal:html:{userId}:{slug}），
 * 便于用户在保存前反复预览/编辑，避免重复调用 LLM。Redis 不可用时自动降级。
 */
public interface PortalHtmlCacheService {

    /** 缓存生成的 HTML，TTL 30 分钟 */
    void cache(long userId, String slug, String html);

    /** 读取缓存的 HTML，Redis 不可用时返回 null */
    String getCached(long userId, String slug);

    /** 清除缓存（保存后调用，避免脏数据） */
    void evict(long userId, String slug);
}
