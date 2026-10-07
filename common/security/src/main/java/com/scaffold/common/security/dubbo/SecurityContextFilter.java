package com.scaffold.common.security.dubbo;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.scaffold.common.core.constant.CacheConstants;
import com.scaffold.common.core.constant.SecurityConstants;
import com.scaffold.common.core.context.SecurityContextHolder;
import com.scaffold.common.core.utils.ServletUtils;
import com.scaffold.common.core.utils.StringUtils;
import com.scaffold.common.core.utils.SpringUtils;
import com.scaffold.common.redis.service.RedisService;
import com.scaffold.common.security.auth.AuthUtil;
import com.scaffold.common.security.utils.SecurityUtils;
import com.scaffold.system.api.model.LoginUser;

/**
 * Dubbo 提供端过滤器
 * 将网关透传的用户信息封装到线程变量中方便获取，同时验证当前用户有效期并自动续期
 *
 * 两种入口：
 * 1. Triple REST（网关直调）：从 HTTP 请求头读取（网关对中文用户名做过 urlEncode，此处解码）；
 * 2. 纯 dubbo 协议（服务间调用）：从 RpcContext 附件读取（由消费端 DubboRequestInterceptor 写入），
 *    并按 userKey 从 Redis 反查登录态重建 LOGIN_USER。
 *
 * @author scaffold
 */
@Activate(group = CommonConstants.PROVIDER)
public class SecurityContextFilter implements Filter
{
    private static final Logger log = LoggerFactory.getLogger(SecurityContextFilter.class);

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException
    {
        HttpRequest request = RpcContext.getServiceContext().getRequest(HttpRequest.class);
        if (StringUtils.isNotNull(request))
        {
            SecurityContextHolder.setUserId(request.header(SecurityConstants.DETAILS_USER_ID));
            String usernameHeader = request.header(SecurityConstants.DETAILS_USERNAME);
            SecurityContextHolder.setUserName(StringUtils.isNotEmpty(usernameHeader)
                    ? ServletUtils.urlDecode(usernameHeader) : usernameHeader);
            SecurityContextHolder.setUserKey(request.header(SecurityConstants.USER_KEY));

            String token = SecurityUtils.replaceTokenPrefix(request.header(SecurityConstants.AUTHORIZATION_HEADER));
            if (StringUtils.isNotEmpty(token))
            {
                LoginUser loginUser = AuthUtil.getLoginUser(token);
                if (StringUtils.isNotNull(loginUser))
                {
                    AuthUtil.verifyLoginUserExpire(loginUser);
                    SecurityContextHolder.set(SecurityConstants.LOGIN_USER, loginUser);
                }
                else
                {
                    // TokenService 已吞掉底层异常，这里补一条入口侧告警：
                    // 常见原因是 token 无效/已过期，或本服务漏配 jwt.sm2.public-key（验签公钥）
                    log.warn("Triple 入口携带 token 但登录态解析失败（token 无效/已过期，或本服务未配置"
                            + " jwt.sm2.public-key），本次调用将无 LOGIN_USER 上下文，业务侧将视为未登录");
                }
            }
        }
        else
        {
            // 纯 dubbo 协议调用：从附件恢复上下文
            RpcContext rpcContext = RpcContext.getServerAttachment();
            String userId = rpcContext.getAttachment(SecurityConstants.DETAILS_USER_ID);
            String username = rpcContext.getAttachment(SecurityConstants.DETAILS_USERNAME);
            String userKey = rpcContext.getAttachment(SecurityConstants.USER_KEY);
            if (StringUtils.isNotEmpty(userId))
            {
                SecurityContextHolder.setUserId(userId);
            }
            if (StringUtils.isNotEmpty(username))
            {
                SecurityContextHolder.setUserName(ServletUtils.urlDecode(username));
            }
            if (StringUtils.isNotEmpty(userKey))
            {
                SecurityContextHolder.setUserKey(userKey);
                restoreLoginUser(userKey);
            }
        }
        try
        {
            return invoker.invoke(invocation);
        }
        finally
        {
            SecurityContextHolder.remove();
        }
    }

    /**
     * 按 userKey 从 Redis 反查登录态（login_tokens:{userKey}，与 TokenService 存取同源），
     * 重建 LOGIN_USER，使 provider 侧 SecurityUtils.getLoginUser() 与 REST 入口行为一致。
     * 查不到（会话过期/被踢/INNER 无登录态）保持为空，由业务侧判空，不阻断内部调用；
     * Redis 异常同样降级为无登录态，只记 warn（登录态缺失不应放大为调用失败）。
     */
    private void restoreLoginUser(String userKey)
    {
        try
        {
            LoginUser loginUser = SpringUtils.getBean(RedisService.class)
                    .getCacheObject(CacheConstants.LOGIN_TOKEN_KEY + userKey);
            if (StringUtils.isNotNull(loginUser))
            {
                SecurityContextHolder.set(SecurityConstants.LOGIN_USER, loginUser);
            }
        }
        catch (Exception e)
        {
            log.warn("dubbo 调用按 userKey 反查登录态失败，provider 侧将无 LOGIN_USER 上下文: {}", e.getMessage());
        }
    }
}
