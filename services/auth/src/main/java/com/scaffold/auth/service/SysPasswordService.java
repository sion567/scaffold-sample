package com.scaffold.auth.service;

import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;
import com.scaffold.common.core.constant.CacheConstants;
import com.scaffold.common.core.constant.Constants;
import com.scaffold.common.core.exception.ServiceException;
import com.scaffold.common.core.text.Convert;
import com.scaffold.common.redis.service.RedisService;
import com.scaffold.common.security.utils.SecurityUtils;
import com.scaffold.system.api.domain.SysUser;

/**
 * 登录密码方法
 *
 * @author scaffold
 */
@Component
public class SysPasswordService
{
    private final RedisService redisService;
    private final SysRecordLogService recordLogService;

    public SysPasswordService(RedisService redisService, SysRecordLogService recordLogService)
    {
        this.redisService = redisService;
        this.recordLogService = recordLogService;
    }

    /**
     * 登录失败最大次数配置键（合规基线项）
     */
    private static final String CONFIG_MAX_RETRY = "sys.account.login.maxRetry";

    /**
     * 密码锁定时间配置键（合规基线项）
     */
    private static final String CONFIG_LOCK_TIME = "sys.account.login.lockTime";

    /**
     * 读取登录失败最大次数（次），缺失/非法回退 CacheConstants 默认 5。
     * 合规基线要求连续失败超过 5 次即锁定，配置值超限时按 5 生效，防止放宽合规基线。
     */
    private int getMaxRetryCount()
    {
        Integer v = Convert.toInt(redisService.getCacheObject(CacheConstants.SYS_CONFIG_KEY + CONFIG_MAX_RETRY), CacheConstants.PASSWORD_MAX_RETRY_COUNT);
        if (v == null || v <= 0)
        {
            return CacheConstants.PASSWORD_MAX_RETRY_COUNT;
        }
        return Math.min(v, CacheConstants.PASSWORD_MAX_RETRY_COUNT);
    }

    /**
     * 读取密码锁定时间（分钟），缺失/非法回退 CacheConstants 默认 30。
     * 合规基线要求锁定时间不少于 30 分钟，配置值低于基线时按 30 生效，防止放宽合规基线。
     */
    private long getLockTime()
    {
        Long v = Convert.toLong(redisService.getCacheObject(CacheConstants.SYS_CONFIG_KEY + CONFIG_LOCK_TIME), CacheConstants.PASSWORD_LOCK_TIME);
        if (v == null || v <= 0)
        {
            return CacheConstants.PASSWORD_LOCK_TIME;
        }
        return Math.max(v, CacheConstants.PASSWORD_LOCK_TIME);
    }

    /**
     * 登录账户密码错误次数缓存键名
     *
     * @param username 用户名
     * @return 缓存键key
     */
    private String getCacheKey(String username)
    {
        return CacheConstants.PWD_ERR_CNT_KEY + username;
    }

    public void validate(SysUser user, String password)
    {
        String username = user.getUserName();

        // 动态读取合规基线配置（sys.account.login.maxRetry / sys.account.login.lockTime），缺失回退 5/30
        int maxRetryCount = getMaxRetryCount();
        long lockTime = getLockTime();

        Integer retryCount = redisService.getCacheObject(getCacheKey(username));

        if (retryCount == null)
        {
            retryCount = 0;
        }

        if (retryCount >= maxRetryCount)
        {
            String errMsg = String.format("密码输入错误%s次，帐户锁定%s分钟", maxRetryCount, lockTime);
            recordLogService.recordLogininfor(username, Constants.LOGIN_FAIL,errMsg);
            throw new ServiceException(errMsg);
        }

        if (!matches(user, password))
        {
            retryCount = retryCount + 1;
            recordLogService.recordLogininfor(username, Constants.LOGIN_FAIL, String.format("密码输入错误%s次", retryCount));
            redisService.setCacheObject(getCacheKey(username), retryCount, lockTime, TimeUnit.MINUTES);
            throw new ServiceException("用户不存在/密码错误");
        }
        else
        {
            clearLoginRecordCache(username);
        }
    }

    public boolean matches(SysUser user, String rawPassword)
    {
        return SecurityUtils.matchesPassword(rawPassword, user.getPassword());
    }

    public void clearLoginRecordCache(String loginName)
    {
        if (redisService.hasKey(getCacheKey(loginName)))
        {
            redisService.deleteObject(getCacheKey(loginName));
        }
    }
}