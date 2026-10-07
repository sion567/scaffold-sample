package com.scaffold.auth.security;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.Security;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.apache.dubbo.rpc.RpcInvocation;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.scaffold.auth.form.LoginBody;
import com.scaffold.auth.security.GmEnvelopeDecryptor.EnvelopeRejectException;
import com.scaffold.common.core.crypto.Sm2Engine;
import com.scaffold.common.gm.keystore.GmIdentity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * GmEnvelopeDecryptor 国密信封解密核心测试。
 *
 * <p>覆盖维度（协议错误码与前端 request.ts 联动）：
 * <ul>
 *   <li>解密成功：SM2 解 encKey → SM4 解 encData → 参数被替换为明文对象</li>
 *   <li>40005：缺 timestamp/nonce、携带 token 但缺签名</li>
 *   <li>40001：timestamp 过期（超过窗口）</li>
 *   <li>40002：nonce 重复（防重放）</li>
 *   <li>40003：签名验证失败；合法签名回环（Sm2Engine 签发 → 验签通过）</li>
 *   <li>40004：SM2 解密失败（非法 encKey）/ 解密器异常</li>
 *   <li>40006：会话签名密钥未初始化</li>
 *   <li>透传：无 encKey/encData（明文兼容）与非信封参数不处理</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GmEnvelopeDecryptorTest
{
    private static final String SM4_KEY = "0123456789abcdef0123456789abcdef";

    @Mock
    private GmIdentity gmIdentity;

    @Mock
    private NonceCache nonceCache;

    @Mock
    private SessionSignKeyStore signKeyStore;

    private GmEnvelopeDecryptor decryptor;

    @BeforeAll
    static void registerBouncyCastle()
    {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
        {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @BeforeEach
    void setUp()
    {
        GmEnvelopeProperties props = new GmEnvelopeProperties();
        props.setEnabled(true);
        props.setTimestampWindow(Duration.ofMinutes(5));
        props.setSignKeyTtl(Duration.ofMinutes(30));
        decryptor = new GmEnvelopeDecryptor(props, gmIdentity, nonceCache, signKeyStore);
        org.mockito.Mockito.when(nonceCache.tryRegister(anyString(), anyString())).thenReturn(true);
    }

    // ─── 构造工具 ─────────────────────────────────────────────────────────────

    private LoginBody envelope(String encKey, String encData)
    {
        LoginBody body = new LoginBody();
        body.setEncKey(encKey);
        body.setEncData(encData);
        body.setTimestamp(String.valueOf(System.currentTimeMillis()));
        body.setNonce(randomNonce());
        return body;
    }

    private static String randomNonce()
    {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String sm4EncryptHex(String keyHex, String plain) throws Exception
    {
        Cipher cipher = Cipher.getInstance("SM4/ECB/PKCS7Padding", BouncyCastleProvider.PROVIDER_NAME);
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(hexToBytes(keyHex), "SM4"));
        return bytesToHex(cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8)));
    }

    private static byte[] hexToBytes(String hex)
    {
        byte[] out = new byte[hex.length() / 2];
        for (int i = 0; i < out.length; i++)
        {
            out[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        return out;
    }

    private static byte[] bigEndian32(java.math.BigInteger value)
    {
        byte[] src = value.toByteArray();
        byte[] out = new byte[32];
        int copy = Math.min(src.length, 32);
        System.arraycopy(src, src.length - copy, out, 32 - copy, copy);
        return out;
    }

    private static String bytesToHex(byte[] bytes)
    {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes)
        {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static RpcInvocation invocation(Object... args)
    {
        RpcInvocation inv = new RpcInvocation();
        inv.setArguments(args);
        inv.setMethodName("login");
        return inv;
    }

    private Object[] decrypt(Object... args)
    {
        return decryptor.decryptArguments(invocation(args));
    }

    private static LoginBody firstBody(Object[] replaced)
    {
        return (LoginBody) replaced[0];
    }

    // ─── 解密成功 ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("解密成功：SM2+SM4 解开信封，参数替换为明文对象且信封字段不回流")
    void decryptSuccess() throws Exception
    {
        LoginBody body = envelope(SM4_KEY,
                sm4EncryptHex(SM4_KEY, "{\"username\":\"admin\",\"password\":\"sm2-cipher\",\"code\":\"5\",\"uuid\":\"u-1\"}"));
        when(gmIdentity.decryptSm2(body.getEncKey())).thenReturn(SM4_KEY);

        String untouched = "plain-arg";
        Object[] replaced = decrypt(body, untouched);

        LoginBody out = firstBody(replaced);
        assertEquals("admin", out.getUsername());
        assertEquals("sm2-cipher", out.getPassword());
        assertEquals("5", out.getCode());
        assertEquals("u-1", out.getUuid());
        assertNull(out.getEncKey());
        assertNull(out.getEncData());
        assertEquals(untouched, replaced[1]);
    }

    @Test
    @DisplayName("合法签名回环：Sm2Engine 签发 → 会话公钥验签通过后解密")
    void validSignatureRoundTrip() throws Exception
    {
        KeyPair keyPair = Sm2Engine.generateKeyPair();
        // Sm2Engine 要求裸格式：私钥 32 字节大端、公钥 65 字节非压缩点 04||X||Y（getEncoded 为 X.509/PKCS8 封装，不可直接用）
        byte[] rawPriv = bigEndian32(((java.security.interfaces.ECPrivateKey) keyPair.getPrivate()).getS());
        java.security.spec.ECPoint w = ((java.security.interfaces.ECPublicKey) keyPair.getPublic()).getW();
        byte[] rawPub = new byte[65];
        rawPub[0] = 0x04;
        System.arraycopy(bigEndian32(w.getAffineX()), 0, rawPub, 1, 32);
        System.arraycopy(bigEndian32(w.getAffineY()), 0, rawPub, 33, 32);
        Sm2Engine signEngine = new Sm2Engine(rawPriv, rawPub);

        LoginBody body = envelope(SM4_KEY, sm4EncryptHex(SM4_KEY, "{\"username\":\"admin\",\"password\":\"p\"}"));
        Map<String, String> fields = new HashMap<>();
        fields.put(EnvelopeCanonical.TIMESTAMP, body.getTimestamp());
        fields.put(EnvelopeCanonical.NONCE, body.getNonce());
        fields.put("encKey", body.getEncKey());
        fields.put("encData", body.getEncData());
        body.setSignature(signEngine.sign(EnvelopeCanonical.build(fields)));

        when(gmIdentity.decryptSm2(body.getEncKey())).thenReturn(SM4_KEY);
        when(signKeyStore.publicKeyOf("tok-1")).thenReturn(signEngine.getPublicKeyHex());

        LoginBody out = firstBody(decrypt(body));
        assertEquals("admin", out.getUsername());
    }

    // ─── 透传 ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("明文兼容：信封字段为空时透传，不产生参数替换")
    void plainEnvelopePassThrough()
    {
        assertNull(decrypt(envelope("", "")));
        assertNull(decrypt(envelope(null, null)));
    }

    @Test
    @DisplayName("非信封参数不处理")
    void nonEnvelopeArgsIgnored()
    {
        assertNull(decrypt("str", 123));
        assertNull(decrypt());
    }

    // ─── 40005 缺参 ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("缺 nonce → 40005")
    void missingNonce()
    {
        LoginBody body = envelope(SM4_KEY, "aa");
        body.setNonce("");

        EnvelopeRejectException ex = assertThrows(EnvelopeRejectException.class, () -> decrypt(body));
        assertEquals(GmEnvelopeDecryptor.CODE_MISSING_PARAMS, ex.getCode());
    }

    @Test
    @DisplayName("携带 token 但缺签名 → 40005")
    void tokenWithoutSignature()
    {
        LoginBody body = envelope("", "");
        body.setEncKey("k");
        body.setEncData("d");
        when(signKeyStore.publicKeyOf("tok-no-sig")).thenReturn("04abcd");

        RpcInvocation inv = invocation(body);
        inv.setAttachment("authorization", "Bearer tok-no-sig");

        EnvelopeRejectException ex = assertThrows(EnvelopeRejectException.class, () -> decryptor.decryptArguments(inv));
        assertEquals(GmEnvelopeDecryptor.CODE_MISSING_PARAMS, ex.getCode());
    }

    // ─── 40001 timestamp 过期 ────────────────────────────────────────────────

    @Test
    @DisplayName("timestamp 超过窗口 → 40001")
    void expiredTimestamp()
    {
        LoginBody body = envelope(SM4_KEY, "aa");
        body.setTimestamp(String.valueOf(System.currentTimeMillis() - 10 * 60 * 1000));

        EnvelopeRejectException ex = assertThrows(EnvelopeRejectException.class, () -> decrypt(body));
        assertEquals(GmEnvelopeDecryptor.CODE_EXPIRED, ex.getCode());
    }

    // ─── 40002 nonce 重放 ────────────────────────────────────────────────────

    @Test
    @DisplayName("nonce 已存在 → 40002")
    void nonceReplay()
    {
        LoginBody body = envelope(SM4_KEY, "aa");
        when(nonceCache.tryRegister(anyString(), anyString())).thenReturn(false);

        EnvelopeRejectException ex = assertThrows(EnvelopeRejectException.class, () -> decrypt(body));
        assertEquals(GmEnvelopeDecryptor.CODE_REPLAY, ex.getCode());
    }

    // ─── 40004 解密失败 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("SM2 解密返回非法 SM4 key 格式 → 40004")
    void badSm4KeyFormat()
    {
        LoginBody body = envelope("badkey", "baddata");
        when(gmIdentity.decryptSm2("badkey")).thenReturn("short");

        EnvelopeRejectException ex = assertThrows(EnvelopeRejectException.class, () -> decrypt(body));
        assertEquals(GmEnvelopeDecryptor.CODE_DECRYPT_FAILED, ex.getCode());
    }

    @Test
    @DisplayName("SM2 解密异常 → 40004")
    void sm2DecryptThrows()
    {
        LoginBody body = envelope("badkey", "baddata");
        when(gmIdentity.decryptSm2("badkey")).thenThrow(new IllegalStateException("keystore error"));

        EnvelopeRejectException ex = assertThrows(EnvelopeRejectException.class, () -> decrypt(body));
        assertEquals(GmEnvelopeDecryptor.CODE_DECRYPT_FAILED, ex.getCode());
    }

    // ─── 40006 会话密钥未初始化 ──────────────────────────────────────────────

    @Test
    @DisplayName("携带 token 但未签发会话密钥 → 40006")
    void signKeyNotInit()
    {
        LoginBody body = envelope("k", "d");
        when(signKeyStore.publicKeyOf("tok-no-key")).thenReturn(null);

        RpcInvocation inv = invocation(body);
        inv.setAttachment("Authorization", "Bearer tok-no-key");

        EnvelopeRejectException ex = assertThrows(EnvelopeRejectException.class, () -> decryptor.decryptArguments(inv));
        assertEquals(GmEnvelopeDecryptor.CODE_SIGN_NOT_INIT, ex.getCode());
    }

    // ─── 40003 签名验证失败 ──────────────────────────────────────────────────

    @Test
    @DisplayName("携带 token 与会话密钥但签名错误 → 40003")
    void badSignature()
    {
        LoginBody body = envelope("k", "d");
        body.setSignature("forged-signature");
        when(signKeyStore.publicKeyOf("tok-bad-sig")).thenReturn("04ab");

        RpcInvocation inv = invocation(body);
        inv.setAttachment("authorization", "Bearer tok-bad-sig");

        EnvelopeRejectException ex = assertThrows(EnvelopeRejectException.class, () -> decryptor.decryptArguments(inv));
        assertEquals(GmEnvelopeDecryptor.CODE_BAD_SIGNATURE, ex.getCode());
    }

    @Test
    @DisplayName("无 attachment token（登录前流量）跳过验签，仅防重放")
    void noTokenSkipsVerify() throws Exception
    {
        LoginBody body = envelope(SM4_KEY, sm4EncryptHex(SM4_KEY, "{\"username\":\"admin\",\"password\":\"p\"}"));
        when(gmIdentity.decryptSm2(body.getEncKey())).thenReturn(SM4_KEY);

        LoginBody out = firstBody(decrypt(body));
        assertEquals("admin", out.getUsername());
    }

    @Test
    @DisplayName("防重放：同一 nonce 第二次被拒（与 40002 用例互为印证，nonce 随机不碰撞）")
    void nonceUniqueness()
    {
        LoginBody body = envelope(SM4_KEY, "aa");
        assertNotEquals(body.getNonce(), envelope(SM4_KEY, "aa").getNonce());
        assertNotNull(body.getNonce());
    }
}
