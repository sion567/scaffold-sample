package com.scaffold.auth.security;

import com.scaffold.common.gm.keystore.GmIdentity;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 国密信封解密装配（可选的合规增强项，默认关闭）。
 * <p>
 * 解密点为 Dubbo provider 过滤器 {@link GmEnvelopeDecryptFilter}（SPI，作用于
 * Triple REST 反序列化之后的调用层）：解密器按 {@code scaffold.auth.gm-envelope.enabled=true}
 * 条件创建并绑定到过滤器（matchIfMissing=false），关闭时过滤器对所有调用直接放行，
 * 也不依赖国密 keystore；需要信封加密能力时再显式开启。
 * <p>
 * nonce 缓存与会话验签公钥走 Redis（多实例共享）；SessionSignKeyStore 无条件注册——
 * 仅依赖 Redis，登出撤销逻辑在信封关闭时也可用。
 */
@Configuration
@EnableConfigurationProperties(GmEnvelopeProperties.class)
public class GmEnvelopeConfiguration {

    @Bean
    public NonceCache gmEnvelopeNonceCache(GmEnvelopeProperties props, RedisTemplate redisTemplate) {
        return new RedisNonceCache(redisTemplate, props.getTimestampWindow());
    }

    /** 会话签名密钥存取：无条件注册，登出撤销逻辑在信封关闭时也可用 */
    @Bean
    public SessionSignKeyStore sessionSignKeyStore(GmEnvelopeProperties props, StringRedisTemplate redisTemplate) {
        return new SessionSignKeyStore(redisTemplate, props.getSignKeyTtl());
    }

    /**
     * 信封解密核心：enabled=true 时创建并绑定到 SPI 过滤器（SPI 实例不受 Spring 管理，
     * 经静态引用委托）；默认关闭时不绑定，过滤器对所有调用直接放行。
     */
    @Bean
    @ConditionalOnProperty(name = "scaffold.auth.gm-envelope.enabled", havingValue = "true", matchIfMissing = false)
    public GmEnvelopeDecryptor gmEnvelopeDecryptor(GmEnvelopeProperties props, GmIdentity gmIdentity,
                                                   NonceCache gmEnvelopeNonceCache, SessionSignKeyStore sessionSignKeyStore) {
        GmEnvelopeDecryptor decryptor = new GmEnvelopeDecryptor(props, gmIdentity, gmEnvelopeNonceCache, sessionSignKeyStore);
        GmEnvelopeDecryptFilter.bind(decryptor);
        return decryptor;
    }
}
