package dev.blackholemax.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.lang.Nullable;

import java.util.concurrent.Callable;

/**
 * Caffeine（本地 L1）+ Redis（分布式 L2）两级缓存：
 * 读操作先查 L1，未命中再查 L2 并回填 L1；写/删操作同步作用于两级。
 * Redis 不可用时自动降级为仅本地缓存（记录 WARN 日志），不阻断业务。
 */
public class TwoLevelCache implements Cache {

    private static final Logger log = LoggerFactory.getLogger(TwoLevelCache.class);

    private final String name;
    private final Cache l1;
    private final Cache l2;

    public TwoLevelCache(String name, Cache l1, Cache l2) {
        this.name = name;
        this.l1 = l1;
        this.l2 = l2;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object getNativeCache() {
        return this;
    }

    @Override
    @Nullable
    public ValueWrapper get(Object key) {
        ValueWrapper wrapper = l1.get(key);
        if (wrapper != null) {
            return wrapper;
        }
        wrapper = getL2(key);
        if (wrapper != null) {
            l1.put(key, wrapper.get());
        }
        return wrapper;
    }

    @Override
    @Nullable
    public <T> T get(Object key, Class<T> type) {
        T value = l1.get(key, type);
        if (value != null) {
            return value;
        }
        value = getL2(key, type);
        if (value != null) {
            l1.put(key, value);
        }
        return value;
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        try {
            ValueWrapper wrapper = get(key);
            if (wrapper != null) {
                return (T) wrapper.get();
            }
            T value = valueLoader.call();
            put(key, value);
            return value;
        } catch (Exception ex) {
            throw new ValueRetrievalException(key, valueLoader, ex);
        }
    }

    @Override
    public void put(Object key, @Nullable Object value) {
        l1.put(key, value);
        putL2(key, value);
    }

    @Override
    public void evict(Object key) {
        l1.evict(key);
        try {
            l2.evict(key);
        } catch (Exception e) {
            log.warn("Redis 缓存 evict 失败，降级为仅本地缓存：{}", e.getMessage());
        }
    }

    @Override
    public void clear() {
        l1.clear();
        try {
            l2.clear();
        } catch (Exception e) {
            log.warn("Redis 缓存 clear 失败，降级为仅本地缓存：{}", e.getMessage());
        }
    }

    @Nullable
    private ValueWrapper getL2(Object key) {
        try {
            return l2.get(key);
        } catch (Exception e) {
            log.warn("Redis 缓存读取失败，降级为仅本地缓存：{}", e.getMessage());
            return null;
        }
    }

    @Nullable
    private <T> T getL2(Object key, Class<T> type) {
        try {
            return l2.get(key, type);
        } catch (Exception e) {
            log.warn("Redis 缓存读取失败，降级为仅本地缓存：{}", e.getMessage());
            return null;
        }
    }

    private void putL2(Object key, @Nullable Object value) {
        if (value == null) {
            return;
        }
        try {
            l2.put(key, value);
        } catch (Exception e) {
            log.warn("Redis 缓存写入失败，降级为仅本地缓存：{}", e.getMessage());
        }
    }
}