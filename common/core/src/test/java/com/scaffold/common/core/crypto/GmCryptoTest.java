package com.scaffold.common.core.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.Security;
import java.util.Arrays;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 国密 BC 实现测试：SM3 已知向量 / HMAC-SM3 与 JCE 交叉验证 /
 * SM2 加解密（C1C3C2 + 历史 C1C2C3/自定义 DER 兼容）与签名验签（DER + 裸 r||s）。
 *
 * @author scaffold
 */
class GmCryptoTest {

    @BeforeAll
    static void registerBouncyCastle() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    // ─── Hexs ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Hexs：大写编码 + 大小写不敏感解码回环")
    void hexRoundTrip() {
        byte[] data = {0x00, 0x0f, (byte) 0xa1, (byte) 0xff};
        String hex = Hexs.encodeHexStr(data);
        assertEquals("000FA1FF", hex);
        assertTrue(Arrays.equals(data, Hexs.decodeHexStr(hex.toLowerCase())));
        assertTrue(Arrays.equals(data, Hexs.decodeHexStr("000fa1ff")));
        assertThrows(IllegalArgumentException.class, () -> Hexs.decodeHexStr("abc"));
        assertThrows(IllegalArgumentException.class, () -> Hexs.decodeHexStr("zz"));
    }

    // ─── SM3 ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("SM3：标准测试向量 SM3(\"abc\")")
    void sm3KnownVector() {
        // GB/T 32905-2016 附录 A 样例
        assertEquals("66C7F0F462EEEDD9D1F2D46BDC10E4E24167C4875CF2F7A2297DA02B8F4BA8E0",
                Sm3Digester.digest("abc"));
        assertEquals(64, Sm3Digester.digest("").length());
    }

    @Test
    @DisplayName("SM3：byte[] 与 String 入口一致")
    void sm3BytesConsistent() {
        String s = "链上留痕-canonical-123";
        assertEquals(Sm3Digester.digest(s),
                Sm3Digester.digest(s.getBytes(StandardCharsets.UTF_8)));
    }

    // ─── HMAC-SM3 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("HMAC-SM3：与 JCE HmacSM3 交叉验证一致")
    void hmacMatchesJce() throws Exception {
        String secret = "gateway-secret-2026";
        String data = "nonce=abc&timestamp=1&user_id=9";
        String expected = Hexs.encodeHexStr(jceHmacSm3(secret.getBytes(StandardCharsets.UTF_8),
                data.getBytes(StandardCharsets.UTF_8)));
        assertEquals(expected, HmacSm3.hmac(secret, data));
        assertEquals(64, HmacSm3.hmac(secret, data).length());
    }

    @Test
    @DisplayName("HMAC-SM3：同钥同文确定，异钥异文不同")
    void hmacDeterministic() {
        assertNotEquals(HmacSm3.hmac("k1", "data"), HmacSm3.hmac("k2", "data"));
        assertEquals(HmacSm3.hmac("k1", "data"), HmacSm3.hmac("k1", "data"));
        assertNotEquals(HmacSm3.hmac("k1", "data"), HmacSm3.hmac("k1", "datab"));
    }

    private static byte[] jceHmacSm3(byte[] key, byte[] data) throws Exception {
        Mac mac = Mac.getInstance("HmacSM3", BouncyCastleProvider.PROVIDER_NAME);
        mac.init(new SecretKeySpec(key, "HmacSM3"));
        return mac.doFinal(data);
    }

    // ─── SM2 ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("SM2 加解密回环：C1C3C2 raw 往返一致")
    void sm2EncryptDecryptRoundTrip() {
        KeyPair kp = Sm2Engine.generateKeyPair();
        Sm2Engine engine = new Sm2Engine(Sm2Keys.privateBytes(kp.getPrivate()),
                Sm2Keys.publicBytes(kp.getPublic()));
        String plain = "sm4-key-hex-0123456789abcdef";
        String cipherHex = engine.encryptHex(plain);
        // C1(65B) + C3(32B) + C2(len)
        assertEquals(65 + 32 + plain.length(), Hexs.decodeHexStr(cipherHex).length);
        assertEquals(plain, engine.decryptStr(cipherHex));
    }

    @Test
    @DisplayName("SM2 解密兼容历史 C1C2C3 序列（auto-try）")
    void sm2DecryptLegacyC1C2C3() {
        KeyPair kp = Sm2Engine.generateKeyPair();
        Sm2Engine engine = new Sm2Engine(Sm2Keys.privateBytes(kp.getPrivate()),
                Sm2Keys.publicBytes(kp.getPublic()));
        byte[] raw = Hexs.decodeHexStr(engine.encryptHex("legacy-payload"));
        // C1C3C2 → C1C2C3（旧序列：C1 || C2 || C3）
        byte[] legacy = new byte[raw.length];
        System.arraycopy(raw, 0, legacy, 0, 65);
        System.arraycopy(raw, 97, legacy, 65, raw.length - 97);
        System.arraycopy(raw, 65, legacy, 65 + (raw.length - 97), 32);
        assertEquals("legacy-payload", engine.decryptStr(Hexs.encodeHexStr(legacy)));
    }

