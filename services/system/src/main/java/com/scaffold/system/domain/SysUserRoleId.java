package com.scaffold.system.domain;

import java.io.Serializable;
import java.util.Objects;

/**
 * SysUserRole 复合主键（userId + roleId）。
 *
 * @author scaffold
 */
public class SysUserRoleId implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Long userId;

    private Long roleId;

    public SysUserRoleId()
    {
    }

    public SysUserRoleId(Long userId, Long roleId)
    {
        this.userId = userId;
        this.roleId = roleId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public Long getRoleId()
    {
        return roleId;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof SysUserRoleId that))
        {
            return false;
        }
        return Objects.equals(userId, that.userId) && Objects.equals(roleId, that.roleId);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(userId, roleId);
    }
}
