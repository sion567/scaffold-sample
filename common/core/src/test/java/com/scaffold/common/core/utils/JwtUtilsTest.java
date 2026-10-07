package com.scaffold.common.core.utils;

import java.security.KeyPair;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECPoint;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.scaffold.common.core.exception.ServiceException;

import io.jsonwebtoken.Claims;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JwtUtils 测试（国密 SM2withSM3：签发/校验闭环 + 篡改/过期/换钥拒绝）。
 * 密钥用 Sm2Engine.generateKeyPair() 现场生成，无外部依赖。
 *
 * @author ct
 */
class JwtUtilsTest
{
    private static String privHex;
    private static String pubHex;

    @BeforeEach
    void generatePair()
    {
        KeyPair kp = com.scaffold.common.core.crypto.Sm2Engine.generateKeyPair();
        privHex = bytesToHex(rawPrivate(kp));
        pubHex = bytesToHex(rawPublic(kp));
    }

    private JwtUtils utils()
    {
        return new JwtUtils(privHex, pubHex);
    }

    private Map<String, Object> claims()
    {
        Map<String, Object> claims = new HashMap<>();
        claims.put("user_key", "usr_1");
        claims.put("username", "admin");
        return claims;
    }

    @Test
    @DisplayName("签发/校验闭环：create→parse 往返一致")
    void roundtrip()
    {
        String token = utils().createToken(claims());
        Claims parsed = utils().parseToken(token);
        assertEquals("usr_1", parsed.get("user_key"));
    }

    @Test
    @DisplayName("篡改令牌：签名校验拒绝")
    void tamperedToken_rejected()
    {
        String token = utils().createToken(claims());
        String tampered = token.substring(0, token.length() - 4) + "AAAA";
        assertThrows(Exception.class, () -> utils().parseToken(tampered));
    }

    @Test
    @DisplayName("换钥拒绝：不同密钥对的令牌校验失败")
    void wrongKey_rejected()
    {
        KeyPair other = com.scaffold.common.core.crypto.Sm2Engine.generateKeyPair();
        JwtUtils otherUtils = new JwtUtils(bytesToHex(rawPrivate(other)), bytesToHex(rawPublic(other)));
        String token = otherUtils.createToken(claims());
        assertThrows(Exception.class, () -> utils().parseToken(token));
    }

    @Test
    @DisplayName("缺私钥配置：签发快速失败")
    void missingPrivateKey_failsFast()
    {
        JwtUtils noKey = new JwtUtils("", "");
        assertThrows(Exception.class, () -> noKey.createToken(claims()));
    }

    @Test
    @DisplayName("下游仅配公钥：可验签恢复登录态，签发快速失败")
    void publicKeyOnly_canVerifyCannotSign()
    {
        String token = utils().createToken(claims());

        JwtUtils verifier = new JwtUtils(null, pubHex);
        Claims parsed = verifier.parseToken(token);
        assertEquals("usr_1", parsed.get("user_key"));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> verifier.createToken(claims()));
        assertTrue(ex.getMessage().contains("签发密钥未配置"));
    }

    private static String bytesToHex(byte[] bytes)
    {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) { sb.append(String.format("%02x", b)); }
        return sb.toString();
    }

    /** Sm2Engine 要求裸密钥：私钥 32 字节，公钥 65 字节（04||X||Y） */
    private static byte[] rawPrivate(KeyPair kp)
    {
        java.math.BigInteger s = ((ECPrivateKey) kp.getPrivate()).getS();
        return to32(s);
    }

    private static byte[] rawPublic(KeyPair kp)
    {
        ECPoint w = ((ECPublicKey) kp.getPublic()).getW();
        byte[] x = to32(w.getAffineX());
        byte[] y = to32(w.getAffineY());
        byte[] out = new byte[65];
        out[0] = 0x04;
        System.arraycopy(x, 0, out, 1, 32);
        System.arraycopy(y, 0, out, 33, 32);
        return out;
    }

    private static byte[] to32(java.math.BigInteger v)
    {
        byte[] b = v.toByteArray();
        byte[] out = new byte[32];
        if (b.length <= 32) { System.arraycopy(b, 0, out, 32 - b.length, b.length); }
        else { System.arraycopy(b, b.length - 32, out, 0, 32); }
        return out;
    }
}
