package com.scaffold.job.repository;

import org.springframework.stereotype.Repository;

import com.scaffold.common.core.jpa.ScaffoldRepository;
import com.scaffold.job.domain.SysJob;

/**
 * SysJob 数据访问（泛型基接口派生：CRUD + Specification 动态条件）。
 *
 * @author ct
 */
@Repository
public interface SysJobRepository extends ScaffoldRepository<SysJob, Long>
{
}
