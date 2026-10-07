package com.scaffold.common.core.utils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.scaffold.common.core.constant.SecurityConstants;
import com.scaffold.common.core.text.Convert;
import com.alibaba.fastjson2.JSON;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SignatureException;

import com.scaffold.common.core.crypto.Sm2Engine;

/**
 * Jwt工具类（国密 SM2withSM3 签名，密评强制组合；jjwt 不支持该算法，
 * 采用标准 JWS compact 结构手写签发/校验：b64u(header).b64u(payload).b64u(SM2签名)）。
 *
 * <p><b>密钥管理（禁止硬编码）</b>：SM2 密钥对经 @Value 注入（hex 裸密钥：
 * 私钥 32 字节 / 公钥 65 字节 04||X||Y），来源为配置项 jwt.sm2.private-key /
 * jwt.sm2.public-key（Nacos/启动参数，可用环境变量占位）。缺失或长度非法时
 * 首次签发/校验快速失败。签发/校验服务必须配置同一密钥对：私钥仅
 * scaffold-auth / scaffold-gateway（签发侧）；<b>凡经 SecurityContextFilter 恢复
 * LOGIN_USER 的下游服务（system/message/audit/job/gen/样例等）至少要配 public-key</b>，
 * 否则 token 无法解析、登录态静默丢失（getLoginUser() 返回 null）。泄露后轮换需
 * 同步所有节点并强制重新登录。</p>
 *
 * @author ct
 */
@Component
public class JwtUtils
{
    private static final Logger log = LoggerFactory.getLogger(JwtUtils.class);

    /** JWS 头（固定：alg=SM2 表示 SM2withSM3 签名） */
    private static final String HEADER_JSON = "{\"alg\":\"SM2\",\"typ\":\"JWT\"}";

    /** 密钥（@Value 注入；volatile 保证多线程可见。下游校验服务只配公钥属合法形态） */
    private static volatile String sm2PrivateHex;

    private static volatile String sm2PublicHex;

    /** 缓存的引擎（首次使用时构建）：签发需私钥+公钥，校验仅需公钥（下游服务合法形态） */
    private static volatile Sm2Engine signEngine;

    private static volatile Sm2Engine verifyEngine;

    public JwtUtils(@Value("${jwt.sm2.private-key:}") String privateKeyHex,
            @Value("${jwt.sm2.public-key:}") String publicKeyHex)
    {
        configure(privateKeyHex, publicKeyHex);
        logKeyStatus();
    }

    /**
     * 启动期密钥自检：把"登录态静默失败"提前到部署时刻暴露。
     * 下游验签服务缺 public-key 是本架构最典型的静默故障（token 解析失败 → getLoginUser() 恒为
     * null），此处只打 ERROR 不阻断启动——不解析 token 的服务仍可运行，但问题在启动日志即可见。
     */
    private static void logKeyStatus()
    {
        boolean hasPrivate = !isBlank(sm2PrivateHex);
        boolean hasPublic = !isBlank(sm2PublicHex);
        if (!hasPublic && !hasPrivate)
        {
            log.error("==================== JWT SM2 密钥未配置 =====================\n"
                    + "当前服务未配置 jwt.sm2.public-key，token 解析将全部失败：\n"
                    + "SecurityContextFilter 无法恢复 LOGIN_USER，getLoginUser() 恒为 null。\n"
                    + "下游服务(system/message/audit/job/gen等)至少配置 jwt.sm2.public-key\n"
                    + "(130 位 hex，与签发方 scaffold-auth 同一对)；签发侧(gateway/auth)\n"
                    + "还需 jwt.sm2.private-key(64 位 hex)。生产经 Nacos/环境变量注入。");
        }
        else if (!hasPublic)
        {
            log.error("JWT SM2 仅配置了私钥(jwt.sm2.private-key)：签发与验签引擎均无法构建"
                    + "（签发需要公私钥对），请补配 jwt.sm2.public-key");
        }
        else
        {
            String pubProblem = keyProblem(sm2PublicHex, 130, true);
            if (pubProblem != null)
            {
                log.error("JWT SM2 验签公钥(jwt.sm2.public-key)格式非法：{}，token 解析将失败", pubProblem);
            }
            else if (hasPrivate)
            {
                String privProblem = keyProblem(sm2PrivateHex, 64, false);
                if (privProblem != null)
                {
                    log.error("JWT SM2 私钥(jwt.sm2.private-key)格式非法：{}，签发将失败", privProblem);
                }
                else
                {
                    log.info("JWT SM2 密钥已配置：签发+验签均可用（签发侧形态）");
                }
            }
            else
            {
                log.info("JWT SM2 验签公钥已配置：token 解析可用（下游验签形态）");
            }
        }
    }

