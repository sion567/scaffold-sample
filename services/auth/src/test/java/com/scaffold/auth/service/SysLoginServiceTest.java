package com.scaffold.auth.service;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.scaffold.auth.config.CaptchaProperties;
import com.scaffold.common.core.constant.CacheConstants;
import com.scaffold.common.core.constant.Constants;
import com.scaffold.common.core.constant.SecurityConstants;
import com.scaffold.common.core.domain.R;
import com.scaffold.common.core.exception.ServiceException;
import com.scaffold.common.core.utils.ip.IpUtils;
import com.scaffold.common.redis.service.RedisService;
import com.scaffold.common.test.DubboReferenceMocks;
import com.scaffold.system.api.convert.LoginUserConvert;
import com.scaffold.system.api.domain.SysUser;
import com.scaffold.system.api.model.LoginUser;
import com.scaffold.system.api.proto.GetUserInfoRequest;
import com.scaffold.system.api.proto.GetUserInfoResponse;
import com.scaffold.system.api.proto.RecordUserLoginRequest;
import com.scaffold.system.api.proto.RemoteUserService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 例子 B-1：Dubbo 消费方单元测试。
 *
 * <p>{@code SysLoginService} 通过 {@code @DubboReference} 字段注入 proto 生成的
 * {@link RemoteUserService}。单测里用 Mockito mock 该接口，再经 starter 的
 * {@link DubboReferenceMocks} 按类型注入字段，即可完整覆盖「消费方 → proto 请求/响应 → 结果处理」
 * 逻辑，不启动 Dubbo、不注册中心。</p>
 *
 * @author scaffold
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysLoginServiceTest
{
    /** proto 生成的内部用户接口（@DubboReference 注入的字段，单测时反射替换） */
    @Mock
    private RemoteUserService remoteUserService;

    @Mock
    private SysPasswordService passwordService;

    @Mock
    private SysRecordLogService recordLogService;

    @Mock
    private RedisService redisService;

    @Mock
    private TwoFactorService twoFactorService;

    private SysLoginService loginService;

    @BeforeEach
    void setUp()
    {
        // 默认关闭验证码校验，聚焦登录主流程；验证码用例单独开启
        loginService = new SysLoginService(passwordService, recordLogService, redisService, twoFactorService,
                captchaProperties(false));
        // @DubboReference 是字段注入，@InjectMocks 填不到：用 starter 工具按类型注入 mock
        DubboReferenceMocks.inject(loginService, remoteUserService);
    }

    private CaptchaProperties captchaProperties(boolean enabled)
    {
        CaptchaProperties props = new CaptchaProperties();
        props.setEnabled(enabled);
        return props;
    }

    /** 正常状态的账号 */
    private SysUser user(String userName)
    {
        SysUser user = new SysUser();
        user.setUserId(1L);
        user.setUserName(userName);
        user.setNickName(userName);
        user.setStatus("0");
        user.setDelFlag("0");
        return user;
    }

    /** 构造提供方（scaffold-system）正常返回的 proto 响应 */
    private GetUserInfoResponse okResponse(SysUser user)
    {
        LoginUser vo = new LoginUser();
        vo.setSysUser(user);
        vo.setRoles(Set.of("common"));
        vo.setPermissions(Set.of("system:user:list"));
        return GetUserInfoResponse.newBuilder()
                .setCode(R.SUCCESS)
                .setData(LoginUserConvert.toProto(vo))
                .build();
    }

    @Test
    @DisplayName("登录成功：proto 响应转 LoginUser，记录成功日志并回报登录信息")
    void login_success()
    {
        try (MockedStatic<IpUtils> ip = mockStatic(IpUtils.class))
        {
            ip.when(IpUtils::getIpAddr).thenReturn("127.0.0.1");
            when(remoteUserService.getUserInfo(any(GetUserInfoRequest.class)))
                    .thenReturn(okResponse(user("admin")));
            when(twoFactorService.isRequired(any(LoginUser.class))).thenReturn(false);

            LoginUser result = loginService.login("admin", "admin123");

            assertEquals("admin", result.getSysUser().getUserName());
            assertEquals("common", result.getRoles().iterator().next());

            // 请求方按 INNER 来源发起内部调用
            verify(remoteUserService).getUserInfo(any(GetUserInfoRequest.class));
            // 成功日志 + 回报登录 IP/时间
            verify(recordLogService).recordLogininfor("admin", Constants.LOGIN_SUCCESS, "登录成功");
            verify(remoteUserService).recordUserLogin(any(RecordUserLoginRequest.class));
        }
    }

    @Test
    @DisplayName("登录失败：提供方返回 R.FAIL 时把 msg 抛成 ServiceException")
    void login_userNotFound_throwsRemoteMsg()
    {
        when(remoteUserService.getUserInfo(any(GetUserInfoRequest.class)))
                .thenReturn(GetUserInfoResponse.newBuilder()
                        .setCode(R.FAIL).setMsg("用户名或密码错误").build());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> loginService.login("admin", "admin123"));

        assertEquals("用户名或密码错误", ex.getMessage());
        verify(recordLogService, never()).recordLogininfor(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("登录失败：账号已删除")
    void login_deletedUser_throws()
    {
        SysUser deleted = user("admin");
        deleted.setDelFlag("2");
        when(remoteUserService.getUserInfo(any(GetUserInfoRequest.class)))
                .thenReturn(okResponse(deleted));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> loginService.login("admin", "admin123"));

        assertEquals("对不起，您的账号：admin 已被删除", ex.getMessage());
        verify(recordLogService).recordLogininfor("admin", Constants.LOGIN_FAIL, "对不起，您的账号已被删除");
    }

    @Test
    @DisplayName("登录失败：账号已停用")
    void login_disabledUser_throws()
    {
        SysUser disabled = user("admin");
        disabled.setStatus("1");
        when(remoteUserService.getUserInfo(any(GetUserInfoRequest.class)))
                .thenReturn(okResponse(disabled));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> loginService.login("admin", "admin123"));

        assertEquals("对不起，您的账号：admin 已停用", ex.getMessage());
    }

    @Test
    @DisplayName("登录失败：双因子开启，OTP 缺失时拒绝")
    void login_twoFactorRequired_withoutOtp_throws()
    {
        try (MockedStatic<IpUtils> ip = mockStatic(IpUtils.class))
        {
            ip.when(IpUtils::getIpAddr).thenReturn("127.0.0.1");
            when(remoteUserService.getUserInfo(any(GetUserInfoRequest.class)))
                    .thenReturn(okResponse(user("admin")));
            when(twoFactorService.isRequired(any(LoginUser.class))).thenReturn(true);
            when(twoFactorService.verifyOtp(eq("admin"), isNull())).thenReturn(false);

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> loginService.login("admin", "admin123"));

            assertEquals("双因子验证失败，请先获取短信验证码", ex.getMessage());
            verify(passwordService).validate(any(SysUser.class), eq("admin123"));
        }
    }

    @Test
    @DisplayName("入参校验：用户名或密码为空")
    void login_blankInput_throwsAndRecords()
    {
        ServiceException ex = assertThrows(ServiceException.class,
                () -> loginService.login("", "admin123"));

        assertEquals("用户/密码必须填写", ex.getMessage());
        verify(recordLogService).recordLogininfor("", Constants.LOGIN_FAIL, "用户/密码必须填写");
    }

    @Test
    @DisplayName("recordLoginInfo：组装 RecordUserLoginRequest，来源为 INNER")
    void recordLoginInfo_sendsProtoRequest()
    {
        try (MockedStatic<IpUtils> ip = mockStatic(IpUtils.class))
        {
            ip.when(IpUtils::getIpAddr).thenReturn("10.0.0.8");

            loginService.recordLoginInfo(1L);

            verify(remoteUserService).recordUserLogin(any(RecordUserLoginRequest.class));
        }
    }

    @Test
    @DisplayName("logout：只记退出日志")
    void logout_recordsOnly()
    {
        loginService.logout("admin");

        verify(recordLogService).recordLogininfor("admin", Constants.LOGOUT, "退出成功");
        verify(remoteUserService, never()).getUserInfo(any(GetUserInfoRequest.class));
    }

    // ── 图形验证码（scaffold.auth.captcha.enabled=true，登录/注册解密后校验）──

    /** 重建开启验证码校验的 service（setUp 默认关闭） */
    private void newServiceWithCaptchaEnabled()
    {
        loginService = new SysLoginService(passwordService, recordLogService, redisService, twoFactorService,
                captchaProperties(true));
        DubboReferenceMocks.inject(loginService, remoteUserService);
    }

    @Test
    @DisplayName("验证码开启：code 为空时拒绝登录且不触达用户接口")
    void login_captchaEnabled_blankCode_throws()
    {
        newServiceWithCaptchaEnabled();

        ServiceException ex = assertThrows(ServiceException.class,
                () -> loginService.login("admin", "admin123", null, "", "uuid-1"));

        assertEquals("验证码不能为空", ex.getMessage());
        verify(recordLogService).recordLogininfor("admin", Constants.LOGIN_FAIL, "验证码不能为空");
        verify(remoteUserService, never()).getUserInfo(any(GetUserInfoRequest.class));
    }

    @Test
    @DisplayName("验证码开启：Redis 无记录视为已失效")
    void login_captchaEnabled_expiredCode_throws()
    {
        newServiceWithCaptchaEnabled();
        when(redisService.getCacheObject(CacheConstants.CAPTCHA_CODE_KEY + "uuid-1")).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> loginService.login("admin", "admin123", null, "1234", "uuid-1"));

        assertEquals("验证码已失效", ex.getMessage());
        verify(remoteUserService, never()).getUserInfo(any(GetUserInfoRequest.class));
    }

    @Test
    @DisplayName("验证码开启：验证码错误时拒绝，且已删除缓存（一次性使用）")
    void login_captchaEnabled_wrongCode_throwsAndDeletes()
    {
        newServiceWithCaptchaEnabled();
        when(redisService.getCacheObject(CacheConstants.CAPTCHA_CODE_KEY + "uuid-1")).thenReturn("4321");

        ServiceException ex = assertThrows(ServiceException.class,
                () -> loginService.login("admin", "admin123", null, "1234", "uuid-1"));

        assertEquals("验证码错误", ex.getMessage());
        verify(redisService).deleteObject(CacheConstants.CAPTCHA_CODE_KEY + "uuid-1");
        verify(remoteUserService, never()).getUserInfo(any(GetUserInfoRequest.class));
    }

    @Test
    @DisplayName("验证码开启：code/uuid 正确时放行走完整登录流程")
    void login_captchaEnabled_correctCode_passes()
    {
        newServiceWithCaptchaEnabled();
        when(redisService.getCacheObject(CacheConstants.CAPTCHA_CODE_KEY + "uuid-1")).thenReturn("1234");
        try (MockedStatic<IpUtils> ip = mockStatic(IpUtils.class))
        {
            ip.when(IpUtils::getIpAddr).thenReturn("127.0.0.1");
            when(remoteUserService.getUserInfo(any(GetUserInfoRequest.class)))
                    .thenReturn(okResponse(user("admin")));
            when(twoFactorService.isRequired(any(LoginUser.class))).thenReturn(false);

            LoginUser result = loginService.login("admin", "admin123", null, "1234", "uuid-1");

            assertEquals("admin", result.getSysUser().getUserName());
            // 一次性使用：取出即删
            verify(redisService).deleteObject(CacheConstants.CAPTCHA_CODE_KEY + "uuid-1");
            verify(recordLogService).recordLogininfor("admin", Constants.LOGIN_SUCCESS, "登录成功");
        }
    }
}
