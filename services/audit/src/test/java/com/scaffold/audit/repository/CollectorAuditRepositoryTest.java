package com.scaffold.audit.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.scaffold.common.test.H2JpaTest;
import com.scaffold.system.api.domain.CollectorAuditLog;
import com.scaffold.system.api.domain.QueryAuditLog;
import java.util.Date;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 采集留痕/查询审计数据访问切片测试（JPA + H2 Oracle 模式，仅追加写入）。
 *
 * @author ct
 */
@H2JpaTest(
    ddl = "sql/audit/h2.sql",
    entityPackages = {"com.scaffold.audit.domain", "com.scaffold.system.api.domain"})
class CollectorAuditRepositoryTest {

  @Test
  @DisplayName("采集留痕写入：应用侧主键，可回查")
  void insertCollectorAudit(CollectorAuditRepository repository) {
    CollectorAuditLog log = new CollectorAuditLog();
    log.setId(990003L);
    log.setDomain("tourist");
    log.setSource("PIRS_API");
    log.setChannelType("API");
    log.setStatus("0");
    log.setRecordCount(1000L);
    log.setCreateTime(new Date());

    repository.save(log);

    CollectorAuditLog loaded = repository.findById(990003L).orElseThrow();
    assertEquals("PIRS_API", loaded.getSource());
    assertEquals(Long.valueOf(1000L), loaded.getRecordCount());
    assertNotNull(loaded.getCreateTime());
  }

  @Test
  @DisplayName("查询审计写入：超长摘要截断到列宽内仍可写入回查")
  void insertQueryAudit(QueryAuditRepository repository) {
    QueryAuditLog log = new QueryAuditLog();
    log.setId(990004L);
    log.setCallerSystem("UI");
    log.setOperatorName("admin");
    log.setSubjectUid("uid-1");
    log.setQueryType("DETAIL");
    log.setRequestId("req-1");
    log.setResultSummary("姓名,证件号");
    log.setQueryTime(new Date());

    repository.save(log);

    QueryAuditLog loaded = repository.findById(990004L).orElseThrow();
    assertEquals("uid-1", loaded.getSubjectUid());
    assertEquals("姓名,证件号", loaded.getResultSummary());
  }
}
