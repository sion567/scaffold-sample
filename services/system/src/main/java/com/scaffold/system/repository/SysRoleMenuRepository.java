package com.scaffold.system.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.scaffold.common.core.jpa.ScaffoldRepository;
import com.scaffold.system.domain.SysRoleMenu;

/**
 * SysRoleMenu 数据访问。
 *
 * @author scaffold
 */
@Repository
public interface SysRoleMenuRepository extends ScaffoldRepository<SysRoleMenu, com.scaffold.system.domain.SysRoleMenuId>
{
    /** 菜单被角色引用数（checkMenuExistRole） */
    long countByMenuId(Long menuId);

    /** 角色已勾选菜单ID（selectMenuIdByRoleId） */
    @Query("select rm.menuId from SysRoleMenu rm where rm.roleId = :roleId")
    List<Long> selectMenuIdsByRoleId(@Param("roleId") Long roleId);

    /** 重建关联前清理（deleteRoleMenuByRoleId） */
    @Transactional
    @Modifying
    @Query("delete from SysRoleMenu rm where rm.roleId = :roleId")
    int deleteByRoleId(@Param("roleId") Long roleId);

    /** 删除角色时清理（deleteRoleMenu） */
    @Transactional
    @Modifying
    @Query("delete from SysRoleMenu rm where rm.roleId in :roleIds")
    int deleteByRoleIds(@Param("roleIds") List<Long> roleIds);
}
