package com.scaffold.audit.repository;

import org.springframework.stereotype.Repository;

import com.scaffold.common.core.jpa.ScaffoldRepository;
import com.scaffold.system.api.domain.CollectorAuditLog;

/**
 * 采集留痕日志数据访问（collector_audit_log，仅追加）。
 *
 * @author ct
 */
@Repository
public interface CollectorAuditRepository extends ScaffoldRepository<CollectorAuditLog, Long>
{
}
