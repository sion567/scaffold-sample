package com.scaffold.common.security.utils;

import com.scaffold.common.security.crypto.Sm3PasswordEncoder;
import com.scaffold.common.core.constant.SecurityConstants;
import com.scaffold.common.core.constant.TokenConstants;
import com.scaffold.common.core.constant.UserConstants;
import com.scaffold.common.core.context.SecurityContextHolder;
import com.scaffold.common.core.utils.ServletUtils;
import com.scaffold.common.core.utils.StringUtils;
import com.scaffold.system.api.model.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * 权限获取工具类
 *
 * @author ct
 */
public class SecurityUtils
{
    /**
     * 获取用户ID
     */
    public static Long getUserId()
    {
        return SecurityContextHolder.getUserId();
    }

    /**
     * 获取用户名称
     */
    public static String getUsername()
    {
        return SecurityContextHolder.getUserName();
    }

    /**
     * 获取用户key
     */
    public static String getUserKey()
    {
        return SecurityContextHolder.getUserKey();
    }

    /**
     * 获取登录用户信息
     */
    public static LoginUser getLoginUser()
    {
        return SecurityContextHolder.get(SecurityConstants.LOGIN_USER, LoginUser.class);
    }

    /**
     * 获取请求token
     */
    public static String getToken()
    {
        return getToken(ServletUtils.getRequest());
    }

    /**
     * 根据request获取请求token
     */
    public static String getToken(HttpServletRequest request)
    {
        // 从header获取token标识
        String token = request.getHeader(SecurityConstants.AUTHORIZATION_HEADER);
        return replaceTokenPrefix(token);
    }

    /**
     * 裁剪token前缀
     */
    public static String replaceTokenPrefix(String token)
    {
        // 如果前端设置了令牌前缀，则裁剪掉前缀
        if (StringUtils.isNotEmpty(token) && token.startsWith(TokenConstants.PREFIX))
        {
            token = token.replaceFirst(TokenConstants.PREFIX, "");
        }
        return token;
    }

    /**
     * 是否为管理员
     *
     * @return 结果
     */
    public static boolean isAdmin()
    {
        return isAdmin(getUserId());
    }

    /**
     * 是否为管理员
     *
     * @param userId 用户ID
     * @return 结果
     */
    public static boolean isAdmin(Long userId)
    {
        return UserConstants.isAdmin(userId);
    }

    /**
     * 获取会话ID
     */
    public static String getSessionId()
    {
        HttpSession session = ServletUtils.getRequest() != null
            ? ServletUtils.getRequest().getSession(false) : null;
        return session != null ? session.getId() : "";
    }

    /**
     * 获取当前 HttpServletRequest（可进一步获取 IP、Header 等）
     */
    public static HttpServletRequest getRequest()
    {
        return ServletUtils.getRequest();
    }

    /**
     * 生成密码哈希：SM3 加盐迭代（格式 salt$hash，见 Sm3PasswordEncoder）。
     * 空/ null 密码抛 IllegalArgumentException，防止静默落库空口令。
     *
     * @param password 密码
     * @return 加密字符串
     */
    public static String encryptPassword(String password)
    {
        if (StringUtils.isEmpty(password))
        {
            throw new IllegalArgumentException("密码不能为空");
        }
        return Sm3PasswordEncoder.encodePassword(password);
    }

    /**
     * 判断密码是否相同（SM3 salt$hash，与 {@link #encryptPassword} 配对）
     *
     * @param rawPassword 真实密码
     * @param encodedPassword 加密后字符
     * @return 结果
     */
    public static boolean matchesPassword(String rawPassword, String encodedPassword)
    {
        return new Sm3PasswordEncoder().matches(rawPassword, encodedPassword);
    }
}
