package com.scaffold.gateway.filter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import com.scaffold.common.core.constant.SecurityConstants;
import com.scaffold.common.core.crypto.GatewaySm3Signature;
import com.scaffold.common.core.utils.StringUtils;
import reactor.core.publisher.Mono;

/**
 * 网关身份头签名签发：在 AuthFilter(-200) 鉴权并透传身份头之后执行，
 * 对 身份头 + timestamp + nonce 追加 HMAC-SM3 签名（X-Gateway-Ts/Nonce/Sign），
 * 下游服务由 GatewaySignVerifyFilter（Dubbo 提供端 Filter）验签。
 * <p>
 * 密钥 scaffold.gateway.internal.secret 与下游共用（Nacos/环境变量下发）：
 * 为空时 dev/test/local 环境告警并停用签名，其余环境拒绝启动（fail-fast）。
 *
 * @author scaffold
 */
@Component
public class GatewaySignFilter implements GlobalFilter, Ordered
{
    private static final Logger log = LoggerFactory.getLogger(GatewaySignFilter.class);

    private final String secret;

    private final boolean enabled;

    public GatewaySignFilter(@Value("${scaffold.gateway.internal.secret:}") String secret, Environment environment)
    {
        this.secret = secret;
        this.enabled = StringUtils.isNotEmpty(secret);
        if (!enabled)
        {
            boolean devLike = Stream.of(environment.getActiveProfiles())
                    .anyMatch(p -> p.contains("dev") || p.contains("test") || p.contains("local"));
            if (devLike)
            {
                log.warn("[网关签名] 未配置 scaffold.gateway.internal.secret，本实例不签发身份头签名"
                        + "（生产环境缺该配置将拒绝启动；下游验签建议保持 alert 模式灰度）");
            }
            else
            {
                throw new IllegalStateException("缺少 scaffold.gateway.internal.secret 配置（网关身份头签名密钥），拒绝启动");
            }
        }
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain)
    {
        if (!enabled)
        {
            return chain.filter(exchange);
        }
        ServerHttpRequest request = exchange.getRequest();
        HttpHeaders headers = request.getHeaders();
        Map<String, String> identity = new HashMap<>();
        putIfNotEmpty(identity, SecurityConstants.DETAILS_USER_ID, headers.getFirst(SecurityConstants.DETAILS_USER_ID));
        putIfNotEmpty(identity, SecurityConstants.USER_KEY, headers.getFirst(SecurityConstants.USER_KEY));
        putIfNotEmpty(identity, SecurityConstants.DETAILS_USERNAME, headers.getFirst(SecurityConstants.DETAILS_USERNAME));

        String timestamp = Long.toString(System.currentTimeMillis());
        String nonce = UUID.randomUUID().toString().replace("-", "");
        String sign = GatewaySm3Signature.sign(secret, identity, timestamp, nonce);

        ServerHttpRequest signed = request.mutate()
                .header(GatewaySm3Signature.HEADER_TIMESTAMP, timestamp)
                .header(GatewaySm3Signature.HEADER_NONCE, nonce)
                .header(GatewaySm3Signature.HEADER_SIGN, sign)
                .build();
        return chain.filter(exchange.mutate().request(signed).build());
    }

    private void putIfNotEmpty(Map<String, String> map, String key, String value)
    {
        if (StringUtils.isNotEmpty(value))
        {
            map.put(key, value);
        }
    }

    @Override
    public int getOrder()
    {
        // 在 AuthFilter(-200) 之后执行，才能读到透传的身份头
        return -190;
    }
}
