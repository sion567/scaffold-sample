package com.scaffold.auth.security;

import java.math.BigInteger;
import java.security.KeyPair;
import java.time.Duration;

import com.scaffold.common.core.crypto.Hexs;
import com.scaffold.common.core.crypto.Sm2Engine;
import com.scaffold.common.core.crypto.Sm2Keys;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 会话签名密钥存取（签名密钥对由后端按会话签发）。
 * <p>
 * 登录后的会话首次请求签名钥时，后端生成 SM2 密钥对：私钥经 TLS 下发给该会话
 * （仅前端内存持有，后端不留存），公钥绑定 token 存 Redis 并带 TTL，登出/过期即销毁
 * ——避免"前端构建期注入静态私钥"带来的密钥明文分发与全局共享私钥问题。
 * <p>
 * 输出格式与前端 sm-crypto 对齐：私钥 = d 的 32 字节 hex（64 字符），
 * 公钥 = 04||X||Y 非压缩 65 字节 hex（130 字符，参与 ZA 计算）。
 */
public class SessionSignKeyStore {

    private static final String KEY_PREFIX = "gm:sign:pub:";

    private final StringRedisTemplate redisTemplate;

    private final Duration ttl;

    public SessionSignKeyStore(StringRedisTemplate redisTemplate, Duration ttl) {
        this.redisTemplate = redisTemplate;
        this.ttl = ttl;
    }

    /**
     * 为会话签发新密钥对：公钥入库（覆盖旧值并重置 TTL），返回私钥 hex 供调用方经 TLS 下发给前端。
     */
    public String issue(String token) {
        KeyPair kp = Sm2Engine.generateKeyPair();
        byte[] pub = Sm2Keys.publicBytes(kp.getPublic());     // 65B: 04||X||Y
        redisTemplate.opsForValue().set(KEY_PREFIX + token, Hexs.encodeHexStr(pub), ttl);
        byte[] priv = Sm2Keys.privateBytes(kp.getPrivate());  // 32B 大端 d
        return Hexs.encodeHexStr(priv);
    }

    /**
     * 取会话验签公钥 hex；未签发/已过期返回 null
     */
    public String publicKeyOf(String token) {
        return redisTemplate.opsForValue().get(KEY_PREFIX + token);
    }

    /**
     * 撤销会话签名密钥（登出）
     */
    public void revoke(String token) {
        redisTemplate.delete(KEY_PREFIX + token);
    }
}
