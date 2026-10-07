package com.scaffold.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.scaffold.common.core.constant.Constants;
import com.scaffold.common.log.persist.LogPersister;
import com.scaffold.system.api.domain.SysLogininfor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

/**
 * SysRecordLogService 登录日志记录测试。
 *
 * <p>覆盖维度：
 * <ul>
 *   <li>记录成功日志：LOGIN_SUCCESS → 状态码 "0"</li>
 *   <li>记录退出日志：LOGOUT → 状态码 "0"</li>
 *   <li>记录注册日志：REGISTER → 状态码 "0"</li>
 *   <li>记录失败日志：LOGIN_FAIL → 状态码 "1"</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysRecordLogServiceTest
{
    @Mock
    private LogPersister logPersister;

    private SysRecordLogService logService;

    @BeforeEach
    void setUp()
    {
        logService = new SysRecordLogService(logPersister);
    }

    @Test
    @DisplayName("LOGIN_SUCCESS → 状态码 0（成功）")
    void recordLogininfor_success_mapsToZero()
    {
        logService.recordLogininfor("admin", Constants.LOGIN_SUCCESS, "登录成功");

        assertEquals("0", capturedStatus());
    }

    @Test
    @DisplayName("LOGOUT → 状态码 0（成功）")
    void recordLogininfor_logout_mapsToZero()
    {
        logService.recordLogininfor("admin", Constants.LOGOUT, "退出成功");

        assertEquals("0", capturedStatus());
    }

    @Test
    @DisplayName("REGISTER → 状态码 0（成功）")
    void recordLogininfor_register_mapsToZero()
    {
        logService.recordLogininfor("newuser", Constants.REGISTER, "注册成功");

        assertEquals("0", capturedStatus());
    }

    @Test
    @DisplayName("LOGIN_FAIL → 状态码 1（失败）")
    void recordLogininfor_fail_mapsToOne()
    {
        logService.recordLogininfor("admin", Constants.LOGIN_FAIL, "密码错误");

        assertEquals("1", capturedStatus());
    }

    private String capturedStatus()
    {
        ArgumentCaptor<SysLogininfor> captor = ArgumentCaptor.forClass(SysLogininfor.class);
        verify(logPersister).saveLogininfor(captor.capture());
        return captor.getValue().getStatus();
    }
}
