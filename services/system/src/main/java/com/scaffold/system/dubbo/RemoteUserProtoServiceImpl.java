package com.scaffold.system.dubbo;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.scaffold.system.api.convert.LoginUserConvert;
import com.scaffold.system.api.convert.SysUserConvert;
import com.scaffold.system.api.proto.*;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Component;
import com.scaffold.common.core.domain.R;
import com.scaffold.common.core.utils.StringUtils;
import com.scaffold.common.security.annotation.InnerAuth;
import com.scaffold.system.api.domain.SysUser;
import com.scaffold.system.api.model.LoginUser;
import com.scaffold.system.service.ISysConfigService;
import com.scaffold.system.service.ISysPermissionService;
import com.scaffold.system.service.ISysUserService;

/**
 * 用户服务内部 Dubbo 实现（IDL/protobuf，Triple 协议）
 *
 * @author ct
 */
@DubboService
public class RemoteUserProtoServiceImpl implements RemoteUserService
{
    private final ISysUserService userService;
    private final ISysPermissionService permissionService;
    private final ISysConfigService configService;

    public RemoteUserProtoServiceImpl(ISysUserService userService, ISysPermissionService permissionService,
                                      ISysConfigService configService)
    {
        this.userService = userService;
        this.permissionService = permissionService;
        this.configService = configService;
    }

    @Override
    @InnerAuth
    public GetUserInfoResponse getUserInfo(GetUserInfoRequest request)
    {
        SysUser sysUser = userService.selectUserByUserName(request.getUsername());
        if (StringUtils.isNull(sysUser))
        {
            return GetUserInfoResponse.newBuilder()
                    .setCode(R.FAIL).setMsg("用户名或密码错误").build();
        }
        // 角色集合
        Set<String> roles = permissionService.getRolePermission(sysUser);
        // 权限集合
        Set<String> permissions = permissionService.getMenuPermission(sysUser);
        LoginUser sysUserVo = new LoginUser();
        sysUserVo.setSysUser(sysUser);
        sysUserVo.setRoles(roles);
        sysUserVo.setPermissions(permissions);
        return GetUserInfoResponse.newBuilder()
                .setCode(R.SUCCESS)
                .setData(LoginUserConvert.toProto(sysUserVo))
                .build();
    }

    @Override
    public CompletableFuture<GetUserInfoResponse> getUserInfoAsync(GetUserInfoRequest request) {
        return CompletableFuture.completedFuture(getUserInfo(request));
    }

    @Override
    @InnerAuth
    public BoolResponse registerUserInfo(RegisterUserInfoRequest request)
    {
        SysUser sysUser = SysUserConvert.toJava(request.getUser());
        String username = sysUser.getUserName();
        if (!("true".equals(configService.selectConfigByKey("sys.account.registerUser"))))
        {
            return BoolResponse.newBuilder()
                    .setCode(R.FAIL).setMsg("当前系统没有开启注册功能！").build();
        }
        if (!userService.checkUserNameUnique(sysUser))
        {
            return BoolResponse.newBuilder()
                    .setCode(R.FAIL).setMsg("保存用户'" + username + "'失败，注册账号已存在").build();
        }
        return BoolResponse.newBuilder()
                .setCode(R.SUCCESS).setData(userService.registerUser(sysUser)).build();
    }

    @Override
    public CompletableFuture<BoolResponse> registerUserInfoAsync(RegisterUserInfoRequest request) {
        return CompletableFuture.completedFuture(registerUserInfo(request));
    }

    @Override
    @InnerAuth
    public BoolResponse recordUserLogin(RecordUserLoginRequest request)
    {
        SysUser sysUser = SysUserConvert.toJava(request.getUser());
        return BoolResponse.newBuilder()
                .setCode(R.SUCCESS).setData(userService.updateLoginInfo(sysUser)).build();
    }

    @Override
    public CompletableFuture<BoolResponse> recordUserLoginAsync(RecordUserLoginRequest request) {
        return CompletableFuture.completedFuture(recordUserLogin(request));
    }
}
