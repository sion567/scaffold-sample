package com.scaffold.common.security.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.Security;
import java.util.Base64;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Sm3PasswordEncoder 测试：格式 salt$hash、迭代哈希与 JCE SM3 交叉验证、
 * matches 回环与恒定失败路径。存储格式与原 zdhr-crypto 实现逐字节兼容
 * （同盐同密码必得同哈希，迭代 10000 次）。
 *
 * @author scaffold
 */
class Sm3PasswordEncoderTest {

    @BeforeAll
    static void registerBouncyCastle() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @Test
    @DisplayName("encode：salt$hash 格式，盐 16B / 哈希 32B")
    void encodeFormat() {
        String encoded = Sm3PasswordEncoder.encodePassword("Admin@123");
        String[] parts = encoded.split("\\$");
        assertEquals(2, parts.length);
        assertEquals(16, Base64.getDecoder().decode(parts[0]).length, "盐为 16 字节");
        assertEquals(32, Base64.getDecoder().decode(parts[1]).length, "SM3 摘要为 32 字节");
    }

    @Test
    @DisplayName("迭代哈希：与 JCE SM3 逐次迭代 10000 次结果一致")
    void iterationMatchesJce() throws Exception {
        String salt = Sm3PasswordEncoder.generateSalt();
        String encoded = Sm3PasswordEncoder.encodePassword("Admin@123", salt);

        byte[] saltBytes = Base64.getDecoder().decode(salt);
        byte[] combined = concat(saltBytes, "Admin@123".getBytes(StandardCharsets.UTF_8));
        MessageDigest sm3 = MessageDigest.getInstance("SM3", BouncyCastleProvider.PROVIDER_NAME);
        byte[] hash = combined;
        for (int i = 0; i < 10000; i++) {
            hash = sm3.digest(hash);
        }
        assertEquals(Base64.getEncoder().encodeToString(hash), encoded.split("\\$")[1]);
    }

    @Test
    @DisplayName("同盐同密码确定；不同盐不同结果；随机盐互不相同")
    void deterministicAndSalted() {
        assertEquals(Sm3PasswordEncoder.encodePassword("p", "QUJDREVGR0hJSktMTU5PUA=="),
                Sm3PasswordEncoder.encodePassword("p", "QUJDREVGR0hJSktMTU5PUA=="));
        assertNotEquals(Sm3PasswordEncoder.encodePassword("p", "QUJDREVGR0hJSktMTU5PUA=="),
                Sm3PasswordEncoder.encodePassword("p", "YWJjZGVmZ2hpamtsbW5vcA=="));
        assertNotEquals(Sm3PasswordEncoder.encodePassword("p"), Sm3PasswordEncoder.encodePassword("p"),
                "随机盐下两次编码结果必不同");
    }

    @Test
    @DisplayName("matches：正确密码通过，错误密码/非法存储串拒绝")
    void matchesPaths() {
        String encoded = Sm3PasswordEncoder.encodePassword("Admin@123");
        assertTrue(new Sm3PasswordEncoder().matches("Admin@123", encoded));
        assertFalse(new Sm3PasswordEncoder().matches("admin@123", encoded));
        assertFalse(new Sm3PasswordEncoder().matches("Admin@123", "not-a-salthash"));
        assertFalse(new Sm3PasswordEncoder().matches("Admin@123", "abc$###"));
        assertFalse(new Sm3PasswordEncoder().matches(null, encoded));
        assertFalse(new Sm3PasswordEncoder().matches("Admin@123", null));
    }

    @Test
    @DisplayName("PasswordEncoder 接口语义：null 返回 null，空密码可编码可验证")
    void encoderContract() {
        assertNull(Sm3PasswordEncoder.encodePassword(null));
        Sm3PasswordEncoder encoder = new Sm3PasswordEncoder();
        String emptyEncoded = encoder.encode("");
        assertTrue(emptyEncoded.split("\\$").length == 2);
        assertTrue(encoder.matches("", emptyEncoded));
        assertThrows(IllegalArgumentException.class, () -> encoder.encode("x", "!!!非base64盐"));
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] out = new byte[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }
}
