package com.scaffold.common.security.service;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.scaffold.common.core.constant.CacheConstants;
import com.scaffold.common.core.constant.SecurityConstants;
import com.scaffold.common.core.text.Convert;
import com.scaffold.common.core.utils.JwtUtils;
import com.scaffold.common.core.utils.ServletUtils;
import com.scaffold.common.core.utils.StringUtils;
import com.scaffold.common.core.utils.ip.IpUtils;
import com.scaffold.common.core.utils.uuid.IdUtils;
import com.scaffold.common.redis.service.RedisService;
import com.scaffold.common.security.utils.SecurityUtils;
import com.scaffold.system.api.model.LoginUser;

/**
 * token验证处理
 * 
 * @author ct
 */
@Component
public class TokenService
{
    private static final Logger log = LoggerFactory.getLogger(TokenService.class);

    private final RedisService redisService;

    public TokenService(RedisService redisService)
    {
        this.redisService = redisService;
    }

    protected static final long MILLIS_SECOND = 1000;

    protected static final long MILLIS_MINUTE = 60 * MILLIS_SECOND;

    private final static String ACCESS_TOKEN = CacheConstants.LOGIN_TOKEN_KEY;

    private final static Long TOKEN_REFRESH_THRESHOLD_MINUTES = CacheConstants.REFRESH_TIME * MILLIS_MINUTE;

    /** P2-3：用户在线会话索引 key 前缀（userId → 该用户全部在线会话 userkey 集合） */
    private static final String USER_SESSIONS_KEY_PREFIX = ACCESS_TOKEN + "user:";

    /** 会话索引自清理时长（小时）：会话本身到期自过期，索引残留成员无副作用 */
    private static final long USER_SESSIONS_TTL_HOURS = 12;

    /**
     * 会话超时配置键（等保，V7 种入，单位分钟）
     */
    private static final String CONFIG_SESSION_TIMEOUT = "sys.account.session.timeout";

    /**
     * 读取会话超时时间（分钟）：等保配置 sys.account.session.timeout，
     * 缺失/非法回退 CacheConstants.EXPIRATION（默认 30）。
     * <p>
     * 配置由 system 服务启动时 loadingConfigCache() 预加载至 Redis（sys_config: 前缀），
     * 配置变更时 refreshCache() 刷新缓存，因此此处直接读缓存键即可拿到最新值。
     * <p>
     * 等保三级要求令牌有效期 ≤30 分钟（CacheConstants.EXPIRATION 注释），
     * 配置值超限时按 30 生效，防止放宽合规基线。
     */
    private long getTokenExpireMinutes()
    {
        Long minutes = Convert.toLong(redisService.getCacheObject(CacheConstants.SYS_CONFIG_KEY + CONFIG_SESSION_TIMEOUT), CacheConstants.EXPIRATION);
        if (minutes == null || minutes <= 0)
        {
            return CacheConstants.EXPIRATION;
        }
        return Math.min(minutes, CacheConstants.EXPIRATION);
    }

    /**
     * 创建令牌
     */
    public Map<String, Object> createToken(LoginUser loginUser)
    {
        String token = IdUtils.fastUUID();
        Long userId = loginUser.getSysUser().getUserId();
        String userName = loginUser.getSysUser().getUserName();
        loginUser.setToken(token);
        loginUser.setUserid(userId);
        loginUser.setUsername(userName);
        loginUser.setIpaddr(IpUtils.getIpAddr());
        refreshToken(loginUser);
        indexSession(userId, token);

        // Jwt存储信息
        Map<String, Object> claimsMap = new HashMap<String, Object>();
        claimsMap.put(SecurityConstants.USER_KEY, token);
        claimsMap.put(SecurityConstants.DETAILS_USER_ID, userId);
        claimsMap.put(SecurityConstants.DETAILS_USERNAME, userName);

        // 等保：JWT exp 与 Redis 会话有效期一致（sys.account.session.timeout，默认 30 分钟）
        long expireMinutes = getTokenExpireMinutes();
        Date expireAt = new Date(System.currentTimeMillis() + expireMinutes * MILLIS_MINUTE);

        // 接口返回信息
        Map<String, Object> rspMap = new HashMap<String, Object>();
        rspMap.put("access_token", JwtUtils.createToken(claimsMap, expireAt));
        rspMap.put("expires_in", expireMinutes);
        return rspMap;
    }

    /**
     * 获取用户身份信息
     *
     * @return 用户信息
     */
    public LoginUser getLoginUser()
    {
        return getLoginUser(ServletUtils.getRequest());
    }

    /**
     * 获取用户身份信息
     *
     * @return 用户信息
     */
    public LoginUser getLoginUser(HttpServletRequest request)
    {
        // 获取请求携带的令牌
        String token = SecurityUtils.getToken(request);
        return getLoginUser(token);
    }

