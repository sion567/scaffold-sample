package com.scaffold.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.UUID;

/**
 * 全链路 TraceId 过滤器
 *
 * @author scaffold
 */
@Component
public class TraceIdFilter implements GlobalFilter, Ordered
{
    private static final Logger log = LoggerFactory.getLogger(TraceIdFilter.class);

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String TRACE_ID_KEY = "traceId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain)
    {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        // 优先取请求头中的 TraceId，没有则生成新的
        String traceId = request.getHeaders().getFirst(TRACE_ID_HEADER);
        if (traceId == null || traceId.isEmpty())
        {
            traceId = UUID.randomUUID().toString();
        }

        // 写入响应头，供前端提取
        response.getHeaders().add(TRACE_ID_HEADER, traceId);

        // 存入 exchange attributes 和 MDC（同步线程可用）
        exchange.getAttributes().put(TRACE_ID_KEY, traceId);
        MDC.put(TRACE_ID_KEY, traceId);

        // 存入 ThreadLocal（响应式线程切换后仍可获取）
        TraceIdHolder.set(traceId);

        log.debug("[TraceId:{}] {}", traceId, request.getPath());

        // 将 traceId 写入 Reactor Context，供后续 Operator 使用；请求结束后清理线程变量
        return chain.filter(exchange)
                .contextWrite(Context.of(TRACE_ID_KEY, traceId))
                .doFinally(signal -> {
                    MDC.remove(TRACE_ID_KEY);
                    TraceIdHolder.remove();
                });
    }

    @Override
    public int getOrder()
    {
        // 最早执行，在鉴权等过滤器之前
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
