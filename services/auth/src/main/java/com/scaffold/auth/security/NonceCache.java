package com.scaffold.auth.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * nonce 去重缓存（信封防重放）。
 * <p>
 * 原 zdhr-crypto webmvc NonceCache 的本地化：仅 auth 信封链路使用，
 * lib 不提供默认实现，由消费方提供（本工程为 {@link RedisNonceCache}）。
 */
public interface NonceCache {

    Logger log = LoggerFactory.getLogger(NonceCache.class);

    boolean tryRegister(String keyId, String nonce);
}
