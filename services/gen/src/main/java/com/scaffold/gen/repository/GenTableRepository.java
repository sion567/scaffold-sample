package com.scaffold.gen.repository;

import org.springframework.stereotype.Repository;

import com.scaffold.common.core.jpa.ScaffoldRepository;
import com.scaffold.gen.domain.GenTable;

/**
 * GenTable 数据访问（CRUD + Specification 动态条件；逆向目录查询走 GenCatalogDao）。
 *
 * @author ct
 */
@Repository
public interface GenTableRepository extends ScaffoldRepository<GenTable, Long>
{
    /** 按表名取业务表配置 */
    GenTable findByTableName(String tableName);

}
