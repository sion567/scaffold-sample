package com.scaffold.auth.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * RedisNonceCache 防重放缓存测试。
 *
 * <p>覆盖维度：
 * <ul>
 *   <li>tryRegister 成功：Redis setIfAbsent 返回 true</li>
 *   <li>tryRegister 失败：Redis setIfAbsent 返回 false（nonce 已存在）</li>
 *   <li>tryRegister 异常：fail-open 返回 true（Redis 抖动时放行）</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RedisNonceCacheTest
{
    @Mock
    private RedisTemplate redisTemplate;

    @Mock
    private ValueOperations valueOps;

    private RedisNonceCache newCache()
    {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        return new RedisNonceCache(redisTemplate, Duration.ofMinutes(5));
    }

    @Test
    @DisplayName("nonce 未注册：setIfAbsent 返回 true → tryRegister 返回 true")
    void tryRegister_firstTime_returnsTrue()
    {
        RedisNonceCache cache = newCache();
        when(valueOps.setIfAbsent(eq("sm:nonce:envelope:n1"), eq("1"), eq(300000L), any()))
                .thenReturn(true);

        boolean result = cache.tryRegister("envelope", "n1");

        assertTrue(result);
    }

    @Test
    @DisplayName("nonce 已存在：setIfAbsent 返回 false → tryRegister 返回 false")
    void tryRegister_duplicate_returnsFalse()
    {
        RedisNonceCache cache = newCache();
        when(valueOps.setIfAbsent(any(), any(), any(Long.class), any())).thenReturn(false);

        boolean result = cache.tryRegister("envelope", "duplicate-nonce");

        assertFalse(result);
    }

    @Test
    @DisplayName("Redis 异常：fail-open 返回 true，记录错误不阻断认证")
    void tryRegister_redisException_failOpen()
    {
        RedisNonceCache cache = newCache();
        when(valueOps.setIfAbsent(any(), any(), any(Long.class), any()))
                .thenThrow(new RuntimeException("Redis connection refused"));

        boolean result = cache.tryRegister("envelope", "any-nonce");

        assertTrue(result);
    }
}
