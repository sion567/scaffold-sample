package com.scaffold.auth.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SessionSignKeyStore 会话签名密钥存取测试。
 *
 * <p>使用真实 Sm2Engine 密钥对生成（无 mockStatic 支持），
 * 只验证 Redis 操作的正确性和返回格式。
 * 覆盖维度：
 * <ul>
 *   <li>issue：生成密钥对，公钥存 Redis，私钥 64 位 hex 返回</li>
 *   <li>publicKeyOf：存在时返回公钥，不存在返回 null</li>
 *   <li>revoke：删除 Redis key</li>
 *   <li>fixed32：BigInteger 归一化（通过 issue 隐式覆盖）</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SessionSignKeyStoreTest
{
    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations valueOps;

    private SessionSignKeyStore keyStore;

    private void newStore()
    {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        keyStore = new SessionSignKeyStore(redisTemplate, Duration.ofMinutes(30));
    }

    // ─── issue ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("issue 签发密钥对")
    class Issue
    {
        @Test
        @DisplayName("签发：公钥写入 Redis，返回 64 位私钥 hex")
        void issue_storesPubKeyAndReturnsPrivHex()
        {
            newStore();

            // 使用真实 Sm2Engine 密钥对，避免 mockStatic
            String privHex = keyStore.issue("tok-123");

            assertNotNull(privHex);
            assertEquals(64, privHex.length()); // 32 字节 = 64 hex chars
            assertTrue(privHex.matches("[0-9a-fA-F]+"));
            verify(valueOps).set(eq("gm:sign:pub:tok-123"), anyString(), eq(Duration.ofMinutes(30)));
        }

        @Test
        @DisplayName("签发 2 次：覆盖 Redis key（幂等）")
        void issue_sameToken_overwrites()
        {
            newStore();

            keyStore.issue("tok-dup");
            keyStore.issue("tok-dup");

            verify(valueOps, org.mockito.Mockito.atLeast(2))
                    .set(eq("gm:sign:pub:tok-dup"), anyString(), eq(Duration.ofMinutes(30)));
        }
    }

    // ─── publicKeyOf ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("publicKeyOf 公钥查询")
    class PublicKeyOf
    {
        @Test
        @DisplayName("公钥存在：返回 hex 字符串")
        void exists()
        {
            newStore();
            when(valueOps.get("gm:sign:pub:tok-abc")).thenReturn("04ABCD1234");

            String result = keyStore.publicKeyOf("tok-abc");

            assertNotNull(result);
            assertEquals("04ABCD1234", result);
        }

        @Test
        @DisplayName("公钥不存在：返回 null")
        void notFound()
        {
            newStore();
            when(valueOps.get(any())).thenReturn(null);

            String result = keyStore.publicKeyOf("tok-unknown");

            assertNull(result);
        }
    }

    // ─── revoke ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("revoke 撤销密钥")
    class Revoke
    {
        @Test
        @DisplayName("撤销：删除 Redis key")
        void revoke_deletesKey()
        {
            newStore();

            keyStore.revoke("tok-logout");

            verify(redisTemplate).delete("gm:sign:pub:tok-logout");
        }
    }
}
