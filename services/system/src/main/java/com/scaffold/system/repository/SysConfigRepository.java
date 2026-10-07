package com.scaffold.system.repository;

import org.springframework.stereotype.Repository;

import com.scaffold.common.core.jpa.ScaffoldRepository;
import com.scaffold.system.domain.SysConfig;

/**
 * SysConfig 数据访问（泛型基接口派生：CRUD + Specification 动态条件）。
 *
 * @author scaffold
 */
@Repository
public interface SysConfigRepository extends ScaffoldRepository<SysConfig, Long>
{
    /** 按参数键取配置（唯一键） */
    java.util.Optional<SysConfig> findByConfigKey(String configKey);
}
