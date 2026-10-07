package com.scaffold.common.trace.dubbo;

import com.scaffold.common.trace.TraceContext;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;

/**
 * Dubbo 提供端过滤器：进站请求绑定 traceId 到 MDC（P1-7）。
 * <p>
 * 来源优先级：Dubbo attachment（服务间调用）> Triple REST 入站头 X-Trace-Id（网关透传，
 * servlet/http12 两种承载都会被 Dubbo 桥接为同一 HttpRequest）> 新生成（兜底，服务内日志也有 traceId）。
 * order=-11000，最先进站绑定，SecurityContext/业务/异常日志全部带 traceId。
 *
 * @author ct
 */
@Activate(group = CommonConstants.PROVIDER, order = -11000)
public class TraceDubboProviderFilter implements Filter
{
    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException
    {
        boolean bound = false;
        if (TraceContext.current() == null || TraceContext.current().isEmpty())
        {
            String inbound = RpcContext.getServerAttachment().getAttachment(TraceContext.ATTACHMENT_KEY);
            if (inbound == null || inbound.isEmpty())
            {
                HttpRequest request = RpcContext.getServiceContext().getRequest(HttpRequest.class);
                if (request != null)
                {
                    inbound = request.header(TraceContext.HEADER);
                }
            }
            TraceContext.bindOrNew(inbound);
            bound = true;
        }
        try
        {
            return invoker.invoke(invocation);
        }
        finally
        {
            if (bound)
            {
                TraceContext.clear();
            }
        }
    }
}
