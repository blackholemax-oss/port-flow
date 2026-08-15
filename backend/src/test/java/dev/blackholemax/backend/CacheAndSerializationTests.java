package dev.blackholemax.backend;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.blackholemax.backend.config.TwoLevelCache;
import dev.blackholemax.backend.dto.PortfolioPageData;
import org.junit.jupiter.api.Test;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 覆盖二级缓存的两个高风险点：
 * 1. PortfolioPageData 经 Redis JSON 序列化（含 @class 类型信息）可无损还原；
 * 2. TwoLevelCache 的 L2 回填 L1 与双级 evict 语义正确。
 */
class CacheAndSerializationTests {

    private ObjectMapper redisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);
        return mapper;
    }

    @Test
    void redisJsonRoundTripPreservesPageData() {
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(redisObjectMapper());

        PortfolioPageData data = new PortfolioPageData(
                "demo", "张三", "让技术成为我的名片", "热爱技术的独立开发者",
                "Java, Spring Boot, AI", "#4F46E5", "card",
                "张三 - 个人作品集 | AI 策展师", "热爱技术的独立开发者，展示作品。",
                "/demo-avatar.svg",
                null,
                List.of(
                        new PortfolioPageData.ProjectData("作品集生成器", "输入资料即可生成作品集页面。", "/demo-cover.svg"),
                        new PortfolioPageData.ProjectData("极简时钟", "Canvas 手写。", "/demo-cover.svg")
                )
        );

        byte[] bytes = serializer.serialize(data);
        PortfolioPageData back = (PortfolioPageData) serializer.deserialize(bytes);

        assertNotNull(back);
        assertEquals("demo", back.slug());
        assertEquals("张三", back.userName());
        assertEquals("card", back.template());
        assertEquals(2, back.projects().size());
        assertEquals("极简时钟", back.projects().get(1).title());
        assertEquals("/demo-cover.svg", back.projects().get(0).coverPath());
    }

    @Test
    void twoLevelCacheBackfillsFromL2AndEvictsBothLevels() {
        CaffeineCacheManager l1Manager = new CaffeineCacheManager("pages");
        l1Manager.setCaffeine(Caffeine.newBuilder().maximumSize(100).expireAfterWrite(Duration.ofMinutes(10)));
        CaffeineCacheManager l2Manager = new CaffeineCacheManager("pages");
        l2Manager.setCaffeine(Caffeine.newBuilder().maximumSize(100).expireAfterWrite(Duration.ofMinutes(10)));

        TwoLevelCache cache = new TwoLevelCache("pages", l1Manager.getCache("pages"), l2Manager.getCache("pages"));

        cache.put("demo", "v1");
        assertEquals("v1", cache.get("demo").get());

        // 模拟 L1 过期：直接从 L1 清除，验证 L2 回填
        l1Manager.getCache("pages").evict("demo");
        assertNull(l1Manager.getCache("pages").get("demo"));
        assertEquals("v1", cache.get("demo").get());
        assertNotNull(l1Manager.getCache("pages").get("demo"), "L2 命中后应回填 L1");

        // evict 应同时清除两级
        cache.evict("demo");
        assertNull(l1Manager.getCache("pages").get("demo"));
        assertNull(l2Manager.getCache("pages").get("demo"));
    }
}