    /** 密钥格式校验：返回问题描述，合法返回 null */
    private static String keyProblem(String hex, int expectedLength, boolean mustStart04)
    {
        if (hex.length() != expectedLength)
        {
            return "长度 " + hex.length() + " ≠ 期望 " + expectedLength;
        }
        if (hex.chars().anyMatch(c -> Character.digit(c, 16) < 0))
        {
            return "包含非 hex 字符";
        }
        if (mustStart04 && !hex.startsWith("04"))
        {
            return "非 04||X||Y 非压缩公钥格式";
        }
        return null;
    }

    /** 配置密钥（hex 裸密钥：私钥 32 字节、公钥 65 字节；空=未配置，首次使用按操作快速失败） */
    static synchronized void configure(String privateKeyHex, String publicKeyHex)
    {
        sm2PrivateHex = privateKeyHex;
        sm2PublicHex = publicKeyHex;
        signEngine = null;
        verifyEngine = null;
    }

    /** 签发引擎（仅签发侧 auth/gateway 可用）：私钥与公钥都必须配置 */
    private static Sm2Engine signEngine()
    {
        if (isBlank(sm2PrivateHex) || isBlank(sm2PublicHex))
        {
            throw new IllegalStateException(
                    "JWT SM2 签发密钥未配置：请设置 jwt.sm2.private-key 与 jwt.sm2.public-key"
                            + "（hex，私钥 64 字符、公钥 130 字符）；仅签发侧（scaffold-auth/gateway）需要私钥");
        }
        synchronized (JwtUtils.class)
        {
            if (signEngine == null)
            {
                signEngine = new Sm2Engine(hexToBytes(sm2PrivateHex), hexToBytes(sm2PublicHex));
            }
        }
        return signEngine;
    }

    /** 校验引擎（下游服务只配 jwt.sm2.public-key 即可）：公钥必须配置 */
    private static Sm2Engine verifyEngine()
    {
        if (isBlank(sm2PublicHex))
        {
            throw new IllegalStateException(
                    "JWT SM2 验签公钥未配置：请设置 jwt.sm2.public-key"
                            + "（hex，公钥 130 字符，与签发方 scaffold-auth 同一对）；缺失则登录态无法恢复");
        }
        synchronized (JwtUtils.class)
        {
            if (verifyEngine == null)
            {
                // 仅公钥构造：可验签、不可签发（私钥不出签发侧）
                verifyEngine = new Sm2Engine(null, hexToBytes(sm2PublicHex));
            }
        }
        return verifyEngine;
    }

    private static boolean isBlank(String v) { return v == null || v.isEmpty(); }

    private static String signingInput(String encodedHeader, String encodedPayload)
    {
        return encodedHeader + "." + encodedPayload;
    }

    /**
     * 从数据声明生成令牌
     *
     * @param claims 数据声明
     * @return 令牌
     */
    public static String createToken(Map<String, Object> claims)
    {
        return createToken(claims, null);
    }

