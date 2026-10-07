package com.scaffold.common.redis.lock;

import java.util.Collections;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 分布式锁句柄：实现 AutoCloseable，配合 try-with-resources 自动释放，
 * AtomicBoolean 防止重复解锁；解锁前先取消自动续约任务。
 *
 * @author ct
 */
public class LockHandle implements AutoCloseable
{
    private static final Logger log = LoggerFactory.getLogger(LockHandle.class);

    private final String key;

    private final String value;

    private final StringRedisTemplate redisTemplate;

    private final ScheduledFuture<?> renewal;

    private final AtomicBoolean released = new AtomicBoolean(false);

    LockHandle(String key, String value, StringRedisTemplate redisTemplate, ScheduledFuture<?> renewal)
    {
        this.key = key;
        this.value = value;
        this.redisTemplate = redisTemplate;
        this.renewal = renewal;
    }

    public String getKey()
    {
        return key;
    }

    /**
     * 释放锁：Lua 校验持有者标识后才删除，防止误删已易主的锁。
     *
     * @return true=本持有者删除成功；false=锁已过期易主（无需处理）
     */
    public boolean unlock()
    {
        if (!released.compareAndSet(false, true))
        {
            return false;
        }
        if (renewal != null)
        {
            renewal.cancel(false);
        }
        Long r = redisTemplate.execute(DistributedLock.UNLOCK_SCRIPT, Collections.singletonList(key), value);
        boolean unlocked = r != null && r == 1;
        if (!unlocked)
        {
            log.debug("[分布式锁] 解锁时锁已失效（过期易主，业务需自检并发安全）: {}", key);
        }
        return unlocked;
    }

    @Override
    public void close()
    {
        unlock();
    }
}
