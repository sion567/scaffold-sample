package com.scaffold.common.redis.lock;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.context.annotation.Bean;

/**
 * 分布式锁自动装配（starter 化）：scaffold.lock.enabled=false 可整体关闭。
 *
 * @author ct
 */
@AutoConfiguration
@AutoConfigureAfter(RedisAutoConfiguration.class)
@ConditionalOnProperty(prefix = "scaffold.lock", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnSingleCandidate(StringRedisTemplate.class)
public class LockAutoConfiguration
{
    @Bean
    public LockLeaseManager lockLeaseManager(StringRedisTemplate redisTemplate)
    {
        return new LockLeaseManager(redisTemplate);
    }

    @Bean
    public DistributedLock distributedLock(StringRedisTemplate redisTemplate, LockLeaseManager leaseManager)
    {
        return new DistributedLock(redisTemplate, leaseManager);
    }
}
