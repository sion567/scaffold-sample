package com.scaffold.common.security.dubbo;

import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import com.scaffold.common.core.constant.SecurityConstants;
import com.scaffold.common.core.context.SecurityContextHolder;
import com.scaffold.common.core.utils.ServletUtils;
import com.scaffold.common.core.utils.StringUtils;

/**
 * Dubbo 消费端过滤器，传递用户信息，防止丢失
 *
 * 服务间 Dubbo 调用均视为内部调用（from-source=inner），@InnerAuth 接口凭此放行；
 * 外部请求无法伪造 RpcContext 附件，安全性由网关 Header 剥离 + 本过滤器保证。
 * 用户信息优先取当前 HTTP 请求头（网关透传），无 HTTP 上下文时（二跳调用、@Async）
 * 回退到 SecurityContextHolder 线程变量。
 *
 * @author scaffold
 */
@Activate(group = CommonConstants.CONSUMER)
public class DubboRequestInterceptor implements Filter
{
    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException
    {
        HttpServletRequest httpServletRequest = ServletUtils.getRequest();
        if (StringUtils.isNotNull(httpServletRequest))
        {
            Map<String, String> headers = ServletUtils.getHeaders(httpServletRequest);
            RpcContext.getClientAttachment().setAttachment(SecurityConstants.DETAILS_USER_ID,
                    headers.get(SecurityConstants.DETAILS_USER_ID));
            RpcContext.getClientAttachment().setAttachment(SecurityConstants.USER_KEY,
                    headers.get(SecurityConstants.USER_KEY));
            RpcContext.getClientAttachment().setAttachment(SecurityConstants.DETAILS_USERNAME,
                    headers.get(SecurityConstants.DETAILS_USERNAME));
            RpcContext.getClientAttachment().setAttachment(SecurityConstants.AUTHORIZATION_HEADER,
                    headers.get(SecurityConstants.AUTHORIZATION_HEADER));
        }
        else
        {
            // 无 HTTP 上下文（服务二跳 / @Async）：从线程变量回填，避免用户信息丢失
            if (SecurityContextHolder.getUserId() != null)
            {
                RpcContext.getClientAttachment().setAttachment(SecurityConstants.DETAILS_USER_ID,
                        String.valueOf(SecurityContextHolder.getUserId()));
            }
            if (StringUtils.isNotEmpty(SecurityContextHolder.getUserName()))
            {
                RpcContext.getClientAttachment().setAttachment(SecurityConstants.DETAILS_USERNAME,
                        SecurityContextHolder.getUserName());
            }
            if (StringUtils.isNotEmpty(SecurityContextHolder.getUserKey()))
            {
                RpcContext.getClientAttachment().setAttachment(SecurityConstants.USER_KEY,
                        SecurityContextHolder.getUserKey());
            }
        }
        // 服务间调用标记为内部来源，供 @InnerAuth 校验
        RpcContext.getClientAttachment().setAttachment(SecurityConstants.FROM_SOURCE, SecurityConstants.INNER);
        return invoker.invoke(invocation);
    }
}
