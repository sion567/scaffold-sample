package com.scaffold.common.security.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.apache.dubbo.rpc.RpcContext;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import jakarta.servlet.http.HttpServletRequest;
import com.scaffold.common.core.constant.SecurityConstants;
import com.scaffold.common.core.exception.InnerAuthException;
import com.scaffold.common.core.utils.ServletUtils;
import com.scaffold.common.core.utils.StringUtils;
import com.scaffold.common.security.annotation.InnerAuth;

/**
 * 内部服务调用验证处理
 * 
 * @author ct
 */
@Aspect
@Component
public class InnerAuthAspect implements Ordered
{
    @Around("@annotation(innerAuth)")
    public Object innerAround(ProceedingJoinPoint point, InnerAuth innerAuth) throws Throwable
    {
        HttpServletRequest request = ServletUtils.getRequest();
        String source;
        String userid;
        String username;
        if (StringUtils.isNotNull(request))
        {
            source = ServletUtils.getHeader(request, SecurityConstants.FROM_SOURCE);
            userid = ServletUtils.getHeader(request, SecurityConstants.DETAILS_USER_ID);
            username = ServletUtils.getHeader(request, SecurityConstants.DETAILS_USERNAME);
        }
        else
        {
            // Dubbo 内部调用时从 RpcContext 附件中获取
            RpcContext rpcContext = RpcContext.getServerAttachment();
            source = rpcContext.getAttachment(SecurityConstants.FROM_SOURCE);
            userid = rpcContext.getAttachment(SecurityConstants.DETAILS_USER_ID);
            username = rpcContext.getAttachment(SecurityConstants.DETAILS_USERNAME);
            // 附件未传递时回退到方法参数中的请求来源
            if (StringUtils.isEmpty(source))
            {
                for (Object arg : point.getArgs())
                {
                    if (SecurityConstants.INNER.equals(arg))
                    {
                        source = SecurityConstants.INNER;
                        break;
                    }
                }
            }
        }
        // 内部请求验证
        if (!StringUtils.equals(SecurityConstants.INNER, source))
        {
            throw new InnerAuthException("没有内部访问权限，不允许访问");
        }
        // 用户信息验证
        if (innerAuth.isUser() && (StringUtils.isEmpty(userid) || StringUtils.isEmpty(username)))
        {
            throw new InnerAuthException("没有设置用户信息，不允许访问 ");
        }
        return point.proceed();
    }

    /**
     * 确保在权限认证aop执行前执行
     */
    @Override
    public int getOrder()
    {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
