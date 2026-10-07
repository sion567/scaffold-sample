package com.scaffold.common.security.gateway;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.AntPathMatcher;
import com.scaffold.common.core.crypto.GatewaySm3Signature;
import com.scaffold.common.redis.service.RedisService;

/**
 * 网关签名验签器（下游服务侧，Spring Bean，由 AutoConfiguration.imports 注册；
 * Dubbo 提供端过滤器 GatewaySignVerifyFilter 通过 SpringUtils 取用本类）。
 * <p>
 * 策略约定（nonce 与 auth 侧 RedisNonceCache 保持一致）：
 * - 签名校验 fail-closed：验签失败按 mode 处理（alert=告警放行 / block=拒绝）；
 * - nonce 防重放 fail-open：Redis 异常放行并告警，宁短暂容忍重放窗口不阻断业务；
 * - 未携带签名头的匿名请求（网关白名单、actuator、内网工具直连）不拦截；
 *   但"携带身份头却无有效签名"的请求按伪造处理——这是本机制防御的核心。
 *
 * @author ct
 */
public class GatewaySignVerifier
{
    private static final Logger log = LoggerFactory.getLogger(GatewaySignVerifier.class);

    private static final String NONCE_KEY_PREFIX = "gw:sign:nonce:";

    /** 验签结果 */
    public enum Result
    {
        /** 校验通过 */
        OK,
        /** 签名无效/时间戳越窗 */
        INVALID,
        /** nonce 重放 */
        REPLAY
    }

    private final String secret;

    private final String mode;

    private final long timestampWindowSeconds;

    private final List<String> skipPaths;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private final RedisService redisService;

    public GatewaySignVerifier(@Value("${scaffold.gateway.internal.secret:}") String secret,
                               @Value("${scaffold.gateway-sign.mode:alert}") String mode,
                               @Value("${scaffold.gateway-sign.timestamp-window-seconds:300}") long timestampWindowSeconds,
                               @Value("${scaffold.gateway-sign.skip-paths:/actuator/**,/v3/api-docs/**}") String skipPaths,
                               RedisService redisService)
    {
        this.secret = secret;
        this.mode = mode;
        this.timestampWindowSeconds = timestampWindowSeconds;
        this.skipPaths = Arrays.asList(skipPaths.split(","));
        this.redisService = redisService;
    }

    public boolean isEnabled()
    {
        return secret != null && !secret.isEmpty() && !"off".equalsIgnoreCase(mode);
    }

    public boolean isBlockMode()
    {
        return "block".equalsIgnoreCase(mode);
    }

    public boolean isSkipPath(String path)
    {
        for (String pattern : skipPaths)
        {
            String p = pattern.trim();
            if (!p.isEmpty() && pathMatcher.match(p, path))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * 验签：签名常量时间比对 → 时间戳窗口 → nonce 防重放（Redis 异常 fail-open）
     */
    public Result verify(Map<String, String> identityParams, String timestamp, String nonce, String sign)
    {
        if (!GatewaySm3Signature.verify(secret, identityParams, timestamp, nonce, sign))
        {
            return Result.INVALID;
        }
        long ts;
        try
        {
            ts = Long.parseLong(timestamp);
        }
        catch (NumberFormatException e)
        {
            return Result.INVALID;
        }
        if (Math.abs(System.currentTimeMillis() - ts) > timestampWindowSeconds * 1000L)
        {
            return Result.INVALID;
        }
        try
        {
            boolean first = redisService.setIfAbsent(NONCE_KEY_PREFIX + nonce, "1",
                    timestampWindowSeconds * 2, TimeUnit.SECONDS);
            if (!first)
            {
                return Result.REPLAY;
            }
        }
        catch (Exception e)
        {
            log.warn("[网关验签] nonce 缓存异常，fail-open: {}", e.getMessage());
        }
        return Result.OK;
    }
}
