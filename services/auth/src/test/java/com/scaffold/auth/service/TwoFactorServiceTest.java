package com.scaffold.auth.service;

import com.scaffold.common.core.exception.ServiceException;
import com.scaffold.common.redis.service.RedisService;
import com.scaffold.system.api.model.LoginUser;
import com.scaffold.auth.config.TwoFactorProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TwoFactorService 双因子认证测试。
 *
 * <p>覆盖维度：
 * <ul>
 *   <li>isEnabled：开关状态</li>
 *   <li>isRequired：配置驱动（required-roles 精确匹配 / required-role-prefixes 前缀匹配）</li>
 *   <li>sendOtp：正常发送、冷却限制、空用户名</li>
 *   <li>verifyOtp：成功验证、错误验证码、失败 5 次锁定、null/空拒绝</li>
 *   <li>非 prod 环境返回明文验证码</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TwoFactorServiceTest
{
    @Mock
    private RedisService redisService;

    private TwoFactorService twoFactor;

    private void newService(boolean enabled, String profile)
    {
        TwoFactorProperties props = new TwoFactorProperties();
        props.setEnabled(enabled);
        twoFactor = new TwoFactorService(redisService, props, profile);
    }

    private void newService(boolean enabled, List<String> roles, List<String> rolePrefixes, String profile)
    {
        TwoFactorProperties props = new TwoFactorProperties();
        props.setEnabled(enabled);
        props.setRequiredRoles(roles);
        props.setRequiredRolePrefixes(rolePrefixes);
        twoFactor = new TwoFactorService(redisService, props, profile);
    }

    private LoginUser loginUser(String username, Set<String> roles)
    {
        LoginUser u = new LoginUser();
        u.setUsername(username);
        u.setRoles(roles);
        return u;
    }

    // ─── isRequired ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("isRequired 双因子是否必需")
    class IsRequired
    {
        @Test
        @DisplayName("开关关闭：返回 false")
        void disabled_alwaysFalse()
        {
            newService(false, "dev");
            assertFalse(twoFactor.isRequired(loginUser("admin", Set.of("admin"))));
        }

        @Test
        @DisplayName("默认配置（required-roles=[admin]）：admin 角色需要双因子")
        void adminRole_defaultConfig()
        {
            newService(true, "prod");
            assertTrue(twoFactor.isRequired(loginUser("anyuser", Set.of("admin"))));
        }

        @Test
        @DisplayName("默认配置：未命中配置角色的用户名不需要双因子")
        void normalUser_defaultConfig()
        {
            newService(true, "prod");
            assertFalse(twoFactor.isRequired(loginUser("zhangsan", Set.of("common"))));
        }

        @Test
        @DisplayName("配置角色前缀：命中前缀的角色需要双因子")
        void prefixConfigured()
        {
            newService(true, List.of(), List.of("op_"), "prod");
            assertTrue(twoFactor.isRequired(loginUser("anyuser", Set.of("op_viewer"))));
        }

        @Test
        @DisplayName("配置角色前缀：未命中前缀的角色不需要双因子")
        void prefixNotMatched()
        {
            newService(true, List.of(), List.of("op_"), "prod");
            assertFalse(twoFactor.isRequired(loginUser("anyuser", Set.of("viewer"))));
        }

        @Test
        @DisplayName("配置角色精确匹配：命中配置角色需要双因子")
        void roleExactMatchConfigured()
        {
            newService(true, List.of("manager"), List.of(), "prod");
            assertTrue(twoFactor.isRequired(loginUser("anyuser", Set.of("manager"))));
        }

        @Test
        @DisplayName("多角色：任一角色命中即需要双因子")
        void anyRoleMatched()
        {
            newService(true, List.of("admin"), List.of("op_"), "prod");
            assertTrue(twoFactor.isRequired(loginUser("anyuser", Set.of("common", "op_viewer"))));
        }

        @Test
        @DisplayName("null 角色集合：返回 false")
        void nullRoles()
        {
            newService(true, "prod");
            assertFalse(twoFactor.isRequired(loginUser("admin", null)));
        }

        @Test
        @DisplayName("null 账号：返回 false")
        void nullLoginUser()
        {
            newService(true, "prod");
            assertFalse(twoFactor.isRequired(null));
        }
    }

    // ─── sendOtp ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("sendOtp 发送验证码")
    class SendOtp
    {
        @Test
        @DisplayName("用户名空：抛异常")
        void blankUsername()
        {
            newService(true, "dev");

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> twoFactor.sendOtp("  "));

            assertTrue(ex.getMessage().contains("用户名不能为空"));
        }

        @Test
        @DisplayName("60 秒冷却期内：抛异常")
        void cooldownActive()
        {
            newService(true, "dev");
            when(redisService.getCacheObject("two_factor:code:admin:cooldown")).thenReturn("1");

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> twoFactor.sendOtp("admin"));

            assertTrue(ex.getMessage().contains("验证码发送过于频繁"));
            verify(redisService, never()).setCacheObject(anyString(), any(), anyLong(), any());
        }

        @Test
        @DisplayName("正常发送（非 prod 环境）：返回明文验证码")
        void normalSend_devEnvironment()
        {
            newService(true, "dev");
            when(redisService.getCacheObject(anyString())).thenReturn(null);

            String code = twoFactor.sendOtp("admin");

            assertNotNull(code);
            assertEquals(6, code.length());
            assertTrue(code.matches("\\d{6}"));
            verify(redisService).setCacheObject(eq("two_factor:code:admin"), eq(code),
                    eq(5L), eq(TimeUnit.MINUTES));
            verify(redisService).setCacheObject(eq("two_factor:code:admin:cooldown"),
                    eq("1"), eq(60L), eq(TimeUnit.SECONDS));
        }

        @Test
        @DisplayName("正常发送（prod 环境）：返回 null（走短信通道）")
        void normalSend_prodEnvironment()
        {
            newService(true, "prod");
            when(redisService.getCacheObject(anyString())).thenReturn(null);

            String code = twoFactor.sendOtp("admin");

            assertNull(code);
        }
    }

    // ─── verifyOtp ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("verifyOtp 校验验证码")
    class VerifyOtp
    {
        @Test
        @DisplayName("null/空用户名或验证码：返回 false")
        void nullOrEmptyInput()
        {
            newService(true, "dev");
            assertFalse(twoFactor.verifyOtp(null, "123456"));
            assertFalse(twoFactor.verifyOtp("admin", null));
            assertFalse(twoFactor.verifyOtp("admin", ""));
            assertFalse(twoFactor.verifyOtp("", "123456"));
        }

        @Test
        @DisplayName("失败 5 次：返回 false（锁定）")
        void failedFiveTimes()
        {
            newService(true, "dev");
            when(redisService.getCacheObject("two_factor:fail:admin")).thenReturn(5);

            boolean result = twoFactor.verifyOtp("admin", "123456");

            assertFalse(result);
        }

        @Test
        @DisplayName("验证码错误：失败计数 +1，返回 false")
        void wrongCode()
        {
            newService(true, "dev");
            when(redisService.getCacheObject("two_factor:code:admin")).thenReturn("111222");

            boolean result = twoFactor.verifyOtp("admin", "  123456  ");

            assertFalse(result);
            verify(redisService).setCacheObject(eq("two_factor:fail:admin"),
                    eq(1), eq(5L), eq(TimeUnit.MINUTES));
        }

        @Test
        @DisplayName("验证码正确：删除验证码和失败计数，返回 true")
        void correctCode()
        {
            newService(true, "dev");
            when(redisService.getCacheObject("two_factor:code:admin")).thenReturn("123456");
            when(redisService.getCacheObject("two_factor:fail:admin")).thenReturn(null);

            boolean result = twoFactor.verifyOtp("admin", "123456");

            assertTrue(result);
            verify(redisService).deleteObject("two_factor:code:admin");
            verify(redisService).deleteObject("two_factor:fail:admin");
        }

        @Test
        @DisplayName("验证码已过期（Redis 无值）：返回 false")
        void expiredCode()
        {
            newService(true, "dev");
            when(redisService.getCacheObject("two_factor:code:admin")).thenReturn(null);

            boolean result = twoFactor.verifyOtp("admin", "123456");

            assertFalse(result);
        }

        @Test
        @DisplayName("第二次失败：失败计数 = 2")
        void secondFailure()
        {
            newService(true, "dev");
            when(redisService.getCacheObject("two_factor:code:admin")).thenReturn("111222");
            when(redisService.getCacheObject("two_factor:fail:admin")).thenReturn(1);

            twoFactor.verifyOtp("admin", "000000");

            verify(redisService).setCacheObject(eq("two_factor:fail:admin"),
                    eq(2), anyLong(), any());
        }
    }
}
