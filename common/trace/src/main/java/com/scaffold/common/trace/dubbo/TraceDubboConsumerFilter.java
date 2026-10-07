package com.scaffold.common.trace.dubbo;

import com.scaffold.common.trace.TraceContext;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;

/**
 * Dubbo 消费端过滤器：出站调用前把 traceId 写入 attachment（P1-7）。
 * traceId 来源：当前线程 MDC（由提供端 TraceDubboProviderFilter 绑定）；本线程没有则新生成并绑定，
 * 保证异步/线程池场景也不丢。
 *
 * @author ct
 */
@Activate(group = CommonConstants.CONSUMER, order = -10000)
public class TraceDubboConsumerFilter implements Filter
{
    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException
    {
        String traceId = TraceContext.current();
        if (traceId == null || traceId.isEmpty())
        {
            traceId = TraceContext.bindOrNew(null);
        }
        RpcContext.getClientAttachment().setAttachment(TraceContext.ATTACHMENT_KEY, traceId);
        return invoker.invoke(invocation);
    }
}
