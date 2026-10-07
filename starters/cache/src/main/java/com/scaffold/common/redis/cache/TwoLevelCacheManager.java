package com.scaffold.common.redis.cache;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.scaffold.common.redis.service.RedisService;

/**
 * 两级缓存管理器（P2-2）：每个 cacheName 一个 TwoLevelCache（L1 Caffeine + L2 Redis）。
 * 作为容器唯一 CacheManager，@Cacheable/@CacheEvict 注解直接生效。
 *
 * @author ct
 */
public class TwoLevelCacheManager implements CacheManager
{
    /** L1 失效广播频道（cacheName|key，key 为 * 表示整库失效） */
    public static final String INVALIDATE_CHANNEL = "scaffold:cache:invalidate";

    private final RedisService redisService;

    private final StringRedisTemplate stringRedisTemplate;

    private final TwoLevelCacheProperties properties;

    private final Map<String, TwoLevelCache> caches = new ConcurrentHashMap<>();

    public TwoLevelCacheManager(RedisService redisService, StringRedisTemplate stringRedisTemplate,
                                TwoLevelCacheProperties properties)
    {
        this.redisService = redisService;
        this.stringRedisTemplate = stringRedisTemplate;
        this.properties = properties;
    }

    @Override
    public Cache getCache(String name)
    {
        return caches.computeIfAbsent(name,
                n -> new TwoLevelCache(n, redisService, stringRedisTemplate, properties));
    }

    /**
     * 失效广播回调：只失效本机 L1，L2 由发起方维护
     */
    public void invalidateLocal(String name, String key)
    {
        TwoLevelCache cache = caches.get(name);
        if (cache != null)
        {
            cache.invalidateLocal(key);
        }
    }

    @Override
    public Collection<String> getCacheNames()
    {
        return Collections.unmodifiableSet(caches.keySet());
    }
}
