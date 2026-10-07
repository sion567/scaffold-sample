package com.scaffold.audit.task;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.scaffold.audit.repository.AuditTraceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 留痕水位任务测试（冷热分层触发门）。
 *
 * @author ct
 */
@ExtendWith(MockitoExtension.class)
class TraceWaterLevelTaskTest {
  @Mock private AuditTraceRepository traceRepository;

  @InjectMocks private TraceWaterLevelTask task;

  @Test
  @DisplayName("未超阈值：INFO 口径")
  void under_threshold() {
    when(traceRepository.count()).thenReturn(100L);
    assertDoesNotThrow(task::stats);
  }

  @Test
  @DisplayName("超阈值：WARN 提醒启动冷热分层")
  void over_threshold() {
    when(traceRepository.count()).thenReturn(task.threshold() + 1);
    assertDoesNotThrow(task::stats);
    assertEquals(5_000_000L, task.threshold());
  }
}
