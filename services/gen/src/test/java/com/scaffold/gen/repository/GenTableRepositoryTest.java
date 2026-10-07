package com.scaffold.gen.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.scaffold.common.core.jpa.JpaSpecs;
import com.scaffold.common.test.H2JpaTest;
import com.scaffold.gen.domain.GenTable;
import com.scaffold.gen.domain.GenTableColumn;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * GenTable/GenTableColumn 数据访问切片测试（JPA + H2，DDL 复用 Flyway H2 基线）。
 * 覆盖：IDENTITY 主键回填、动态条件、字段列表装载（对齐原 XML LEFT JOIN）。
 *
 * @author ct
 */
@H2JpaTest(
        ddl = "db/migration/h2/V1__init.sql",
        entityPackages = "com.scaffold.gen.domain",
        urlOptions = "INIT=CREATE SCHEMA IF NOT EXISTS GEN_DB\\;SET SCHEMA GEN_DB")
class GenTableRepositoryTest {

  @Test
  @DisplayName("新增业务表与字段：IDENTITY 回填主键，字段按 sort 升序装载")
  void insertWithColumns(GenTableRepository tableRepository, GenTableColumnRepository columnRepository) {
    GenTable table = new GenTable();
    table.setTableName("portable_tbl");
    table.setTableComment("可移植性回归表");
    table.setClassName("PortableTbl");
    table.setPackageName("com.demo");
    table.setModuleName("demo");
    table.setBusinessName("portable");
    table.setFunctionName("可移植性");
    table.setFunctionAuthor("ct");
    assertTrue(table.getTableId() == null);
    tableRepository.saveAndFlush(table);
    assertNotNull(table.getTableId(), "IDENTITY 应回填主键");

    for (int i = 1; i <= 2; i++) {
      GenTableColumn column = new GenTableColumn();
      column.setTableId(table.getTableId());
      column.setColumnName("col_" + i);
      column.setColumnComment("列" + i);
      column.setColumnType("varchar(64)");
      column.setJavaType("String");
      column.setJavaField("col" + i);
      column.setSort(i);
      columnRepository.saveAndFlush(column);
    }

    GenTable loaded = tableRepository.findById(table.getTableId()).orElseThrow();
    List<GenTableColumn> columns = columnRepository.findByTableIdOrderBySortAsc(loaded.getTableId());
    assertEquals(2, columns.size());
    assertTrue(columns.get(0).getSort() <= columns.get(1).getSort());

    GenTable query = new GenTable();
    query.setTableName("Portable_TBL");
    assertEquals(1, tableRepository.list(JpaSpecs.likeIf("tableName", query.getTableName()), org.springframework.data.domain.Sort.unsorted()).size());
  }
}
