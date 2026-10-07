package com.scaffold.common.trace;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * traceId 上下文（对齐网关 TraceIdFilter：header X-Trace-Id、MDC key traceId）。
 * <p>
 * 传播链路：网关生成 X-Trace-Id → 服务进站由 TraceDubboProviderFilter 绑定 MDC
 * （来源优先级：Dubbo attachment > Triple REST 入站头 > 新生成）→
 * 服务出站调用由 TraceDubboConsumerFilter 写 attachment → 日志 pattern 配 %X{traceId} 即可串联全链路。
 *
 * @author ct
 */
public final class TraceContext
{
    /** Dubbo attachment key（服务间传播） */
    public static final String ATTACHMENT_KEY = "scaffold_trace_id";

    /** 网关透传的请求头名（与 scaffold-gateway TraceIdFilter.HEADER_ID_HEADER 一致） */
    public static final String HEADER = "X-Trace-Id";

    /** MDC key（与网关一致，logback pattern 用 %X{traceId} 输出） */
    public static final String MDC_KEY = "traceId";

    private TraceContext()
    {
    }

    public static String current()
    {
        return MDC.get(MDC_KEY);
    }

    public static void bind(String traceId)
    {
        MDC.put(MDC_KEY, traceId);
    }

    public static void clear()
    {
        MDC.remove(MDC_KEY);
    }

    /**
     * 入站绑定：inbound 非空用 inbound，否则新生成；返回实际生效的 traceId
     */
    public static String bindOrNew(String inbound)
    {
        String traceId = inbound != null && !inbound.isEmpty() ? inbound : newTraceId();
        bind(traceId);
        return traceId;
    }

    public static String newTraceId()
    {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
