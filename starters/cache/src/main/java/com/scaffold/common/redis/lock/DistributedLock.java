package com.scaffold.common.redis.lock;

import java.net.InetAddress;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

/**
 * 分布式锁（SETNX + Lua 校验持有者 + 自动续约，纯 Redis 实现不依赖 Redisson）。
 * <p>
 * 推荐用法（自动释放，获取失败抛 LockAcquisitionException）：
 * <pre>
 * String result = distributedLock.executeWithLock("order:create:" + userId,
 *         3000, 10000, () -> doCreate());
 * </pre>
 * 手动用法（必须 finally 释放或用 try-with-resources）：
 * <pre>
 * try (LockHandle handle = distributedLock.acquire(key, 3000, 10000)) { ... }
 * </pre>
 * <p>
 * 设计要点：
 * - 加锁 SET NX PX，锁值 = 主机名+UUID 持有者标识；
 * - 解锁/续约走 Lua：get == 持有者标识 才 del/pexpire，防止误删他人锁；
 * - 等待重试带随机抖动，防止多实例同时重试的惊群；
 * - leaseMillis &gt; 0 时由 LockLeaseManager 每 TTL/2 自动续约，长任务不丢锁。
 *
 * @author ct
 */
public class DistributedLock
{
    static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DistributedLock.class);

    private final StringRedisTemplate redisTemplate;

    private final LockLeaseManager leaseManager;

    private final String ownerIdPrefix;

    public DistributedLock(StringRedisTemplate redisTemplate, LockLeaseManager leaseManager)
    {
        this.redisTemplate = redisTemplate;
        this.leaseManager = leaseManager;
        this.ownerIdPrefix = resolveHostName() + "-";
    }

    /**
     * 默认参数执行：等待 3s，租期 10s（自动续约）
     */
    public <T> T executeWithLock(String key, Supplier<T> action)
    {
        return executeWithLock(key, 3000L, 10000L, action);
    }

    /**
     * 持锁执行：获取失败抛 {@link LockAcquisitionException}；业务异常时锁照常释放后异常继续上抛
     */
    public <T> T executeWithLock(String key, long waitMillis, long leaseMillis, Supplier<T> action)
    {
        LockHandle handle = acquire(key, waitMillis, leaseMillis);
        if (handle == null)
        {
            throw new LockAcquisitionException("获取分布式锁超时: " + key);
        }
        try (handle)
        {
            return action.get();
        }
    }

    public void executeWithLock(String key, long waitMillis, long leaseMillis, Runnable action)
    {
        executeWithLock(key, waitMillis, leaseMillis, () -> {
            action.run();
            return null;
        });
    }

    /**
     * 抢锁：成功返回句柄（已启动自动续约）；等待超时返回 null（由调用方决定提示或降级）
     */
    public LockHandle acquire(String key, long waitMillis, long leaseMillis)
    {
        String value = ownerIdPrefix + UUID.randomUUID();
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(waitMillis);
        while (System.nanoTime() < deadline)
        {
            Boolean ok = redisTemplate.opsForValue()
                    .setIfAbsent(key, value, leaseMillis, TimeUnit.MILLISECONDS);
            if (Boolean.TRUE.equals(ok))
            {
                return new LockHandle(key, value, redisTemplate, leaseManager.startRenewal(key, value, leaseMillis));
            }
            // 随机抖动 20~100ms，避免多实例同时重试的惊群
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(20 + ThreadLocalRandom.current().nextInt(80)));
        }
        return null;
    }

    private static String resolveHostName()
    {
        try
        {
            return InetAddress.getLocalHost().getHostName();
        }
        catch (Exception e)
        {
            log.warn("[分布式锁] 获取主机名失败，使用 unknown: {}", e.getMessage());
            return "unknown";
        }
    }
}
