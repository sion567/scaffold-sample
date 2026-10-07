package com.scaffold.auth.service;

import java.security.SecureRandom;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.scaffold.auth.config.TwoFactorProperties;
import com.scaffold.common.core.utils.StringUtils;
import com.scaffold.common.redis.service.RedisService;
import com.scaffold.system.api.model.LoginUser;

/**
 * 双因子认证服务（可选的合规增强项）。
 *
 * 策略由 {@code scaffold.auth.two-factor.*} 配置驱动（见
 * {@link com.scaffold.auth.config.TwoFactorProperties}）：
 * enabled=true 且登录用户角色命中 required-roles / required-role-prefixes 时，
 * 登录必须通过短信验证码（或 OTP）。验证码存 Redis，5 分钟有效，一次性使用。
 *
 * 安全措施：
 * - 验证码用 SecureRandom 生成
 * - 发送频率限制（60 秒冷却）
 * - 校验失败 5 次锁定验证码
 * - 生产环境不返回/不打印验证码（短信通道接入点 {@link #sendOtp}）
 *
 * @author scaffold
 */
@Component
public class TwoFactorService
{
    private static final Logger log = LoggerFactory.getLogger(TwoFactorService.class);

    private static final String KEY_PREFIX = "two_factor:code:";

    private static final String FAIL_CNT_PREFIX = "two_factor:fail:";

    private static final long EXPIRE_MINUTES = 5;

    private static final long RESEND_COOLDOWN_SECONDS = 60;

    private static final int MAX_VERIFY_FAIL = 5;

    private final RedisService redisService;

    private final TwoFactorProperties properties;

    private final String activeProfile;

    public TwoFactorService(RedisService redisService,
                             TwoFactorProperties properties,
                             @Value("${spring.profiles.active:dev}") String activeProfile)
    {
        this.redisService = redisService;
        this.properties = properties;
        this.activeProfile = activeProfile;
    }

    private final SecureRandom secureRandom = new SecureRandom();

    public boolean isEnabled()
    {
        return properties.isEnabled();
    }

    /**
     * 是否需要双因子：开关开启 且 用户角色命中配置的角色/角色前缀
     */
    public boolean isRequired(LoginUser loginUser)
    {
        if (!properties.isEnabled() || loginUser == null)
        {
            return false;
        }
        Set<String> roles = loginUser.getRoles();
        if (roles == null)
        {
            return false;
        }
        for (String role : roles)
        {
            if (StringUtils.isEmpty(role))
            {
                continue;
            }
            if (properties.getRequiredRoles().contains(role))
            {
                return true;
            }
            for (String prefix : properties.getRequiredRolePrefixes())
            {
                if (StringUtils.isNotEmpty(prefix) && role.startsWith(prefix))
                {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 生成并发送验证码（60 秒冷却）
     * 开发/联调环境返回验证码便于测试；生产环境接短信通道后返回 null（不泄露）
     */
    public String sendOtp(String username)
    {
        if (StringUtils.isBlank(username))
        {
            throw new com.scaffold.common.core.exception.ServiceException("用户名不能为空");
        }
        // 发送频率限制（60 秒）
        String cooldownKey = KEY_PREFIX + username + ":cooldown";
        if (redisService.getCacheObject(cooldownKey) != null)
        {
            throw new com.scaffold.common.core.exception.ServiceException("验证码发送过于频繁，请稍后再试");
        }
        String code = String.valueOf(100000 + secureRandom.nextInt(900000));
        redisService.setCacheObject(KEY_PREFIX + username, code, EXPIRE_MINUTES, TimeUnit.MINUTES);
        redisService.setCacheObject(cooldownKey, "1", RESEND_COOLDOWN_SECONDS, TimeUnit.SECONDS);
        // TODO 生产环境替换为短信服务商调用（aliyun sms / tencent sms），严禁日志打印验证码
        if (!"prod".equalsIgnoreCase(activeProfile))
        {
            log.info("[双因子] 用户 {} 获取验证码成功（开发环境: {}）", username, code);
            return code;
        }
        log.info("[双因子] 用户 {} 验证码已发送（短信通道）", username);
        return null;
    }

    /**
     * 校验验证码（一次性；失败 5 次锁定）
     */
    public boolean verifyOtp(String username, String code)
    {
        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(code))
        {
            return false;
        }
        // 失败次数限制
        String failKey = FAIL_CNT_PREFIX + username;
        Integer failCount = redisService.getCacheObject(failKey);
        if (failCount != null && failCount >= MAX_VERIFY_FAIL)
        {
            return false;
        }
        String cached = redisService.getCacheObject(KEY_PREFIX + username);
        if (cached == null || !cached.equals(code.trim()))
        {
            int count = (failCount == null ? 0 : failCount) + 1;
            redisService.setCacheObject(failKey, count, EXPIRE_MINUTES, TimeUnit.MINUTES);
            return false;
        }
        redisService.deleteObject(KEY_PREFIX + username);
        redisService.deleteObject(failKey);
        return true;
    }
}
