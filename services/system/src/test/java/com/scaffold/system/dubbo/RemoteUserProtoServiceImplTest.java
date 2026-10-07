package com.scaffold.system.dubbo;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.scaffold.common.core.constant.SecurityConstants;
import com.scaffold.common.core.domain.R;
import com.scaffold.system.api.convert.SysUserConvert;
import com.scaffold.system.api.proto.BoolResponse;
import com.scaffold.system.api.proto.GetUserInfoRequest;
import com.scaffold.system.api.proto.GetUserInfoResponse;
import com.scaffold.system.api.proto.RegisterUserInfoRequest;
import com.scaffold.system.api.proto.RecordUserLoginRequest;
import com.scaffold.system.api.domain.SysUser;
import com.scaffold.system.service.ISysConfigService;
import com.scaffold.system.service.ISysPermissionService;
import com.scaffold.system.service.ISysUserService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 例子 B-2：Dubbo 提供方单元测试（Triple/protobuf 内部接口）。
 *
 * <p>提供方 {@link RemoteUserProtoServiceImpl} 继承 dubbo-maven-plugin 生成的
 * {@code RemoteUserServiceImplBase}。测试时直接 new（@InjectMocks）并调用方法，
 * 用 proto Builder 构造入参、读 proto 字段断言出参——不启动 Dubbo Server、
 * 不走注册中心，就能覆盖 IDL 契约、业务分支与 @InnerAuth 之下的全部逻辑。</p>
 *
 * <p>（@InnerAuth 是 AOP 切面，纯单测不织入；其拦截逻辑由 InnerAuthAspect 自己的单测覆盖。）</p>
 *
 * @author ct
 */
@ExtendWith(MockitoExtension.class)
class RemoteUserProtoServiceImplTest
{
    @Mock
    private ISysUserService userService;

    @Mock
    private ISysPermissionService permissionService;

    @Mock
    private ISysConfigService configService;

    @InjectMocks
    private RemoteUserProtoServiceImpl provider;

    private SysUser user(String userName)
    {
        SysUser user = new SysUser();
        user.setUserId(1L);
        user.setDeptId(100L);
        user.setUserName(userName);
        user.setNickName("管理员");
        user.setStatus("0");
        user.setDelFlag("0");
        return user;
    }

    @Test
    @DisplayName("getUserInfo：命中用户，返回 R.SUCCESS + proto 化的用户/角色/权限")
    void getUserInfo_found_returnsData()
    {
        SysUser stored = user("admin");
        when(userService.selectUserByUserName("admin")).thenReturn(stored);
        when(permissionService.getRolePermission(stored)).thenReturn(Set.of("admin"));
        when(permissionService.getMenuPermission(stored)).thenReturn(Set.of("*:*:*"));

        GetUserInfoResponse response = provider.getUserInfo(
                GetUserInfoRequest.newBuilder().setUsername("admin").setSource(SecurityConstants.INNER).build());

        assertEquals(R.SUCCESS, response.getCode());
        assertEquals(1L, response.getData().getSysUser().getUserId());
        assertEquals("管理员", response.getData().getSysUser().getNickName());
        assertTrue(response.getData().getRolesList().contains("admin"));
        assertTrue(response.getData().getPermissionsList().contains("*:*:*"));
    }

    @Test
    @DisplayName("getUserInfo：用户不存在，返回 R.FAIL 且不带 data")
    void getUserInfo_notFound_returnsFail()
    {
        when(userService.selectUserByUserName("nobody")).thenReturn(null);

        GetUserInfoResponse response = provider.getUserInfo(
                GetUserInfoRequest.newBuilder().setUsername("nobody").setSource(SecurityConstants.INNER).build());

        assertEquals(R.FAIL, response.getCode());
        assertEquals("用户名或密码错误", response.getMsg());
        assertFalse(response.hasData());
    }

    @Test
    @DisplayName("registerUserInfo：注册开关关闭时拒绝")
    void register_switchOff_rejected()
    {
        when(configService.selectConfigByKey("sys.account.registerUser")).thenReturn("false");

        BoolResponse response = provider.registerUserInfo(RegisterUserInfoRequest.newBuilder()
                .setUser(SysUserConvert.toProto(user("newbie")))
                .setSource(SecurityConstants.INNER)
                .build());

        assertEquals(R.FAIL, response.getCode());
        assertEquals("当前系统没有开启注册功能！", response.getMsg());
        verify(userService, org.mockito.Mockito.never()).registerUser(any(SysUser.class));
    }

    @Test
    @DisplayName("registerUserInfo：开关开启且用户名唯一时注册成功")
    void register_switchOn_registers()
    {
        when(configService.selectConfigByKey("sys.account.registerUser")).thenReturn("true");
        when(userService.checkUserNameUnique(any(SysUser.class))).thenReturn(true);
        when(userService.registerUser(any(SysUser.class))).thenReturn(true);

        BoolResponse response = provider.registerUserInfo(RegisterUserInfoRequest.newBuilder()
                .setUser(SysUserConvert.toProto(user("newbie")))
                .setSource(SecurityConstants.INNER)
                .build());

        assertEquals(R.SUCCESS, response.getCode());
        assertTrue(response.getData());
    }

    @Test
    @DisplayName("recordUserLogin：透传用户登录信息并返回 BoolResponse")
    void recordLoginInfo_updatesAndReturnsTrue()
    {
        when(userService.updateLoginInfo(any(SysUser.class))).thenReturn(true);

        BoolResponse response = provider.recordUserLogin(RecordUserLoginRequest.newBuilder()
                .setUser(SysUserConvert.toProto(user("admin")))
                .setSource(SecurityConstants.INNER)
                .build());

        assertEquals(R.SUCCESS, response.getCode());
        assertTrue(response.getData());
        verify(userService).updateLoginInfo(any(SysUser.class));
    }
}
