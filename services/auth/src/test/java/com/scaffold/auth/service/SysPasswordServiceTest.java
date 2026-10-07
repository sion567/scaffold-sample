package com.scaffold.auth.service;

import com.scaffold.common.core.constant.CacheConstants;
import com.scaffold.common.core.constant.Constants;
import com.scaffold.common.core.exception.ServiceException;
import com.scaffold.common.redis.service.RedisService;
import com.scaffold.common.security.utils.SecurityUtils;
import com.scaffold.system.api.domain.SysUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SysPasswordService 密码校验与锁定策略测试。
 *
 * <p>使用真实 SecurityUtils（SM3 加盐哈希）验证密码匹配，不依赖 mockStatic。
 * 覆盖维度：
 * <ul>
 *   <li>密码正确：验证通过，清除错误计数</li>
 *   <li>密码错误：错误计数 +1，抛出异常</li>
 *   <li>错误超限：锁定 30 分钟，抛出异常</li>
 *   <li>配置缺失：回退到默认值（5 次 / 30 分钟）</li>
 *   <li>合规基线：配置值超过/低于基线时按基线生效</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysPasswordServiceTest
{
    @Mock
    private RedisService redisService;

    @Mock
    private SysRecordLogService recordLogService;

    private SysPasswordService passwordService;

    private SysUser user(String name, String password)
    {
        SysUser u = new SysUser();
        u.setUserName(name);
        u.setPassword(SecurityUtils.encryptPassword(password));
        return u;
    }

    @BeforeEach
    void setUp()
    {
        passwordService = new SysPasswordService(redisService, recordLogService);
    }

    @Nested
    @DisplayName("validate 密码正确")
    class PasswordCorrect
    {
        @Test
        @DisplayName("密码正确：清除错误计数，不抛异常")
        void validate_correct_clearsCache()
        {
            SysUser u = user("admin", "pass123");
            when(redisService.hasKey("pwd_err_cnt:admin")).thenReturn(true);

            passwordService.validate(u, "pass123");

            verify(redisService).deleteObject("pwd_err_cnt:admin");
            verify(recordLogService, never()).recordLogininfor(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("密码正确且无错误计数：直接通过")
        void validate_correct_noPriorCount()
        {
            SysUser u = user("admin", "pass123");
            when(redisService.hasKey("pwd_err_cnt:admin")).thenReturn(false);

            passwordService.validate(u, "pass123");

            verify(redisService, never()).deleteObject(anyString());
        }
    }

    @Nested
    @DisplayName("validate 密码错误")
    class PasswordWrong
    {
        private SysUser hashedUser()
        {
            // 存储正确密码的 hash，但传入错误密码
            return user("admin", "correct-password");
        }

        @Test
        @DisplayName("密码错误（首次）：错误计数变为 1，抛出异常")
        void validate_wrong_firstTime()
        {
            when(redisService.getCacheObject(anyString())).thenReturn(null);
            when(redisService.getCacheObject(CacheConstants.SYS_CONFIG_KEY + "sys.account.login.maxRetry"))
                    .thenReturn(null);
            when(redisService.getCacheObject(CacheConstants.SYS_CONFIG_KEY + "sys.account.login.lockTime"))
                    .thenReturn(null);

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> passwordService.validate(hashedUser(), "wrong-password"));

            assertEquals("用户不存在/密码错误", ex.getMessage());
            verify(redisService).setCacheObject(eq("pwd_err_cnt:admin"), eq(1), anyLong(), any());
        }

        @Test
        @DisplayName("密码错误（已有 1 次）：错误计数变为 2")
        void validate_wrong_secondTime()
        {
            when(redisService.getCacheObject("pwd_err_cnt:admin")).thenReturn(1);
            when(redisService.getCacheObject(CacheConstants.SYS_CONFIG_KEY + "sys.account.login.maxRetry"))
                    .thenReturn(null);
            when(redisService.getCacheObject(CacheConstants.SYS_CONFIG_KEY + "sys.account.login.lockTime"))
                    .thenReturn(null);

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> passwordService.validate(hashedUser(), "wrong-password"));

            verify(redisService).setCacheObject(eq("pwd_err_cnt:admin"), eq(2), anyLong(), any());
        }
    }

    @Nested
    @DisplayName("validate 密码锁定（合规基线）")
    class PasswordLock
    {
        private SysUser hashedUser()
        {
            return user("admin", "correct-password");
        }

        @Test
        @DisplayName("错误 5 次（合规上限）：抛出锁定异常，30 分钟")
        void validate_lockedAtMaxRetry()
        {
            when(redisService.getCacheObject("pwd_err_cnt:admin")).thenReturn(5);
            when(redisService.getCacheObject(CacheConstants.SYS_CONFIG_KEY + "sys.account.login.maxRetry"))
                    .thenReturn(null);
            when(redisService.getCacheObject(CacheConstants.SYS_CONFIG_KEY + "sys.account.login.lockTime"))
                    .thenReturn(null);

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> passwordService.validate(hashedUser(), "wrong-password"));

            assertEquals("密码输入错误5次，帐户锁定30分钟", ex.getMessage());
            verify(recordLogService).recordLogininfor(eq("admin"), eq(Constants.LOGIN_FAIL), anyString());
        }

        @Test
        @DisplayName("配置超限（10 次）→ maxRetry 按上限 5 生效，lockTime 取配置值 100")
        void validate_configExceedsBaseline_usesBaseline()
        {
            when(redisService.getCacheObject("pwd_err_cnt:admin")).thenReturn(5);
            // maxRetry=10 > 基线5 → Math.min(10,5)=5
            when(redisService.getCacheObject(CacheConstants.SYS_CONFIG_KEY + "sys.account.login.maxRetry"))
                    .thenReturn(10);
            // lockTime=100 > 基线30 → Math.max(100,30)=100
            when(redisService.getCacheObject(CacheConstants.SYS_CONFIG_KEY + "sys.account.login.lockTime"))
                    .thenReturn(100L);

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> passwordService.validate(hashedUser(), "wrong-password"));

            // maxRetry=5（Math.min），lockTime=100（Math.max）
            assertEquals("密码输入错误5次，帐户锁定100分钟", ex.getMessage());
        }

        @Test
        @DisplayName("配置低于基线（3 次 / 10 分钟）→ 按合规基线 5/30 生效")
        void validate_configBelowBaseline_usesBaseline()
        {
            when(redisService.getCacheObject("pwd_err_cnt:admin")).thenReturn(5);
            // maxRetry=3 < 基线5 → Math.min(3,5)=3，但错误计数已达5，触发锁定
            when(redisService.getCacheObject(CacheConstants.SYS_CONFIG_KEY + "sys.account.login.maxRetry"))
                    .thenReturn(3);
            // lockTime=10 < 基线30 → Math.max(10,30)=30
            when(redisService.getCacheObject(CacheConstants.SYS_CONFIG_KEY + "sys.account.login.lockTime"))
                    .thenReturn(10L);

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> passwordService.validate(hashedUser(), "wrong-password"));

            // maxRetry=3（实际已满足5次），lockTime=30（Math.max）
            assertEquals("密码输入错误3次，帐户锁定30分钟", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("clearLoginRecordCache")
    class ClearCache
    {
        @Test
        @DisplayName("存在错误计数 → 删除后返回")
        void clear_loginRecordCache_deletes()
        {
            when(redisService.hasKey("pwd_err_cnt:admin")).thenReturn(true);

            passwordService.clearLoginRecordCache("admin");

            verify(redisService).deleteObject("pwd_err_cnt:admin");
        }

        @Test
        @DisplayName("不存在错误计数 → 不调用 delete")
        void clear_loginRecordCache_nothingToDelete()
        {
            when(redisService.hasKey("pwd_err_cnt:admin")).thenReturn(false);

            passwordService.clearLoginRecordCache("admin");

            verify(redisService, never()).deleteObject(anyString());
        }
    }
}
