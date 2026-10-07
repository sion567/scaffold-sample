package com.scaffold.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import com.scaffold.common.core.constant.SecurityConstants;
import com.scaffold.common.core.crypto.GatewaySm3Signature;
import reactor.core.publisher.Mono;

/**
 * 入站请求头清洗：所有请求（含白名单路径）剥除伪造的网关签名头与内部来源标识，
 * 保证下游看到的 X-Gateway-* 必然出自本网关。
 * AuthFilter 原先只在鉴权分支剥 from-source，白名单路径存在透传缺口，这里统一堵上。
 *
 * @author scaffold
 */
@Component
public class GatewayHeaderCleanFilter implements GlobalFilter, Ordered
{
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain)
    {
        ServerHttpRequest cleaned = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(GatewaySm3Signature.HEADER_TIMESTAMP);
                    headers.remove(GatewaySm3Signature.HEADER_NONCE);
                    headers.remove(GatewaySm3Signature.HEADER_SIGN);
                    headers.remove(SecurityConstants.FROM_SOURCE);
                })
                .build();
        return chain.filter(exchange.mutate().request(cleaned).build());
    }

    @Override
    public int getOrder()
    {
        // 早于 AuthFilter(-200) 与 GatewaySignFilter(-190)
        return -210;
    }
}
