package com.scaffold.common.gm.field;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * GmFieldCrypto 测试：DEK/ENV 双模式加解密回环、密文格式（$SM4$ 版本化头 + AAD 防篡改）、
 * 宽松/严格解密语义。密文格式与原 zdhr-crypto 逐字节兼容。
 *
 * @author scaffold
 */
class GmFieldCryptoTest {

    private static final Pattern GCM_FORMAT =
            Pattern.compile("^\\$SM4\\$v1\\$GCM\\$[0-9a-fA-F]{24}\\$[A-Za-z0-9+/=]+\\$[0-9a-fA-F]{32}$");

    @TempDir
    Path dir;

    private FileSymmetricKeyProvider provider;

    @BeforeEach
    void setUp() throws Exception {
        Files.write(dir.resolve("sm4-root-v1"), "0123456789abcdef".getBytes(StandardCharsets.UTF_8));
        provider = new FileSymmetricKeyProvider(dir.toString(), "v1", 0);
        provider.init();
    }

    @Test
    @DisplayName("DEK 模式：加密格式 $SM4$v1$GCM$<nonce24hex>$<ct b64>$<tag32hex>，往返一致")
    void dekRoundTrip() {
        GmFieldCrypto crypto = new GmFieldCrypto(provider, "DEK");
        crypto.init();
        String plain = "15888888888";
        String cipher = crypto.encrypt(plain);

        assertTrue(GCM_FORMAT.matcher(cipher).matches(), "实际密文: " + cipher);
        assertEquals(plain, crypto.decrypt(cipher));
        assertEquals(plain, crypto.decrypt(cipher, false));
    }

    @Test
    @DisplayName("DEK 模式：nonce 一次一密，同明文两次密文不同")
    void dekNonceUnique() {
        GmFieldCrypto crypto = new GmFieldCrypto(provider, "DEK");
        crypto.init();
        assertNotEquals(crypto.encrypt("same"), crypto.encrypt("same"));
    }

    @Test
    @DisplayName("DEK 模式：空值/ null 直通（encrypt/decrypt 原样返回）")
    void dekEmptyPassthrough() {
        GmFieldCrypto crypto = new GmFieldCrypto(provider, "DEK");
        crypto.init();
        assertEquals("", crypto.encrypt(""));
        assertEquals("", crypto.decrypt(""));
        assertEquals(null, crypto.encrypt(null));
    }

    @Test
    @DisplayName("ENV 模式：wrappedDEK 信封往返一致")
    void envRoundTrip() {
        GmFieldCrypto crypto = new GmFieldCrypto(provider, "KEK");
        crypto.init();
        String cipher = crypto.encrypt("信封数据");
        assertTrue(cipher.startsWith("$SM4$v1$ENV$SM4$"), "实际密文: " + cipher);
        assertEquals("信封数据", crypto.decrypt(cipher));
    }

    @Test
    @DisplayName("ENV 模式：wrappedDEK 长度非法 → 严格解密拒绝")
    void envBadWrappedDek() {
        GmFieldCrypto crypto = new GmFieldCrypto(provider, "KEK");
        crypto.init();
        // 头合法但 wrappedDEK 被截断
        String cipher = "$SM4$v1$ENV$SM4$" + Base64.getEncoder().encodeToString(new byte[10])
                + "$0102030405060708090a0b0c$QQ==$YAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA==";
        assertThrows(RuntimeException.class, () -> crypto.decrypt(cipher));
        assertEquals(cipher, crypto.decrypt(cipher, false), "宽松模式返回原文");
    }

    @Test
    @DisplayName("AAD 防篡改：密文头 version/nonce 任一改动解密失败")
    void headerTamperRejected() {
        GmFieldCrypto crypto = new GmFieldCrypto(provider, "DEK");
        crypto.init();
        String cipher = crypto.encrypt("敏感字段");
        // nonce 改动 → GCM 认证失败
        String[] parts = cipher.split("\\$");
        String tamperedNonce = parts[4].startsWith("0") ? "1" + parts[4].substring(1) : "0" + parts[4].substring(1);
        String tampered = "$SM4$" + parts[2] + "$GCM$" + tamperedNonce + "$" + parts[5] + "$" + parts[6];
        assertNotEquals(cipher, tampered);
        assertThrows(RuntimeException.class, () -> crypto.decrypt(tampered));
        assertEquals(tampered, crypto.decrypt(tampered, false), "宽松模式返回原文");

        // tag 改动 → GCM 认证失败（构造必然不同的尾部，避免 1/256 恰好相同）
        String tagTail = cipher.substring(cipher.length() - 2);
        String tamperedTagTail = "00".equals(tagTail) ? "11" : "00";
        String tagTampered = cipher.substring(0, cipher.length() - 2) + tamperedTagTail;
        assertNotEquals(cipher, tagTampered);
        assertThrows(RuntimeException.class, () -> crypto.decrypt(tagTampered));
    }

    @Test
    @DisplayName("宽松解密：非版本化输入原样返回；严格解密抛异常")
    void lenientDecrypt() {
        GmFieldCrypto crypto = new GmFieldCrypto(provider, "DEK");
        crypto.init();
        assertEquals("plain-text", crypto.decrypt("plain-text", false));
        assertThrows(RuntimeException.class, () -> crypto.decrypt("plain-text"));
        assertFalse(crypto.isVersioned("plain-text"));
        assertTrue(crypto.isVersioned("$SM4$v1$GCM$aa$QQ==$bb"));
        assertDoesNotThrow(() -> crypto.decrypt("$SM4$v1$GCM$$$", false), "畸形头宽松模式不抛");
    }

    @Test
    @DisplayName("holder 注入：init 后 Sm4FieldCrypto 门面可用（FieldCryptoHolder）")
    void holderInjection() {
        GmFieldCrypto crypto = new GmFieldCrypto(provider, "DEK");
        crypto.init();
        assertEquals("门面数据", com.scaffold.common.core.crypto.FieldCryptoHolder.get().decrypt(
                com.scaffold.common.core.crypto.FieldCryptoHolder.get().encrypt("门面数据")));
    }
}
