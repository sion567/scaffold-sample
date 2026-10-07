package com.scaffold.system.api.domain;

import java.io.Serializable;
import java.util.Objects;

/**
 * SysRoleDept 复合主键（roleId + deptId）。
 *
 * @author scaffold
 */
public class SysRoleDeptId implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Long roleId;

    private Long deptId;

    public SysRoleDeptId()
    {
    }

    public SysRoleDeptId(Long roleId, Long deptId)
    {
        this.roleId = roleId;
        this.deptId = deptId;
    }

    public Long getRoleId()
    {
        return roleId;
    }

    public Long getDeptId()
    {
        return deptId;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof SysRoleDeptId that))
        {
            return false;
        }
        return Objects.equals(roleId, that.roleId) && Objects.equals(deptId, that.deptId);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(roleId, deptId);
    }
}
