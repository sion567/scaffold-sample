package com.scaffold.auth.security;

import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的 nonce 防重放缓存（SETNX + TTL，原子操作）。
 * <p>
 * Redis 异常时放行（fail-open）并记录错误：宁可短暂容忍重放窗口，
 * 也不因缓存抖动阻断全部认证请求；上线后应配合告警监控该日志。
 */
public class RedisNonceCache implements NonceCache {

    private static final String KEY_PREFIX = "sm:nonce:";

    private final RedisTemplate redisTemplate;
    private final Duration window;

    public RedisNonceCache(RedisTemplate redisTemplate, Duration window) {
        this.redisTemplate = redisTemplate;
        this.window = window;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean tryRegister(String keyId, String nonce) {
        try {
            Boolean ok = redisTemplate.opsForValue()
                    .setIfAbsent(KEY_PREFIX + keyId + ":" + nonce, "1", window.toMillis(), TimeUnit.MILLISECONDS);
            return Boolean.TRUE.equals(ok);
        } catch (Exception e) {
            NonceCache.log.error("NonceCache redis 异常，fail-open: {}", e.getMessage());
            return true;
        }
    }
}
