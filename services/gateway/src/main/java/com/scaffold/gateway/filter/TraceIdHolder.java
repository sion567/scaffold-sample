package com.scaffold.gateway.filter;

/**
 * TraceId ThreadLocal 持有者
 * 用于 WebFlux 响应式线程切换时仍能获取 traceId
 *
 * @author scaffold
 */
public class TraceIdHolder
{
    private static final ThreadLocal<String> TRACE_ID_HOLDER = new ThreadLocal<>();

    public static void set(String traceId)
    {
        TRACE_ID_HOLDER.set(traceId);
    }

    public static String get()
    {
        return TRACE_ID_HOLDER.get();
    }

    public static void remove()
    {
        TRACE_ID_HOLDER.remove();
    }
}
