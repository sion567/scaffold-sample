package com.scaffold.system.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.scaffold.common.core.jpa.ScaffoldRepository;
import com.scaffold.system.api.domain.SysUser;

/**
 * SysUser 数据访问（泛型基接口派生：CRUD + Specification 动态条件）。
 *
 * @author scaffold
 */
@Repository
public interface SysUserRepository extends ScaffoldRepository<SysUser, Long>
{
    /** 部门在编用户数（checkDeptExistUser） */
    long countByDeptIdAndDelFlag(Long deptId, String delFlag);

    /** 按账号取用户（唯一键） */
    java.util.Optional<SysUser> findByUserName(String userName);

    /** 按账号取在编用户（selectUserByUserName） */
    java.util.Optional<SysUser> findByUserNameAndDelFlag(String userName, String delFlag);

    /** 手机号唯一校验（checkPhoneUnique） */
    java.util.Optional<SysUser> findByPhonenumberAndDelFlag(String phonenumber, String delFlag);

    /** 邮箱唯一校验（checkEmailUnique） */
    java.util.Optional<SysUser> findByEmailAndDelFlag(String email, String delFlag);

    /** 用户角色（selectRolesByUserName） */
    @org.springframework.data.jpa.repository.Query("select r from SysRole r join SysUserRole ur on ur.roleId = r.roleId "
         + "join SysUser u on u.userId = ur.userId where u.userName = :userName and r.delFlag = '0'")
    java.util.List<com.scaffold.system.api.domain.SysRole> selectRolesByUserName(@org.springframework.data.repository.query.Param("userName") String userName);

    /** 修改状态（updateUserStatus） */
    @Transactional
    @Modifying
    @Query("update SysUser u set u.status = :status where u.userId = :userId")
    int updateStatus(@Param("userId") Long userId, @Param("status") String status);

    /** 修改头像（updateUserAvatar） */
    @Transactional
    @Modifying
    @Query("update SysUser u set u.avatar = :avatar where u.userId = :userId")
    int updateAvatar(@Param("userId") Long userId, @Param("avatar") String avatar);

    /** 更新登录信息（updateLoginInfo） */
    @Transactional
    @Modifying
    @Query("update SysUser u set u.loginIp = :loginIp, u.loginDate = :loginDate where u.userId = :userId")
    int updateLoginInfo(@Param("userId") Long userId, @Param("loginIp") String loginIp, @Param("loginDate") java.util.Date loginDate);

    /** 重置密码（resetUserPwd） */
    @Transactional
    @Modifying
    @Query("update SysUser u set u.password = :password, u.pwdUpdateDate = CURRENT_TIMESTAMP where u.userId = :userId")
    int resetPassword(@Param("userId") Long userId, @Param("password") String password);

    /** 用户目录（selectDirectoryByIds，构造器投影） */
    @Query("select new com.scaffold.system.api.UserDirectoryItem(u.userId, u.userName, u.nickName) "
         + "from SysUser u where u.delFlag = '0' and u.status = '0' and u.userId in :ids order by u.userId")
    List<com.scaffold.system.api.UserDirectoryItem> selectDirectoryByIds(@Param("ids") Collection<Long> ids);

    /** 按角色键取用户目录（selectDirectoryByRoleKeys） */
    @Query("select distinct new com.scaffold.system.api.UserDirectoryItem(u.userId, u.userName, u.nickName) "
         + "from SysUser u join SysUserRole ur on ur.userId = u.userId "
         + "join SysRole r on r.roleId = ur.roleId "
         + "where u.delFlag = '0' and u.status = '0' and r.status = '0' and r.delFlag = '0' "
         + "and r.roleKey in :roleKeys order by u.userId")
    List<com.scaffold.system.api.UserDirectoryItem> selectDirectoryByRoleKeys(@Param("roleKeys") Collection<String> roleKeys);

    /** 全量用户目录（selectDirectoryAll） */
    @Query("select new com.scaffold.system.api.UserDirectoryItem(u.userId, u.userName, u.nickName) "
         + "from SysUser u where u.delFlag = '0' and u.status = '0' order by u.userId")
    List<com.scaffold.system.api.UserDirectoryItem> selectDirectoryAll();

    /** 内置管理员补全（initAdminAccount：仅补空值，不覆盖已有） */
    @Transactional
    @Modifying
    @Query("update SysUser u set u.password = coalesce(u.password, :password), "
         + "u.phonenumber = coalesce(u.phonenumber, :phonenumber), "
         + "u.pwdUpdateDate = case when u.password is null then CURRENT_TIMESTAMP else u.pwdUpdateDate end, "
         + "u.updateTime = CURRENT_TIMESTAMP "
         + "where u.userName = :userName and u.delFlag = '0'")
    int initAdminAccount(@Param("userName") String userName, @Param("password") String password,
            @Param("phonenumber") String phonenumber);

    /** 软删除（deleteUserById：del_flag='2'） */
    @Transactional
    @Modifying
    @Query("update SysUser u set u.delFlag = '2' where u.userId = :userId")
    int softDeleteById(@Param("userId") Long userId);

    /** 批量软删除（deleteUserByIds） */
    @Transactional
    @Modifying
    @Query("update SysUser u set u.delFlag = '2' where u.userId in :userIds")
    int softDeleteByIds(@Param("userIds") Collection<Long> userIds);

    /** 部门ID → 部门信息批量回填（selectUserList 的 dept 联表展示） */
    @org.springframework.data.jpa.repository.Query("select d from SysDept d where d.deptId in :deptIds")
    java.util.List<com.scaffold.system.api.domain.SysDept> findDepts(@Param("deptIds") Collection<Long> deptIds);
}