    /**
     * 从数据声明生成令牌（支持自定义过期时间，等保要求 JWT 自带 exp 与 Redis 会话有效期一致）
     *
     * @param claims 数据声明
     * @param expiration 过期时间（毫秒时间戳），null 则不设置 exp
     * @return 令牌
     */
    public static String createToken(Map<String, Object> claims, Date expiration)
    {
        Map<String, Object> payload = new LinkedHashMap<>(claims);
        if (expiration != null)
        {
            payload.put("exp", expiration.getTime() / 1000L);
        }
        String headerB64 = base64Url(HEADER_JSON.getBytes(StandardCharsets.UTF_8));
        String payloadB64 = base64Url(JSON.toJSONString(payload).getBytes(StandardCharsets.UTF_8));
        String signingInput = signingInput(headerB64, payloadB64);
        return signingInput + "." + base64Url(hexToBytes(sign(signingInput)));
    }

    /**
     * 从令牌中获取数据声明（含 SM2 签名校验与 exp 过期校验）
     *
     * @param token 令牌
     * @return 数据声明
     */
    public static Claims parseToken(String token)
    {
        String[] parts = token.split("\\.");
        if (parts.length != 3)
        {
            throw new SignatureException("非法令牌结构（应为三段式）");
        }
        // 第三段为 b64u(SM2签名)，先还原为引擎所需的 hex 再验签
        String sigHex = bytesToHex(base64UrlDecode(parts[2]));
        if (!verify(signingInput(parts[0], parts[1]), sigHex))
        {
            throw new SignatureException("令牌签名校验失败");
        }
        String payloadJson = new String(base64UrlDecode(parts[1]), StandardCharsets.UTF_8);
        Map<String, Object> map = JSON.parseObject(payloadJson);
        Claims claims = Jwts.claims(map);
        Object exp = claims.get("exp");
        if (exp instanceof Number && ((Number) exp).longValue() * 1000L < System.currentTimeMillis())
        {
            throw new ExpiredJwtException(null, claims, "令牌已过期");
        }
        return claims;
    }

    private static String sign(String signingInput)
    {
        return signEngine().sign(signingInput);
    }

    private static boolean verify(String signingInput, String sigHex)
    {
        return verifyEngine().verify(signingInput, sigHex);
    }

    /**
     * 根据令牌获取用户标识
     *
     * @param token 令牌
     * @return 用户ID
     */
    public static String getUserKey(String token)
    {
        Claims claims = parseToken(token);
        return getValue(claims, SecurityConstants.USER_KEY);
    }

    /**
     * 根据令牌获取用户标识
     *
     * @param claims 身份信息
     * @return 用户ID
     */
    public static String getUserKey(Claims claims)
    {
        return getValue(claims, SecurityConstants.USER_KEY);
    }

    /**
     * 根据令牌获取用户ID
     *
     * @param token 令牌
     * @return 用户ID
     */
    public static String getUserId(String token)
    {
        Claims claims = parseToken(token);
        return getValue(claims, SecurityConstants.DETAILS_USER_ID);
    }

    /**
     * 根据身份信息获取用户ID
     *
     * @param claims 身份信息
     * @return 用户ID
     */
    public static String getUserId(Claims claims)
    {
        return getValue(claims, SecurityConstants.DETAILS_USER_ID);
    }

    /**
     * 根据令牌获取用户名
     *
     * @param token 令牌
     * @return 用户名
     */
    public static String getUserName(String token)
    {
        Claims claims = parseToken(token);
        return getValue(claims, SecurityConstants.DETAILS_USERNAME);
    }

    /**
     * 根据身份信息获取用户名
     *
     * @param claims 身份信息
     * @return 用户名
     */
    public static String getUserName(Claims claims)
    {
        return getValue(claims, SecurityConstants.DETAILS_USERNAME);
    }

    /**
     * 从数据声明取值
     *
     * @param claims 数据声明
     * @param key 键
     * @return 值
     */
    public static String getValue(Claims claims, String key)
    {
        return Convert.toStr(claims.get(key), "");
    }

    static String base64Url(byte[] bytes)
    {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static byte[] base64UrlDecode(String value)
    {
        return Base64.getUrlDecoder().decode(value);
    }

    static byte[] hexToBytes(String hex)
    {
        byte[] out = new byte[hex.length() / 2];
        for (int i = 0; i < out.length; i++)
        {
            out[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        return out;
    }

    static String bytesToHex(byte[] bytes)
    {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes)
        {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
