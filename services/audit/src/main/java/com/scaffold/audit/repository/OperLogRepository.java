package com.scaffold.audit.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.scaffold.common.core.jpa.ScaffoldRepository;
import com.scaffold.system.api.domain.SysOperLog;

/**
 * 操作日志数据访问（audit_oper_log，仅追加）。
 *
 * @author ct
 */
@Repository
public interface OperLogRepository extends ScaffoldRepository<SysOperLog, Long>
{
    /**
     * 权限变更时间线（某用户的角色分配/权限变更/密码重置记录）。
     *
     * @param userId 用户ID
     * @param bizKeyPrefix 业务键前缀（"user:{userId}%"，由调用方拼接以保持跨方言 like 语义）
     */
    @Query("""
            select o from SysOperLog o
            where o.eventType in ('ROLE_ASSIGN', 'PERM_CHANGE', 'PWD_RESET', 'PWD_CHANGE')
              and (o.bizKey like :bizKeyPrefix or o.userId = :userId)
            order by o.operTime desc
            """)
    List<SysOperLog> selectPermissionChanges(@Param("userId") Long userId,
                                             @Param("bizKeyPrefix") String bizKeyPrefix);
}
