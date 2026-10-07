package com.scaffold.common.core.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.TreeMap;

/**
 * 网关身份头 HMAC-SM3 签名：网关签发（GatewaySignFilter），下游 Dubbo 提供端验签（GatewaySignVerifyFilter）。
 * <p>
 * 背景：网关 AuthFilter 验完 token 后把 user_id/user_key/username 明文透传给下游，
 * 内网直连服务伪造这些头即可绕过网关。本类对 身份头 + timestamp + nonce 做 HMAC-SM3，
 * 下游验签确认请求确实来自网关。算法用国密 SM3（合规要求，勿改回 SHA 系）。
 * <p>
 * 签名串拼装风格与 auth 的 EnvelopeCanonical 保持一致：参数名 ASCII 升序、空值不参与、& 连接。
 * 身份头取网关透传的原始值（username 为 urlEncode 后的值），签名与验签两侧一致，与解码无关。
 */
public final class GatewaySm3Signature
{
    public static final String HEADER_TIMESTAMP = "X-Gateway-Ts";

    public static final String HEADER_NONCE = "X-Gateway-Nonce";

    public static final String HEADER_SIGN = "X-Gateway-Sign";

    private GatewaySm3Signature()
    {
    }

    /**
     * 签名：sign = hex( HMAC-SM3(secret, 规范化签名串) )
     *
     * @param secret 共享内部密钥（网关与下游一致，来自 Nacos/环境变量）
     * @param identityParams 身份参数（user_id/user_key/username），空值不参与签名
     * @param timestamp 毫秒时间戳
     * @param nonce 单次随机串
     */
    public static String sign(String secret, Map<String, String> identityParams, String timestamp, String nonce)
    {
        StringBuilder canonical = buildCanonical(identityParams, timestamp, nonce);
        return HmacSm3.hmac(secret, canonical.toString());
    }

    /**
     * 验签：常量时间比较防时序攻击；secret/timestamp/nonce/sign 任一为空直接失败
     */
    public static boolean verify(String secret, Map<String, String> identityParams,
                                 String timestamp, String nonce, String sign)
    {
        if (isEmpty(secret) || isEmpty(timestamp) || isEmpty(nonce) || isEmpty(sign))
        {
            return false;
        }
        String expected = sign(secret, identityParams, timestamp, nonce);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                sign.getBytes(StandardCharsets.UTF_8));
    }

    public static StringBuilder buildCanonical(Map<String, String> identityParams, String timestamp, String nonce)
    {
        TreeMap<String, String> sorted = new TreeMap<>();
        sorted.put("nonce", nonce);
        sorted.put("timestamp", timestamp);
        if (identityParams != null)
        {
            identityParams.forEach((k, v) -> {
                if (!isEmpty(v))
                {
                    sorted.put(k, v);
                }
            });
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : sorted.entrySet())
        {
            if (sb.length() > 0)
            {
                sb.append('&');
            }
            sb.append(entry.getKey()).append('=').append(entry.getValue());
        }
        return sb;
    }

    private static boolean isEmpty(String s)
    {
        return s == null || s.isEmpty();
    }
}
