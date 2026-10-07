package com.scaffold.audit.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.scaffold.audit.domain.AuditTrace;
import com.scaffold.common.core.jpa.JpaSpecs;
import com.scaffold.common.test.H2JpaTest;
import java.util.Date;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;

/**
 * AuditTrace 数据访问切片测试（JPA + H2 Oracle 模式）。
 * 覆盖：应用侧雪花主键写入、SM3 链尾查询、动态条件追溯、Slice 游标分批。
 *
 * @author ct
 */
@H2JpaTest(
    ddl = "sql/audit/h2.sql",
    entityPackages = {"com.scaffold.audit.domain", "com.scaffold.system.api.domain"})
class AuditTraceRepositoryTest {

  private AuditTrace trace(long id, String currDigest, String prevDigest) {
    AuditTrace trace = new AuditTrace();
    trace.setTraceId(id);
    trace.setScene("指令下发");
    trace.setBizType("instruction");
    trace.setBizId(String.valueOf(id));
    trace.setOperator("admin");
    trace.setKeyAction("1");
    trace.setPrevDigest(prevDigest);
    trace.setCurrDigest(currDigest);
    trace.setOpTime(new Date());
    return trace;
  }

  @Test
  @DisplayName("空表：链尾摘要为 null（链头 prev 视为空串）")
  void selectLastDigest_empty(AuditTraceRepository repository) {
    assertNull(repository.selectLastDigest());
  }

  @Test
  @DisplayName("写入后链尾返回最新 curr_digest；仅追加不改历史")
  void insert_and_lastDigest(AuditTraceRepository repository) {
    repository.save(trace(990101L, "digest-aaa", null));
    repository.save(trace(990102L, "digest-bbb", "digest-aaa"));
    assertEquals("digest-bbb", repository.selectLastDigest());
  }

  @Test
  @DisplayName("追溯查询：按 bizId 精确命中，倒序截断")
  void list_by_biz_id(AuditTraceRepository repository) {
    repository.save(trace(9001L, "d1", ""));
    repository.save(trace(9002L, "d2", "d1"));

    var list =
        repository.list(
            JpaSpecs.eqIf("bizType", "instruction").and(JpaSpecs.eqIf("bizId", "9002")),
            Sort.by(Sort.Direction.DESC, "traceId"));
    assertEquals(1, list.size());
    assertEquals(Long.valueOf(9002L), list.get(0).getTraceId());

    assertTrue(
        repository.list(JpaSpecs.likeIf("scene", "指令"), Sort.unsorted()).size() >= 2);
    assertTrue(
        repository
            .list(JpaSpecs.likeIf("operator", "nobody-" + System.nanoTime()), Sort.unsorted())
            .isEmpty());
  }

  @Test
  @DisplayName("Slice 游标分批：免 count 推进到末尾")
  void chainPage_cursor(AuditTraceRepository repository) {
    // 游标从"本类其他用例已插入的最大 traceId"起步，只统计本用例的行（同一类共享一个 H2 库）
    var existing = repository.list(JpaSpecs.alwaysTrue(), Sort.by(Sort.Direction.DESC, "traceId"));
    long afterId = existing.isEmpty() ? 0L : existing.get(0).getTraceId();
    long firstId = afterId + 1;
    int inserted = 10;
    for (long id = firstId; id < firstId + inserted; id++) {
      repository.save(trace(id, "d" + id, ""));
    }
    int total = 0;
    while (true) {
      Slice<AuditTrace> slice =
          repository.findByTraceIdGreaterThan(
              afterId, PageRequest.of(0, 4, Sort.by(Sort.Direction.ASC, "traceId")));
      if (!slice.hasContent()) {
        break;
      }
      for (AuditTrace row : slice) {
        assertTrue(row.getTraceId() > afterId);
        afterId = row.getTraceId();
        total++;
      }
      if (!slice.hasNext()) {
        break;
      }
    }
    assertEquals(inserted, total);
  }
}
