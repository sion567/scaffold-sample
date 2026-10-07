package com.scaffold.system.api.convert;

import java.util.Arrays;
import java.util.HashSet;

import com.scaffold.system.api.domain.SysRole;
import com.scaffold.system.api.proto.SysRoleProto;

import static com.scaffold.system.api.convert.ProtoConverts.emptyToNull;
import static com.scaffold.system.api.convert.ProtoConverts.toDate;
import static com.scaffold.system.api.convert.ProtoConverts.toInteger;
import static com.scaffold.system.api.convert.ProtoConverts.toLong;
import static com.scaffold.system.api.convert.ProtoConverts.toMillis;

/**
 * SysRole <-> SysRoleProto 转换。
 *
 * @author ct
 */
public final class SysRoleConvert
{
    private SysRoleConvert()
    {
    }

    public static SysRoleProto toProto(SysRole role)
    {
        if (role == null)
        {
            return null;
        }
        SysRoleProto.Builder builder = SysRoleProto.newBuilder();
        if (role.getRoleId() != null)
        {
            builder.setRoleId(role.getRoleId());
        }
        if (role.getRoleName() != null)
        {
            builder.setRoleName(role.getRoleName());
        }
        if (role.getRoleKey() != null)
        {
            builder.setRoleKey(role.getRoleKey());
        }
        if (role.getRoleSort() != null)
        {
            builder.setRoleSort(role.getRoleSort());
        }
        if (role.getDataScope() != null)
        {
            builder.setDataScope(role.getDataScope());
        }
        builder.setMenuCheckStrictly(role.isMenuCheckStrictly());
        builder.setDeptCheckStrictly(role.isDeptCheckStrictly());
        if (role.getStatus() != null)
        {
            builder.setStatus(role.getStatus());
        }
        if (role.getDelFlag() != null)
        {
            builder.setDelFlag(role.getDelFlag());
        }
        builder.setFlag(role.isFlag());
        if (role.getMenuIds() != null)
        {
            builder.addAllMenuIds(Arrays.asList(role.getMenuIds()));
        }
        if (role.getDeptIds() != null)
        {
            builder.addAllDeptIds(Arrays.asList(role.getDeptIds()));
        }
        if (role.getPermissions() != null)
        {
            builder.addAllPermissions(role.getPermissions());
        }
        if (role.getCreateBy() != null)
        {
            builder.setCreateBy(role.getCreateBy());
        }
        if (role.getCreateTime() != null)
        {
            builder.setCreateTime(toMillis(role.getCreateTime()));
        }
        if (role.getUpdateBy() != null)
        {
            builder.setUpdateBy(role.getUpdateBy());
        }
        if (role.getUpdateTime() != null)
        {
            builder.setUpdateTime(toMillis(role.getUpdateTime()));
        }
        if (role.getRemark() != null)
        {
            builder.setRemark(role.getRemark());
        }
        if (role.getVersion() != null)
        {
            builder.setVersion(role.getVersion());
        }
        return builder.build();
    }

    public static SysRole toJava(SysRoleProto proto)
    {
        if (proto == null)
        {
            return null;
        }
        SysRole role = new SysRole();
        role.setRoleId(toLong(proto.getRoleId()));
        role.setRoleName(emptyToNull(proto.getRoleName()));
        role.setRoleKey(emptyToNull(proto.getRoleKey()));
        role.setRoleSort(toInteger(proto.getRoleSort()));
        role.setDataScope(emptyToNull(proto.getDataScope()));
        role.setMenuCheckStrictly(proto.getMenuCheckStrictly());
        role.setDeptCheckStrictly(proto.getDeptCheckStrictly());
        role.setStatus(emptyToNull(proto.getStatus()));
        role.setDelFlag(emptyToNull(proto.getDelFlag()));
        role.setFlag(proto.getFlag());
        role.setMenuIds(proto.getMenuIdsList().toArray(new Long[0]));
        role.setDeptIds(proto.getDeptIdsList().toArray(new Long[0]));
        role.setPermissions(new HashSet<>(proto.getPermissionsList()));
        role.setCreateBy(emptyToNull(proto.getCreateBy()));
        role.setCreateTime(toDate(proto.getCreateTime()));
        role.setUpdateBy(emptyToNull(proto.getUpdateBy()));
        role.setUpdateTime(toDate(proto.getUpdateTime()));
        role.setRemark(emptyToNull(proto.getRemark()));
        role.setVersion(toInteger(proto.getVersion()));
        return role;
    }
}
