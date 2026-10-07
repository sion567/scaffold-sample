package com.scaffold.sample.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import com.scaffold.common.core.constant.SecurityConstants;
import com.scaffold.common.core.context.SecurityContextHolder;
import com.scaffold.common.core.utils.ServletUtils;
import com.scaffold.common.core.utils.StringUtils;
import com.scaffold.common.security.auth.AuthUtil;
import com.scaffold.common.security.utils.SecurityUtils;
import com.scaffold.system.api.model.LoginUser;

/**
 * 网关身份头解析拦截器（MVC 版，逻辑对齐 Dubbo 侧 SecurityContextFilter）
 *
 * 从网关透传的请求头还原登录用户：userid/username/userkey 写入线程上下文，
 * Authorization 头对应 token 在 Redis 校验并续期，完整 LoginUser 供权限注解使用。
 *
 * @author scaffold
 */
@Component
public class HeaderAuthInterceptor implements HandlerInterceptor
{
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
    {
        SecurityContextHolder.setUserId(request.getHeader(SecurityConstants.DETAILS_USER_ID));
        String usernameHeader = request.getHeader(SecurityConstants.DETAILS_USERNAME);
        SecurityContextHolder.setUserName(StringUtils.isNotEmpty(usernameHeader)
                ? ServletUtils.urlDecode(usernameHeader) : usernameHeader);
        SecurityContextHolder.setUserKey(request.getHeader(SecurityConstants.USER_KEY));

        String token = SecurityUtils.replaceTokenPrefix(request.getHeader(SecurityConstants.AUTHORIZATION_HEADER));
        if (StringUtils.isNotEmpty(token))
        {
            LoginUser loginUser = AuthUtil.getLoginUser(token);
            if (StringUtils.isNotNull(loginUser))
            {
                AuthUtil.verifyLoginUserExpire(loginUser);
                SecurityContextHolder.set(SecurityConstants.LOGIN_USER, loginUser);
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
    {
        SecurityContextHolder.remove();
    }
}
