package com.scaffold.common.redis.cache;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.support.SimpleValueWrapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.scaffold.common.redis.service.RedisService;

/**
 * 两级缓存（P2-2，借鉴 MateCloud CompositeCache）：Caffeine L1（纳秒级、TTL 短）
 * + Redis L2（分布式、TTL 长）。写/删后经 Redis pub/sub 广播失效其他实例的 L1。
 * 不缓存 null（null 穿透由业务侧用哨兵值解决）。
 *
 * @author ct
 */
public class TwoLevelCache implements Cache
{
    private static final Logger log = LoggerFactory.getLogger(TwoLevelCache.class);

    private final String name;

    private final com.github.benmanes.caffeine.cache.Cache<Object, Object> l1;

    private final RedisService redisService;

    private final StringRedisTemplate stringRedisTemplate;

    private final String l2Prefix;

    private final long l2TtlSeconds;

    TwoLevelCache(String name, RedisService redisService, StringRedisTemplate stringRedisTemplate,
                  TwoLevelCacheProperties properties)
    {
        this.name = name;
        this.redisService = redisService;
        this.stringRedisTemplate = stringRedisTemplate;
        this.l2TtlSeconds = properties.getL2TtlSeconds();
        this.l2Prefix = "cache:" + name + ":";
        this.l1 = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(properties.getL1TtlSeconds()))
                .maximumSize(properties.getL1MaxSize())
                .build();
    }

    @Override
    public ValueWrapper get(Object key)
    {
        Object value = l1.getIfPresent(key);
        if (value == null)
        {
            value = redisService.getCacheObject(l2Key(key));
            if (value != null)
            {
                l1.put(key, value);
            }
        }
        return value == null ? null : new SimpleValueWrapper(value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(Object key, Class<T> type)
    {
        ValueWrapper wrapper = get(key);
        Object value = wrapper == null ? null : wrapper.get();
        if (value != null && !type.isInstance(value))
        {
            throw new IllegalStateException("两级缓存类型不匹配: cache=" + name + ", key=" + key
                    + ", expected=" + type.getName());
        }
        return (T) value;
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader)
    {
        ValueWrapper wrapper = get(key);
        if (wrapper != null)
        {
            @SuppressWarnings("unchecked")
            T cached = (T) wrapper.get();
            return cached;
        }
        T loaded;
        try
        {
            loaded = valueLoader.call();
        }
        catch (Exception e)
        {
            throw new ValueRetrievalException(key, valueLoader, e);
        }
        if (loaded != null)
        {
            put(key, loaded);
        }
        return loaded;
    }

    @Override
    public void put(Object key, Object value)
    {
        if (value == null)
        {
            evict(key);
            return;
        }
        l1.put(key, value);
        redisService.setCacheObject(l2Key(key), value, l2TtlSeconds, TimeUnit.SECONDS);
        broadcast(String.valueOf(key));
    }

    @Override
    public void evict(Object key)
    {
        invalidateLocal(key);
        redisService.deleteObject(l2Key(key));
        broadcast(String.valueOf(key));
    }

    @Override
    public void clear()
    {
        l1.invalidateAll();
        Collection<String> keys = redisService.keys(l2Prefix + "*");
        if (keys != null && !keys.isEmpty())
        {
            redisService.deleteObject(keys);
        }
        broadcast("*");
    }

    /**
     * 仅失效本机 L1（失效广播回调入口）；key="*" 表示整库失效
     */
    void invalidateLocal(Object key)
    {
        if ("*".equals(key))
        {
            l1.invalidateAll();
        }
        else
        {
            l1.invalidate(key);
        }
    }

    private void broadcast(String key)
    {
        try
        {
            stringRedisTemplate.convertAndSend(TwoLevelCacheManager.INVALIDATE_CHANNEL, name + "|" + key);
        }
        catch (Exception e)
        {
            log.warn("[两级缓存] 失效广播失败（本机已失效，其他实例 L1 依赖 TTL 兜底）: {}, {}",
                    name, e.getMessage());
        }
    }

    private String l2Key(Object key)
    {
        return l2Prefix + key;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public Object getNativeCache()
    {
        return l1;
    }
}
