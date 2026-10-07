package com.scaffold.audit.task;

import com.scaffold.audit.domain.AuditTrace;
import com.scaffold.audit.repository.AuditTraceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 留痕哈希链完整性巡检（A12，密评核心证据）
 *
 * <p>链序=物理写入序（trace_id，雪花序≈时间序）：逐条校验 {@code 本条.prev_digest == 物理前一条.curr_digest}、首条 prev 为空串；
 * 业务时间（op_time）仅展示检索，不参与链序。断链=存在删除/篡改/乱序写， 立即 ERROR 告警（篡改可检测而非事后才发现）。
 *
 * <p>分批游标遍历（{@link Slice} 按 trace_id 游标分批，免 count 查询，防大表全量加载 OOM）；默认关闭
 * （audit.trace.chain-check.enabled=true 开启）；多实例仅一个开启。
 *
 * @author ct
 */
@Component
@ConditionalOnProperty(value = "audit.trace.chain-check.enabled", havingValue = "true")
public class TraceChainIntegrityJob {
  private static final Logger log = LoggerFactory.getLogger(TraceChainIntegrityJob.class);

  /** 游标批大小 */
  static final int BATCH = 5000;

  @Autowired private AuditTraceRepository traceRepository;

  @Scheduled(fixedDelayString = "${audit.trace.chain-check.interval-ms:86400000}")
  public void check() {
    long afterId = 0L;
    int total = 0;
    int broken = 0;
    String expectPrev = "";
    while (true) {
      // 游标推进：每批从上一批末尾的 trace_id 续读；Slice 免 count，无每批全表计数
      Slice<AuditTrace> slice =
          traceRepository.findByTraceIdGreaterThan(
              afterId, PageRequest.of(0, BATCH, Sort.by(Sort.Direction.ASC, "traceId")));
      if (!slice.hasContent()) {
        break;
      }
      for (AuditTrace row : slice) {
        String prev = row.getPrevDigest() == null ? "" : row.getPrevDigest();
        if (!expectPrev.equals(prev)) {
          broken++;
          if (broken <= 3) {
            log.error(
                "[留痕链巡检] 断链：traceId={} 期望 prev={} 实际 prev={}", row.getTraceId(), expectPrev, prev);
          }
        }
        expectPrev = row.getCurrDigest() == null ? "" : row.getCurrDigest();
        afterId = row.getTraceId();
        total++;
      }
      if (!slice.hasNext()) {
        break;
      }
    }
    if (total == 0) {
      log.info("[留痕链巡检] 无留痕数据，跳过");
    } else if (broken > 0) {
      log.error("[留痕链巡检] 共 {} 条留痕，发现 {} 处断链——存在删除/篡改/乱序写，请立即核查", total, broken);
    } else {
      log.info("[留痕链巡检] {} 条留痕哈希链完整", total);
    }
  }
}
