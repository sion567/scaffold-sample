package com.scaffold.gen.repository;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.scaffold.common.core.jpa.ScaffoldRepository;
import com.scaffold.gen.domain.GenTableColumn;

/**
 * GenTableColumn 数据访问（CRUD；逆向目录查询走 GenCatalogDao）。
 *
 * @author ct
 */
@Repository
public interface GenTableColumnRepository extends ScaffoldRepository<GenTableColumn, Long>
{
    /** 按业务表取字段列表（ordinal_position/sort 升序） */
    List<GenTableColumn> findByTableIdOrderBySortAsc(Long tableId);

    /** 按业务表删除全部字段（同步库结构时先删后插的旧列清理） */
    void deleteByTableId(Long tableId);
}
