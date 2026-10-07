package com.scaffold.system.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.scaffold.common.core.jpa.ScaffoldRepository;
import com.scaffold.system.api.domain.SysRoleDept;

/**
 * SysRoleDept 数据访问。
 *
 * @author scaffold
 */
@Repository
public interface SysRoleDeptRepository extends ScaffoldRepository<SysRoleDept, com.scaffold.system.api.domain.SysRoleDeptId>
{
    /** 角色已勾选部门ID（selectDeptIdByRoleId） */
    @Query("select rd.deptId from SysRoleDept rd where rd.roleId = :roleId")
    List<Long> selectDeptIdsByRoleId(@Param("roleId") Long roleId);

    /** 重建关联前清理（deleteRoleDeptByRoleId） */
    @Transactional
    @Modifying
    @Query("delete from SysRoleDept rd where rd.roleId = :roleId")
    int deleteByRoleId(@Param("roleId") Long roleId);

    /** 删除角色时清理（deleteRoleDept） */
    @Transactional
    @Modifying
    @Query("delete from SysRoleDept rd where rd.roleId in :roleIds")
    int deleteByRoleIds(@Param("roleIds") List<Long> roleIds);
}
