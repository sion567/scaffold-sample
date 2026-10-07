package com.scaffold.audit.task;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import com.scaffold.audit.domain.AuditTrace;
import com.scaffold.audit.repository.AuditTraceRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 链巡检任务测试（游标分批遍历 + 断链统计，边界：空表/末批不满/断链）。
 *
 * @author ct
 */
@ExtendWith(MockitoExtension.class)
class TraceChainIntegrityJobTest {
  @Mock private AuditTraceRepository traceRepository;

  @InjectMocks private TraceChainIntegrityJob job;

  private AuditTrace row(long id, String prev, String curr) {
    AuditTrace t = new AuditTrace();
    t.setTraceId(id);
    t.setPrevDigest(prev);
    t.setCurrDigest(curr);
    return t;
  }

  /** 内存表：trace_id 升序、prev=前一条 curr 的完整链 */
  private List<AuditTrace> linked(long fromId, int count) {
    List<AuditTrace> all = new ArrayList<>();
    String prev = "";
    for (int i = 0; i < count; i++) {
      String curr = "d" + (fromId + i);
      all.add(row(fromId + i, prev, curr));
      prev = curr;
    }
    return all;
  }

  private void mockPages(List<AuditTrace> all) {
    when(traceRepository.findByTraceIdGreaterThan(anyLong(), any(Pageable.class)))
        .thenAnswer(
            inv -> {
              long afterId = inv.getArgument(0);
              Pageable pageable = inv.getArgument(1);
              int limit = pageable.getPageSize();
              List<AuditTrace> page = new ArrayList<>();
              for (AuditTrace t : all) {
                if (t.getTraceId() > afterId) {
                  page.add(t);
                  if (page.size() >= limit) {
                    break;
                  }
                }
              }
              return new PageImpl<>(page, PageRequest.of(0, limit, Sort.unsorted()), all.size());
            });
  }

  @Test
  @DisplayName("游标分批：末批不满即终止（12000 行 / 5000 批 = 3 页）")
  void cursorPagination() {
    List<AuditTrace> all = linked(1, TraceChainIntegrityJob.BATCH * 2 + 2000);
    mockPages(all);
    assertDoesNotThrow(job::check);
  }

  @Test
  @DisplayName("空表：首查即空，静默跳过")
  void emptyTable() {
    when(traceRepository.findByTraceIdGreaterThan(anyLong(), any(Pageable.class)))
        .thenReturn(new PageImpl<>(new ArrayList<>(), PageRequest.of(0, 1, Sort.unsorted()), 0L));
    assertDoesNotThrow(job::check);
  }

  @Test
  @DisplayName("断链：中间行 prev 与前一条 curr 不符（检测删除/篡改）")
  void brokenChain() {
    List<AuditTrace> all = linked(1, 3);
    all.get(1).setPrevDigest("tampered");
    mockPages(all);
    assertDoesNotThrow(job::check);
  }

  // ─── 变异测试补强：巡检结论必须落到日志（计数/断链定位条数/空表口径） ───

  private ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> attach() {
    ch.qos.logback.classic.Logger logger =
        (ch.qos.logback.classic.Logger)
            org.slf4j.LoggerFactory.getLogger(TraceChainIntegrityJob.class);
    ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> appender =
        new ch.qos.logback.core.read.ListAppender<>();
    appender.start();
    logger.addAppender(appender);
    return appender;
  }

  private void detach(
      ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> appender) {
    ((ch.qos.logback.classic.Logger)
            org.slf4j.LoggerFactory.getLogger(TraceChainIntegrityJob.class))
        .detachAppender(appender);
  }

  private long countOf(
      ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> appender,
      String fragment) {
    return appender.list.stream().filter(e -> e.getFormattedMessage().contains(fragment)).count();
  }

  @Test
  @DisplayName("巡检计数：12000 行三页遍历，完整链落「12000 条留痕哈希链完整」")
  void cursorPagination_reportsTotal() {
    List<AuditTrace> all = linked(1, TraceChainIntegrityJob.BATCH * 2 + 2000);
    mockPages(all);
    var appender = attach();
    try {
      job.check();
      assertEquals(1, countOf(appender, (TraceChainIntegrityJob.BATCH * 2 + 2000) + " 条留痕哈希链完整"));
    } finally {
      detach(appender);
    }
  }

  @Test
  @DisplayName("断链口径：4 处断链报「发现 4 处」，逐条定位只打前 3 条")
  void brokenChain_reportsCountAndCapsDetails() {
    List<AuditTrace> all = linked(1, 6);
    all.get(1).setPrevDigest("tampered-1");
    all.get(3).setPrevDigest("tampered-2");
    all.get(4).setPrevDigest("tampered-3");
    all.get(5).setPrevDigest("tampered-4");
    mockPages(all);
    var appender = attach();
    try {
      job.check();
      assertEquals(1, countOf(appender, "发现 4 处断链"));
      assertEquals(3, countOf(appender, "断链："), "逐条定位最多打 3 条");
    } finally {
      detach(appender);
    }
  }

  @Test
  @DisplayName("完整链：不断链时只报完整结论，不报发现断链")
  void intactChain_reportsOkOnly() {
    List<AuditTrace> all = linked(1, 3);
    mockPages(all);
    var appender = attach();
    try {
      job.check();
      assertEquals(1, countOf(appender, "3 条留痕哈希链完整"));
      assertEquals(0, countOf(appender, "断链"));
      assertEquals(0, countOf(appender, "无留痕数据"));
    } finally {
      detach(appender);
    }
  }

  @Test
  @DisplayName("空表：报「无留痕数据，跳过」")
  void emptyTable_reportsSkip() {
    when(traceRepository.findByTraceIdGreaterThan(anyLong(), any(Pageable.class)))
        .thenReturn(new PageImpl<>(new ArrayList<>(), PageRequest.of(0, 1, Sort.unsorted()), 0L));
    var appender = attach();
    try {
      job.check();
      assertEquals(1, countOf(appender, "无留痕数据"));
    } finally {
      detach(appender);
    }
  }
}
