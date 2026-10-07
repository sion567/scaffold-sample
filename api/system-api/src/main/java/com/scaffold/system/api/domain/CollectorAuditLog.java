package com.scaffold.system.api.domain;

import java.io.Serializable;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.data.domain.Persistable;

/**
 * 采集留痕日志(collector 采集回执/失败记录)
 *
 * <p>审计域仅追加表（audit 服务持有），主键由应用侧雪花生成、无 update/delete——
 * 实现 {@link Persistable#isNew()} 恒真，save 直接 persist 不触发 merge 前置查询。
 *
 * @author ct
 */
@Entity
@Table(name = "collector_audit_log")
public class CollectorAuditLog implements Serializable, Persistable<Long> {

  private static final long serialVersionUID = 1L;

  /** 主键 */
  @Id
  private Long id;

  /** 领域标识(police/gov/ship...) */
  private String domain;

  /** 数据源标识(PIRS_API/PIRS_FILE/DWH_CZRK...) */
  private String source;

  /** 通道类型(API/FILE/DB/MQ) */
  private String channelType;

  /** 结果：0-成功 1-失败 */
  private String status;

  /** 本批记录数 */
  private Long recordCount;

  /** 采集前水位 */
  private String watermarkBefore;

  /** 采集后水位(失败时空) */
  private String watermarkAfter;

  /** 失败原因(截断存储) */
  private String errorMsg;

  /** 耗时(毫秒) */
  private Long costMs;

  /** 采集开始时间 */
  private java.util.Date startTime;

  /** 入库时间 */
  private java.util.Date createTime;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getDomain() {
    return domain;
  }

  public void setDomain(String domain) {
    this.domain = domain;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public String getChannelType() {
    return channelType;
  }

  public void setChannelType(String channelType) {
    this.channelType = channelType;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Long getRecordCount() {
    return recordCount;
  }

  public void setRecordCount(Long recordCount) {
    this.recordCount = recordCount;
  }

  public String getWatermarkBefore() {
    return watermarkBefore;
  }

  public void setWatermarkBefore(String watermarkBefore) {
    this.watermarkBefore = watermarkBefore;
  }

  public String getWatermarkAfter() {
    return watermarkAfter;
  }

  public void setWatermarkAfter(String watermarkAfter) {
    this.watermarkAfter = watermarkAfter;
  }

  public String getErrorMsg() {
    return errorMsg;
  }

  public void setErrorMsg(String errorMsg) {
    this.errorMsg = errorMsg;
  }

  public Long getCostMs() {
    return costMs;
  }

  public void setCostMs(Long costMs) {
    this.costMs = costMs;
  }

  public java.util.Date getStartTime() {
    return startTime;
  }

  public void setStartTime(java.util.Date startTime) {
    this.startTime = startTime;
  }

  public java.util.Date getCreateTime() {
    return createTime;
  }

  public void setCreateTime(java.util.Date createTime) {
    this.createTime = createTime;
  }

  /** 仅追加表：save 恒为 persist（应用侧雪花主键不走 merge 前置查询） */
  @Override
  public boolean isNew() {
    return true;
  }
}
