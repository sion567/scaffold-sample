package com.scaffold.system.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.scaffold.common.core.jpa.ScaffoldRepository;
import com.scaffold.system.domain.SysUserRole;

/**
 * SysUserRole 数据访问。
 *
 * @author scaffold
 */
@Repository
public interface SysUserRoleRepository extends ScaffoldRepository<SysUserRole, com.scaffold.system.domain.SysUserRoleId>
{
    /** 用户已绑定角色ID（selectRoleIdsByUserId） */
    @Query("select ur.roleId from SysUserRole ur where ur.userId = :userId")
    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);

    /** 角色被引用数（countUserRoleByRoleId） */
    long countByRoleId(Long roleId);

    /** 注销清理（deleteUserRoleByUserId） */
    @Modifying
    @Query("delete from SysUserRole ur where ur.userId = :userId")
    int deleteByUserId(@Param("userId") Long userId);

    /** 批量删除用户关联（deleteUserRole） */
    @Modifying
    @Query("delete from SysUserRole ur where ur.userId in :userIds")
    int deleteByUserIdIn(@Param("userIds") List<Long> userIds);

    /** 批量取消授权（deleteUserRoleInfos） */
    @Modifying
    @Query("delete from SysUserRole ur where ur.roleId = :roleId and ur.userId in :userIds")
    int deleteByRoleIdAndUserIds(@Param("roleId") Long roleId, @Param("userIds") List<Long> userIds);
}
