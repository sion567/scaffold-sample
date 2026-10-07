package com.scaffold.common.redis.lock;

import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

/**
 * 锁自动续约管理器：长任务持有锁期间，每 TTL/2 周期用 Lua 校验持有者后续期，
 * 防止业务未完成锁先过期被他人抢占。解锁时由 LockHandle 取消续约任务。
 *
 * @author ct
 */
public class LockLeaseManager
{
    private static final Logger log = LoggerFactory.getLogger(LockLeaseManager.class);

    private static final DefaultRedisScript<Long> RENEW_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('pexpire', KEYS[1], ARGV[2]) else return 0 end",
            Long.class);

    private final StringRedisTemplate redisTemplate;

    private final ScheduledExecutorService scheduler;

    public LockLeaseManager(StringRedisTemplate redisTemplate)
    {
        this.redisTemplate = redisTemplate;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "scaffold-lock-lease");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * 启动续约：每 TTL/2 续期一次；leaseMillis <= 0 表示不自动续约（返回 null）
     */
    public ScheduledFuture<?> startRenewal(String key, String value, long leaseMillis)
    {
        if (leaseMillis <= 0)
        {
            return null;
        }
        long period = Math.max(leaseMillis / 2, 1000L);
        return scheduler.scheduleWithFixedDelay(() -> {
            try
            {
                Long r = redisTemplate.execute(RENEW_SCRIPT, Collections.singletonList(key),
                        value, String.valueOf(leaseMillis));
                if (r == null || r == 0)
                {
                    log.warn("[分布式锁] 续约未生效（锁已过期或易主，注意业务时长与 leaseTime 是否匹配）: {}", key);
                }
            }
            catch (Exception e)
            {
                log.warn("[分布式锁] 续约异常: {}, {}", key, e.getMessage());
            }
        }, period, period, TimeUnit.MILLISECONDS);
    }

    @jakarta.annotation.PreDestroy
    public void shutdown()
    {
        scheduler.shutdownNow();
    }
}
