package com.scaffold.system.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.scaffold.common.core.jpa.ScaffoldRepository;
import com.scaffold.system.api.domain.SysRole;

/**
 * SysRole 数据访问。
 *
 * @author scaffold
 */
@Repository
public interface SysRoleRepository extends ScaffoldRepository<SysRole, Long>
{
    /** 唯一校验 */
    Optional<SysRole> findByRoleName(String roleName);

    /** 唯一校验 */
    Optional<SysRole> findByRoleKey(String roleKey);

    /** 用户拥有的角色（selectRolePermissionByUserId） */
    @Query("select r from SysRole r join SysUserRole ur on ur.roleId = r.roleId "
         + "join SysUser u on u.userId = ur.userId "
         + "where u.userId = :userId and r.delFlag = '0'")
    List<SysRole> selectRolePermissionByUserId(@Param("userId") Long userId);

    /** 软删除（deleteRoleById：del_flag='2'） */
    @Modifying
    @Query("update SysRole r set r.delFlag = '2' where r.roleId = :roleId")
    int softDeleteById(@Param("roleId") Long roleId);

    /** 批量软删除（deleteRoleByIds） */
    @Modifying
    @Query("update SysRole r set r.delFlag = '2' where r.roleId in :roleIds")
    int softDeleteByIds(@Param("roleIds") Collection<Long> roleIds);
}
