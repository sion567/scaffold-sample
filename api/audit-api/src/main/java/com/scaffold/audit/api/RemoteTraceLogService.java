package com.scaffold.audit.api;

/**
 * 全链路业务留痕 Dubbo 契约（G2/G3：SM3 摘要链 + 关键动作 SM2 签名 → audit_trace 表）
 *
 * <p>实现方：scaffold-audit（审计域权威模块）。提供方负责：取前一条留痕摘要计算 SM3 链、 关键动作签名、雪花主键赋值。调用方（scaffold-common-log 切面）只采集快照，不感知链。
 *
 * @author ct
 */
public interface RemoteTraceLogService {
  /**
   * 记录一条业务留痕（append-only；SM3 摘要链在提供方计算）
   *
   * @param entry 留痕条目（scene 必填，其余可空）
   * @return 写入行数（0=入参非法未写入）
   */
  int record(TraceLogEntry entry);
}