    @Test
    @DisplayName("SM2 解密兼容自定义 DER 历史格式（auto-try）")
    void sm2DecryptLegacyCustomDer() throws Exception {
        KeyPair kp = Sm2Engine.generateKeyPair();
        Sm2Engine engine = new Sm2Engine(Sm2Keys.privateBytes(kp.getPrivate()),
                Sm2Keys.publicBytes(kp.getPublic()));
        String cipherHex = engine.encryptHex("der-payload");
        String derHex = Hexs.encodeHexStr(Sm2Engine.rawToCustomDer(Hexs.decodeHexStr(cipherHex)));
        assertEquals("der-payload", engine.decryptStr(derHex));
    }

    @Test
    @DisplayName("SM2 解密契约：无 04 前缀密文由调用方（GmIdentity）补前缀，引擎本身不处理")
    void sm2DecryptRequires04Prefix() {
        KeyPair kp = Sm2Engine.generateKeyPair();
        Sm2Engine engine = new Sm2Engine(Sm2Keys.privateBytes(kp.getPrivate()),
                Sm2Keys.publicBytes(kp.getPublic()));
        String cipherHex = engine.encryptHex("no-prefix");
        String stripped = cipherHex.substring(2);
        assertNotEquals("04", stripped.substring(0, 2), "前置条件：已剥离 04 前缀");
        // 与 zdhr Sm2Engine 语义一致：补 04 前缀发生在 GmIdentity.decryptSm2
        assertThrows(IllegalStateException.class, () -> engine.decryptStr(stripped));
    }

    @Test
    @DisplayName("SM2 签名验签回环：签发可验，篡改被拒；签名兼容裸 r||s 形态")
    void sm2SignVerify() {
        KeyPair kp = Sm2Engine.generateKeyPair();
        Sm2Engine engine = new Sm2Engine(Sm2Keys.privateBytes(kp.getPrivate()),
                Sm2Keys.publicBytes(kp.getPublic()));
        String sigHex = engine.sign("canonical-string");
        assertTrue(engine.verify("canonical-string", sigHex));
        assertFalse(engine.verify("tampered-string", sigHex));
        assertFalse(engine.verify("canonical-string", sigHex.substring(0, sigHex.length() - 2) + "00"));
        // DER → 裸 r||s（sm-crypto 默认输出形态）仍可验
        byte[] sig = Hexs.decodeHexStr(sigHex);
        BigInteger[] rs = parseDer(sig);
        byte[] raw = new byte[64];
        System.arraycopy(to32(rs[0]), 0, raw, 0, 32);
        System.arraycopy(to32(rs[1]), 0, raw, 32, 32);
        assertTrue(engine.verify("canonical-string", Hexs.encodeHexStr(raw)));
    }

    @Test
    @DisplayName("SM2 换钥拒绝：不同密钥对签发/密文不可用")
    void sm2WrongKeyRejected() {
        KeyPair kp1 = Sm2Engine.generateKeyPair();
        KeyPair kp2 = Sm2Engine.generateKeyPair();
        Sm2Engine engine1 = new Sm2Engine(Sm2Keys.privateBytes(kp1.getPrivate()),
                Sm2Keys.publicBytes(kp1.getPublic()));
        Sm2Engine engine2 = new Sm2Engine(Sm2Keys.privateBytes(kp2.getPrivate()),
                Sm2Keys.publicBytes(kp2.getPublic()));
        assertFalse(engine2.verify("data", engine1.sign("data")));
        assertThrows(Exception.class, () -> engine2.decryptStr(engine1.encryptHex("data")));
    }

    @Test
    @DisplayName("SM2 仅公钥引擎：可验签不可签发；密钥格式校验")
    void sm2PublicKeyOnlyEngine() {
        KeyPair kp = Sm2Engine.generateKeyPair();
        byte[] pub = Sm2Keys.publicBytes(kp.getPublic());
        Sm2Engine signer = new Sm2Engine(Sm2Keys.privateBytes(kp.getPrivate()), pub);
        Sm2Engine verifier = new Sm2Engine(null, pub);
        assertTrue(verifier.verify("msg", signer.sign("msg")));
        assertThrows(IllegalStateException.class, () -> verifier.sign("msg"));
        assertThrows(IllegalArgumentException.class, () -> new Sm2Engine(null, new byte[64]));
        assertThrows(IllegalArgumentException.class, () -> new Sm2Engine(null, new byte[65]));
    }

    private static BigInteger[] parseDer(byte[] der) {
        // SEQUENCE { INTEGER r, INTEGER s }
        assertTrue(der.length > 6 && der[0] == 0x30);
        int idx = 2;
        BigInteger[] out = new BigInteger[2];
        for (int i = 0; i < 2; i++) {
            assertTrue(der[idx] == 0x02);
            int len = der[idx + 1] & 0xFF;
            out[i] = new BigInteger(1, Arrays.copyOfRange(der, idx + 2, idx + 2 + len));
            idx += 2 + len;
        }
        return out;
    }

    private static byte[] to32(BigInteger v) {
        byte[] b = v.toByteArray();
        byte[] out = new byte[32];
        if (b.length <= 32) {
            System.arraycopy(b, 0, out, 32 - b.length, b.length);
        } else {
            System.arraycopy(b, b.length - 32, out, 0, 32);
        }
        return out;
    }
}
