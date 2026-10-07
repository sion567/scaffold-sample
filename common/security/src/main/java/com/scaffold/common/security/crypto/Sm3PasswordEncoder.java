package com.scaffold.common.security.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import org.bouncycastle.crypto.digests.SM3Digest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * SM3 加盐迭代口令编码器（BouncyCastle 实现）。
 * <p>
 * 原 zdhr-crypto Sm3PasswordEncoder 的同语义替代，存储格式逐字节兼容
 * （存量口令哈希无需迁移）：{@code <Base64(16B盐)>$<Base64(哈希)>}，
 * 哈希 = SM3 对 {@code 盐||密码} 迭代 10000 次；比对用恒定时间比较。
 */
public class Sm3PasswordEncoder implements PasswordEncoder {
    private static final Logger log = LoggerFactory.getLogger(Sm3PasswordEncoder.class);

    private static final int SALT_LENGTH = 16;
    private static final int ITERATIONS = 10000;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * 对外公开：生成 Base64 编码的盐值（方便调试或外部调用）
     */
    public static String generateSalt() {
        byte[] saltBytes = new byte[SALT_LENGTH];
        SECURE_RANDOM.nextBytes(saltBytes);
        return Base64.getEncoder().encodeToString(saltBytes);
    }

    /**
     * 静态入口：对原始密码生成随机盐并哈希，返回 "盐值$最终哈希"。
     * 供非 Spring 环境直接调用（如其他项目设置默认密码后存库）。
     */
    public static String encodePassword(CharSequence rawPassword) {
        if (rawPassword == null) {
            return null;
        }
        return encodePassword(rawPassword, generateSalt());
    }

    /**
     * 静态入口：用指定盐值对原始密码哈希，返回 "盐值$最终哈希"（同盐同密码结果确定）。
     */
    public static String encodePassword(CharSequence rawPassword, String salt) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("rawPassword cannot be null");
        }
        byte[] saltBytes = Base64.getDecoder().decode(salt);
        byte[] passwordBytes = rawPassword.toString().getBytes(StandardCharsets.UTF_8);
        byte[] combined = new byte[saltBytes.length + passwordBytes.length];
        System.arraycopy(saltBytes, 0, combined, 0, saltBytes.length);
        System.arraycopy(passwordBytes, 0, combined, saltBytes.length, passwordBytes.length);
        return encodeWithSalt(combined, saltBytes);
    }

    private static String encodeWithSalt(byte[] combined, byte[] saltBytes) {
        byte[] hash = combined;
        for (int i = 0; i < ITERATIONS; i++) {
            SM3Digest digest = new SM3Digest();
            digest.update(hash, 0, hash.length);
            hash = new byte[digest.getDigestSize()];
            digest.doFinal(hash, 0);
        }
        return Base64.getEncoder().encodeToString(saltBytes)
                + "$" + Base64.getEncoder().encodeToString(hash);
    }

    @Override
    public String encode(CharSequence rawPassword) {
        return encodePassword(rawPassword);
    }

    public String encode(CharSequence rawPassword, String salt) {
        return encodePassword(rawPassword, salt);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        try {
            String[] parts = encodedPassword.split("\\$");
            if (parts.length != 2) {
                return false;
            }
            String recalculated = encodePassword(rawPassword, parts[0]);
            // 恒定时间比对（防时序攻击）
            return MessageDigest.isEqual(
                    Base64.getDecoder().decode(recalculated.split("\\$")[1]),
                    Base64.getDecoder().decode(parts[1]));
        } catch (Exception e) {
            log.debug("SM3 密码比对失败: {}", e.getMessage());
            return false;
        }
    }
}