    /**
     * 获取用户身份信息
     *
     * @return 用户信息
     */
    public LoginUser getLoginUser(String token)
    {
        LoginUser user = null;
        try
        {
            if (StringUtils.isNotEmpty(token))
            {
                String userkey = JwtUtils.getUserKey(token);
                user = redisService.getCacheObject(getTokenKey(userkey));
                return user;
            }
        }
        catch (Exception e)
        {
            log.error("获取用户信息异常'{}'", e.getMessage());
        }
        return user;
    }

    /**
     * 设置用户身份信息
     */
    public void setLoginUser(LoginUser loginUser)
    {
        if (StringUtils.isNotNull(loginUser) && StringUtils.isNotEmpty(loginUser.getToken()))
        {
            refreshToken(loginUser);
        }
    }

    /**
     * 删除用户缓存信息（JWT 过期时容忍解析失败：过期会话同样需要清理 Redis，等保 30 分钟有效期）
     */
    public void delLoginUser(String token)
    {
        if (StringUtils.isNotEmpty(token))
        {
            try
            {
                String userkey = JwtUtils.getUserKey(token);
                redisService.deleteObject(getTokenKey(userkey));
                // 从在线会话索引移除（token 过期解析失败时跳过，索引随 TTL 自清理）
                String userId = JwtUtils.getUserId(token);
                if (StringUtils.isNotEmpty(userId))
                {
                    removeSessionIndex(Long.valueOf(userId), userkey);
                }
            }
            catch (Exception e)
            {
                log.warn("删除用户缓存异常（token 可能已过期）: {}", e.getMessage());
            }
        }
    }

    /**
     * 验证令牌有效期，相差不足120分钟，自动刷新缓存
     *
     * @param loginUser
     */
    public void verifyToken(LoginUser loginUser)
    {
        long expireTime = loginUser.getExpireTime();
        long currentTime = System.currentTimeMillis();
        if (expireTime - currentTime <= TOKEN_REFRESH_THRESHOLD_MINUTES)
        {
            refreshToken(loginUser);
        }
    }

    /**
     * 刷新令牌有效期
     *
     * @param loginUser 登录信息
     */
    public void refreshToken(LoginUser loginUser)
    {
        long expireMinutes = getTokenExpireMinutes();
        loginUser.setLoginTime(System.currentTimeMillis());
        loginUser.setExpireTime(loginUser.getLoginTime() + expireMinutes * MILLIS_MINUTE);
        // 根据uuid将loginUser缓存
        String userKey = getTokenKey(loginUser.getToken());
        redisService.setCacheObject(userKey, loginUser, expireMinutes, TimeUnit.MINUTES);
    }

    /**
     * 会话索引登记（P2-3 多设备管理）：登录成功后把 userkey 记入用户会话集合
     */
    private void indexSession(Long userId, String userKey)
    {
        try
        {
            redisService.setCacheSet(getUserSessionsKey(userId), Collections.singleton(userKey));
            redisService.expire(getUserSessionsKey(userId), USER_SESSIONS_TTL_HOURS * 3600);
        }
        catch (Exception e)
        {
            log.warn("登记会话索引异常: userId={}, {}", userId, e.getMessage());
        }
    }

    private void removeSessionIndex(Long userId, String userKey)
    {
        try
        {
            redisService.removeCacheSetValue(getUserSessionsKey(userId), userKey);
        }
        catch (Exception e)
        {
            log.warn("清理会话索引异常: userId={}, {}", userId, e.getMessage());
        }
    }

    private String getUserSessionsKey(Long userId)
    {
        return USER_SESSIONS_KEY_PREFIX + userId;
    }

    /**
     * 踢出指定会话（单设备下线，P2-3）
     *
     * @return true=会话存在且已删除
     */
    public boolean kickSession(Long userId, String userKey)
    {
        boolean removed = redisService.deleteObject(getTokenKey(userKey));
        removeSessionIndex(userId, userKey);
        return removed;
    }

    /**
     * 踢出用户全部在线会话（全设备下线/改密强制下线，P2-3）
     *
     * @return 删除的会话数
     */
    public int kickAllSessions(Long userId)
    {
        int count = 0;
        Set<String> sessions = redisService.getCacheSet(getUserSessionsKey(userId));
        if (sessions != null)
        {
            for (String userKey : sessions)
            {
                if (redisService.deleteObject(getTokenKey(userKey)))
                {
                    count++;
                }
            }
        }
        redisService.deleteObject(getUserSessionsKey(userId));
        return count;
    }

    private String getTokenKey(String token)
    {
        return ACCESS_TOKEN + token;
    }
}