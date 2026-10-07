package com.scaffold.system.api.convert;

import java.util.ArrayList;
import java.util.Arrays;

import com.scaffold.system.api.domain.SysUser;
import com.scaffold.system.api.proto.SysUserProto;

import static com.scaffold.system.api.convert.ProtoConverts.emptyToNull;
import static com.scaffold.system.api.convert.ProtoConverts.toDate;
import static com.scaffold.system.api.convert.ProtoConverts.toInteger;
import static com.scaffold.system.api.convert.ProtoConverts.toLong;
import static com.scaffold.system.api.convert.ProtoConverts.toMillis;

/**
 * SysUser <-> SysUserProto 转换。
 *
 * @author ct
 */
public final class SysUserConvert
{
    private SysUserConvert()
    {
    }

    public static SysUserProto toProto(SysUser user)
    {
        if (user == null)
        {
            return null;
        }
        SysUserProto.Builder builder = SysUserProto.newBuilder();
        if (user.getUserId() != null)
        {
            builder.setUserId(user.getUserId());
        }
        if (user.getDeptId() != null)
        {
            builder.setDeptId(user.getDeptId());
        }
        if (user.getUserName() != null)
        {
            builder.setUserName(user.getUserName());
        }
        if (user.getNickName() != null)
        {
            builder.setNickName(user.getNickName());
        }
        if (user.getEmail() != null)
        {
            builder.setEmail(user.getEmail());
        }
        if (user.getPhonenumber() != null)
        {
            builder.setPhonenumber(user.getPhonenumber());
        }
        if (user.getSex() != null)
        {
            builder.setSex(user.getSex());
        }
        if (user.getAvatar() != null)
        {
            builder.setAvatar(user.getAvatar());
        }
        if (user.getPassword() != null)
        {
            builder.setPassword(user.getPassword());
        }
        if (user.getStatus() != null)
        {
            builder.setStatus(user.getStatus());
        }
        if (user.getDelFlag() != null)
        {
            builder.setDelFlag(user.getDelFlag());
        }
        if (user.getLoginIp() != null)
        {
            builder.setLoginIp(user.getLoginIp());
        }
        if (user.getLoginDate() != null)
        {
            builder.setLoginDate(toMillis(user.getLoginDate()));
        }
        if (user.getPwdUpdateDate() != null)
        {
            builder.setPwdUpdateDate(toMillis(user.getPwdUpdateDate()));
        }
        if (user.getDept() != null)
        {
            builder.setDept(SysDeptConvert.toProto(user.getDept()));
        }
        if (user.getRoles() != null)
        {
            user.getRoles().forEach(role -> builder.addRoles(SysRoleConvert.toProto(role)));
        }
        if (user.getRoleIds() != null)
        {
            builder.addAllRoleIds(Arrays.asList(user.getRoleIds()));
        }
        if (user.getPostIds() != null)
        {
            builder.addAllPostIds(Arrays.asList(user.getPostIds()));
        }
        if (user.getRoleId() != null)
        {
            builder.setRoleId(user.getRoleId());
        }
        if (user.getCreateBy() != null)
        {
            builder.setCreateBy(user.getCreateBy());
        }
        if (user.getCreateTime() != null)
        {
            builder.setCreateTime(toMillis(user.getCreateTime()));
        }
        if (user.getUpdateBy() != null)
        {
            builder.setUpdateBy(user.getUpdateBy());
        }
        if (user.getUpdateTime() != null)
        {
            builder.setUpdateTime(toMillis(user.getUpdateTime()));
        }
        if (user.getRemark() != null)
        {
            builder.setRemark(user.getRemark());
        }
        if (user.getVersion() != null)
        {
            builder.setVersion(user.getVersion());
        }
        return builder.build();
    }

    public static SysUser toJava(SysUserProto proto)
    {
        if (proto == null)
        {
            return null;
        }
        SysUser user = new SysUser();
        user.setUserId(toLong(proto.getUserId()));
        user.setDeptId(toLong(proto.getDeptId()));
        user.setUserName(emptyToNull(proto.getUserName()));
        user.setNickName(emptyToNull(proto.getNickName()));
        user.setEmail(emptyToNull(proto.getEmail()));
        user.setPhonenumber(emptyToNull(proto.getPhonenumber()));
        user.setSex(emptyToNull(proto.getSex()));
        user.setAvatar(emptyToNull(proto.getAvatar()));
        user.setPassword(emptyToNull(proto.getPassword()));
        user.setStatus(emptyToNull(proto.getStatus()));
        user.setDelFlag(emptyToNull(proto.getDelFlag()));
        user.setLoginIp(emptyToNull(proto.getLoginIp()));
        user.setLoginDate(toDate(proto.getLoginDate()));
        user.setPwdUpdateDate(toDate(proto.getPwdUpdateDate()));
        user.setDept(SysDeptConvert.toJava(proto.hasDept() ? proto.getDept() : null));
        user.setRoles(new ArrayList<>());
        for (com.scaffold.system.api.proto.SysRoleProto roleProto : proto.getRolesList())
        {
            user.getRoles().add(SysRoleConvert.toJava(roleProto));
        }
        user.setRoleIds(proto.getRoleIdsList().toArray(new Long[0]));
        user.setPostIds(proto.getPostIdsList().toArray(new Long[0]));
        user.setRoleId(toLong(proto.getRoleId()));
        user.setCreateBy(emptyToNull(proto.getCreateBy()));
        user.setCreateTime(toDate(proto.getCreateTime()));
        user.setUpdateBy(emptyToNull(proto.getUpdateBy()));
        user.setUpdateTime(toDate(proto.getUpdateTime()));
        user.setRemark(emptyToNull(proto.getRemark()));
        user.setVersion(toInteger(proto.getVersion()));
        return user;
    }
}
