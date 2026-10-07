package com.scaffold.gateway.filter;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.CompositeConverter;
import org.slf4j.MDC;

/**
 * Logback 转换器：从 MDC / Reactor Context 提取 traceId
 *
 * @author scaffold
 */
public class TraceIdConverter extends CompositeConverter<ILoggingEvent>
{
    @Override
    protected String transform(ILoggingEvent event, String in)
    {
        // 优先取 MDC（同步线程）
        String traceId = MDC.get(TraceIdFilter.TRACE_ID_KEY);
        if (traceId != null && !traceId.isEmpty())
        {
            return traceId;
        }
        // 降级取 ThreadLocal（WebFlux 切换线程后仍可用）
        return TraceIdHolder.get();
    }
}
