package com.scaffold.system.dubbo;

import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.scaffold.common.core.constant.SecurityConstants;
import com.scaffold.common.core.domain.R;
import com.scaffold.common.test.DubboDirectRuntime;
import com.scaffold.system.api.proto.GetUserInfoRequest;
import com.scaffold.system.api.proto.GetUserInfoResponse;
import com.scaffold.system.api.proto.RemoteUserService;
import com.scaffold.system.api.domain.SysUser;
import com.scaffold.system.service.ISysConfigService;
import com.scaffold.system.service.ISysPermissionService;
import com.scaffold.system.service.ISysUserService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Dubbo 直连集成测试（ct-starter-test 的 {@code DubboDirectRuntime}）。
 *
 * <p>与 {@link RemoteUserProtoServiceImplTest}（纯单测，直接调方法）的区别：
 * 这里经真实 Triple 端口走一遍 RPC——协议暴露、protobuf 序列化往返、
 * Dubbo Filter 链都会真实执行，但注册中心是 N/A，不依赖 Nacos。</p>
 *
 * <p>注意：实现是手工 new 的普通对象，Spring AOP 切面（@InnerAuth）不会织入；
 * 需要验证 InnerAuth 语义时单独测 InnerAuthAspect。</p>
 *
 * @author ct
 */
@ExtendWith(MockitoExtension.class)
class RemoteUserProtoDirectConnectTest
{
    @Mock
    private ISysUserService userService;

    @Mock
    private ISysPermissionService permissionService;

    @Mock
    private ISysConfigService configService;

    private DubboDirectRuntime runtime;

    @BeforeEach
    void startRuntime()
    {
        RemoteUserProtoServiceImpl impl =
                new RemoteUserProtoServiceImpl(userService, permissionService, configService);
        runtime = DubboDirectRuntime.tri()
                .service(RemoteUserService.class, impl)
                .start();
    }

    @AfterEach
    void stopRuntime()
    {
        runtime.close();
    }

    @Test
    @DisplayName("getUserInfo：经真实 tri 端口完成 protobuf RPC 往返")
    void getUserInfo_overRealTriRpc()
    {
        SysUser stored = new SysUser();
        stored.setUserId(1L);
        stored.setUserName("admin");
        stored.setNickName("管理员");
        stored.setStatus("0");
        stored.setDelFlag("0");
        when(userService.selectUserByUserName("admin")).thenReturn(stored);
        when(permissionService.getRolePermission(any(SysUser.class))).thenReturn(Set.of("admin"));
        when(permissionService.getMenuPermission(any(SysUser.class))).thenReturn(Set.of("*:*:*"));

        // 直连消费方：等价于 scaffold-auth 里 @DubboReference 注入后发起的调用
        RemoteUserService consumer = runtime.consumer(RemoteUserService.class);

        GetUserInfoResponse response = consumer.getUserInfo(
                GetUserInfoRequest.newBuilder().setUsername("admin").setSource(SecurityConstants.INNER).build());

        assertEquals(R.SUCCESS, response.getCode());
        assertEquals("管理员", response.getData().getSysUser().getNickName());
        assertEquals(Set.of("admin"), Set.copyOf(response.getData().getRolesList()));
    }
}
