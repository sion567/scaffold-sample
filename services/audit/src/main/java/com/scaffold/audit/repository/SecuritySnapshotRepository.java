package com.scaffold.audit.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.scaffold.audit.domain.AuditTrace;
import com.scaffold.common.core.jpa.ScaffoldRepository;

/**
 * 安全快照只读查询（system 域只读授权表：sys_user_role/sys_role/sys_config）。
 *
 * <p>这三张表归属 system 服务，audit 库中为只读副本，本服务不持有对应实体——
 * 跨域只读查询走原生 SQL（可移植写法、无方言分支），列别名与投影接口字段对齐。
 * 挂在 {@link AuditTrace} 实体名下仅为提供 repository 载体，SQL 不涉及该实体。</p>
 *
 * @author ct
 */
@Repository
public interface SecuritySnapshotRepository extends ScaffoldRepository<AuditTrace, Long>
{
    /** 权限快照：某用户当前角色 */
    @Query(value = """
            select r.role_id as "roleId", r.role_name as "roleName", r.role_key as "roleKey",
                   r.status as "status"
            from sys_user_role ur
            left join sys_role r on r.role_id = ur.role_id
            where ur.user_id = :userId
              and r.del_flag = '0'
            """, nativeQuery = true)
    List<UserRoleStat> selectUserRoles(@Param("userId") Long userId);

    /** 安全配置快照（密码策略等 sys_config） */
    @Query(value = """
            select config_name as "configName", config_key as "configKey", config_value as "configValue"
            from sys_config
            where config_type = 'Y'
            order by config_id
            """, nativeQuery = true)
    List<SecurityConfigItem> selectSecurityConfig();

    /** 权限快照行投影 */
    interface UserRoleStat
    {
        Long getRoleId();

        String getRoleName();

        String getRoleKey();

        String getStatus();
    }

    /** 安全配置行投影 */
    interface SecurityConfigItem
    {
        String getConfigName();

        String getConfigKey();

        String getConfigValue();
    }
}
