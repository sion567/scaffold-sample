package com.scaffold.system.domain;

import java.io.Serializable;
import java.util.Objects;

/**
 * SysRoleMenu 复合主键（roleId + menuId）。
 *
 * @author scaffold
 */
public class SysRoleMenuId implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Long roleId;

    private Long menuId;

    public SysRoleMenuId()
    {
    }

    public SysRoleMenuId(Long roleId, Long menuId)
    {
        this.roleId = roleId;
        this.menuId = menuId;
    }

    public Long getRoleId()
    {
        return roleId;
    }

    public Long getMenuId()
    {
        return menuId;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof SysRoleMenuId that))
        {
            return false;
        }
        return Objects.equals(roleId, that.roleId) && Objects.equals(menuId, that.menuId);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(roleId, menuId);
    }
}
