package com.scaffold.audit.repository;

import org.springframework.stereotype.Repository;

import com.scaffold.common.core.jpa.ScaffoldRepository;
import com.scaffold.system.api.domain.QueryAuditLog;

/**
 * 查询审计日志数据访问（query_audit_log，仅追加）。
 *
 * @author ct
 */
@Repository
public interface QueryAuditRepository extends ScaffoldRepository<QueryAuditLog, Long>
{
}
