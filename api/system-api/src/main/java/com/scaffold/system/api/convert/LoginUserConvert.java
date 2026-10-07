package com.scaffold.system.api.convert;

import com.scaffold.system.api.model.LoginUser;
import com.scaffold.system.api.proto.LoginUserProto;

import static com.scaffold.system.api.convert.ProtoConverts.emptyToNull;
import static com.scaffold.system.api.convert.ProtoConverts.toLong;

/**
 * LoginUser <-> LoginUserProto 转换。
 *
 * @author ct
 */
public final class LoginUserConvert
{
    private LoginUserConvert()
    {
    }

    public static LoginUserProto toProto(LoginUser loginUser)
    {
        if (loginUser == null)
        {
            return null;
        }
        LoginUserProto.Builder builder = LoginUserProto.newBuilder();
        if (loginUser.getToken() != null)
        {
            builder.setToken(loginUser.getToken());
        }
        if (loginUser.getUserid() != null)
        {
            builder.setUserid(loginUser.getUserid());
        }
        if (loginUser.getUsername() != null)
        {
            builder.setUsername(loginUser.getUsername());
        }
        if (loginUser.getLoginTime() != null)
        {
            builder.setLoginTime(loginUser.getLoginTime());
        }
        if (loginUser.getExpireTime() != null)
        {
            builder.setExpireTime(loginUser.getExpireTime());
        }
        if (loginUser.getIpaddr() != null)
        {
            builder.setIpaddr(loginUser.getIpaddr());
        }
        if (loginUser.getPermissions() != null)
        {
            builder.addAllPermissions(loginUser.getPermissions());
        }
        if (loginUser.getRoles() != null)
        {
            builder.addAllRoles(loginUser.getRoles());
        }
        if (loginUser.getSysUser() != null)
        {
            builder.setSysUser(SysUserConvert.toProto(loginUser.getSysUser()));
        }
        return builder.build();
    }

    public static LoginUser toJava(LoginUserProto proto)
    {
        if (proto == null)
        {
            return null;
        }
        LoginUser loginUser = new LoginUser();
        loginUser.setToken(emptyToNull(proto.getToken()));
        loginUser.setUserid(toLong(proto.getUserid()));
        loginUser.setUsername(emptyToNull(proto.getUsername()));
        loginUser.setLoginTime(proto.getLoginTime() == 0L ? null : proto.getLoginTime());
        loginUser.setExpireTime(proto.getExpireTime() == 0L ? null : proto.getExpireTime());
        loginUser.setIpaddr(emptyToNull(proto.getIpaddr()));
        loginUser.setPermissions(new java.util.HashSet<>(proto.getPermissionsList()));
        loginUser.setRoles(new java.util.HashSet<>(proto.getRolesList()));
        loginUser.setSysUser(SysUserConvert.toJava(proto.hasSysUser() ? proto.getSysUser() : null));
        return loginUser;
    }
}
