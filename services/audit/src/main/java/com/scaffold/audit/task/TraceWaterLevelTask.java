package com.scaffold.audit.task;

import com.scaffold.audit.repository.AuditTraceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 留痕水位观测（P2 留痕冷热分层触发门：audit_trace 行数超阈值提醒启动归档切换）。
 *
 * <p>归档载体已就绪（ct-job V3 归档表 + LogArchiveTask），缺的只是"触发门可见"—— 本任务定期报水位，超阈值
 * WARN。默认关闭（audit.trace.watermark.enabled=true 开启）。
 *
 * @author ct
 */
@Component
@ConditionalOnProperty(value = "audit.trace.watermark.enabled", havingValue = "true")
public class TraceWaterLevelTask {
  private static final Logger log = LoggerFactory.getLogger(TraceWaterLevelTask.class);

  @Autowired private AuditTraceRepository traceRepository;

  @Scheduled(fixedDelayString = "${audit.trace.watermark.interval-ms:86400000}")
  public void stats() {
    long rows = traceRepository.count();
    long threshold = threshold();
    if (rows > threshold) {
      log.warn("[留痕水位] audit_trace 行数 {} 已超阈值 {}——启动留痕冷热分层（历史行转 ct-job 归档表）", rows, threshold);
    } else {
      log.info("[留痕水位] audit_trace 当前行数 {}（阈值 {}）", rows, threshold);
    }
  }

  long threshold() {
    return 5_000_000L;
  }
}
