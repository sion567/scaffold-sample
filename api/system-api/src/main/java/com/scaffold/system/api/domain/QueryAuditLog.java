package com.scaffold.system.api.domain;

import java.io.Serializable;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.data.domain.Persistable;

/**
 * 查询审计日志(谁、何时、查了谁、返回什么)
 *
 * <p>审计域仅追加表（audit 服务持有），主键由应用侧雪花生成、无 update/delete——
 * 实现 {@link Persistable#isNew()} 恒真，save 直接 persist 不触发 merge 前置查询。
 *
 * @author ct
 */
@Entity
@Table(name = "query_audit_log")
public class QueryAuditLog implements Serializable, Persistable<Long> {

  private static final long serialVersionUID = 1L;

  /** 主键 */
  @Id
  private Long id;

  /** 调用方系统标识(UI/DATASCREEN/POLICE_COLLECTOR...) */
  private String callerSystem;

  /** 操作员账号(系统触发为 SYSTEM) */
  private String operatorName;

  /** 查询对象人员UID */
  private String subjectUid;

  /** 查询对象摘要(脱敏，如 zhang某 / 证件号前6后4) */
  private String subjectMasked;

  /** 查询类型(RISK_CHECK/TRAJECTORY/DETAIL...) */
  private String queryType;

  /** 业务用途/场景 */
  private String purpose;

  /** 请求流水号 */
  private String requestId;

  /** 返回记录数 */
  private Long resultCount;

  /** 返回摘要(脱敏后的字段清单，不含明文) */
  private String resultSummary;

  /** 查询时间 */
  private java.util.Date queryTime;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getCallerSystem() {
    return callerSystem;
  }

  public void setCallerSystem(String callerSystem) {
    this.callerSystem = callerSystem;
  }

  public String getOperatorName() {
    return operatorName;
  }

  public void setOperatorName(String operatorName) {
    this.operatorName = operatorName;
  }

  public String getSubjectUid() {
    return subjectUid;
  }

  public void setSubjectUid(String subjectUid) {
    this.subjectUid = subjectUid;
  }

  public String getSubjectMasked() {
    return subjectMasked;
  }

  public void setSubjectMasked(String subjectMasked) {
    this.subjectMasked = subjectMasked;
  }

  public String getQueryType() {
    return queryType;
  }

  public void setQueryType(String queryType) {
    this.queryType = queryType;
  }

  public String getPurpose() {
    return purpose;
  }

  public void setPurpose(String purpose) {
    this.purpose = purpose;
  }

  public String getRequestId() {
    return requestId;
  }

  public void setRequestId(String requestId) {
    this.requestId = requestId;
  }

  public Long getResultCount() {
    return resultCount;
  }

  public void setResultCount(Long resultCount) {
    this.resultCount = resultCount;
  }

  public String getResultSummary() {
    return resultSummary;
  }

  public void setResultSummary(String resultSummary) {
    this.resultSummary = resultSummary;
  }

  public java.util.Date getQueryTime() {
    return queryTime;
  }

  public void setQueryTime(java.util.Date queryTime) {
    this.queryTime = queryTime;
  }

  /** 仅追加表：save 恒为 persist（应用侧雪花主键不走 merge 前置查询） */
  @Override
  public boolean isNew() {
    return true;
  }
}
