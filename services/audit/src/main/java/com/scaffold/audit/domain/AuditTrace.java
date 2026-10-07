package com.scaffold.audit.domain;

import java.io.Serializable;
import java.util.Date;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.data.domain.Persistable;

/**
 * 全链路业务留痕（audit_trace，详细设计 §3.4）
 *
 * <p>仅追加不 UPDATE；trace_id 应用侧雪花生成；摘要链在写入侧计算。
 * 实现 {@link Persistable#isNew()} 恒真：save 恒为 persist，不触发 merge 前置查询。
 *
 * @author ct
 */
@Entity
@Table(name = "audit_trace")
public class AuditTrace implements Serializable, Persistable<Long> {
  private static final long serialVersionUID = 1L;

  /** 主键（应用侧雪花生成） */
  @Id
  private Long traceId;

  private String scene;

  private String bizType;

  private String bizId;

  private String operator;

  private String operateIp;

  private String terminal;

  private String methodName;

  private String snapshotJson;

  private String resultJson;

  private String errorMsg;

  private Long costMs;

  /** 0-否 1-是（关键动作 SM2 签名） */
  private String keyAction;

  private String prevDigest;

  private String currDigest;

  private String signValue;

  private Date opTime;

  private Date createTime;

  public Long getTraceId() {
    return traceId;
  }

  public void setTraceId(Long traceId) {
    this.traceId = traceId;
  }

  public String getScene() {
    return scene;
  }

  public void setScene(String scene) {
    this.scene = scene;
  }

  public String getBizType() {
    return bizType;
  }

  public void setBizType(String bizType) {
    this.bizType = bizType;
  }

  public String getBizId() {
    return bizId;
  }

  public void setBizId(String bizId) {
    this.bizId = bizId;
  }

  public String getOperator() {
    return operator;
  }

  public void setOperator(String operator) {
    this.operator = operator;
  }

  public String getOperateIp() {
    return operateIp;
  }

  public void setOperateIp(String operateIp) {
    this.operateIp = operateIp;
  }

  public String getTerminal() {
    return terminal;
  }

  public void setTerminal(String terminal) {
    this.terminal = terminal;
  }

  public String getMethodName() {
    return methodName;
  }

  public void setMethodName(String methodName) {
    this.methodName = methodName;
  }

  public String getSnapshotJson() {
    return snapshotJson;
  }

  public void setSnapshotJson(String snapshotJson) {
    this.snapshotJson = snapshotJson;
  }

  public String getResultJson() {
    return resultJson;
  }

  public void setResultJson(String resultJson) {
    this.resultJson = resultJson;
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

  public String getKeyAction() {
    return keyAction;
  }

  public void setKeyAction(String keyAction) {
    this.keyAction = keyAction;
  }

  public String getPrevDigest() {
    return prevDigest;
  }

  public void setPrevDigest(String prevDigest) {
    this.prevDigest = prevDigest;
  }

  public String getCurrDigest() {
    return currDigest;
  }

  public void setCurrDigest(String currDigest) {
    this.currDigest = currDigest;
  }

  public String getSignValue() {
    return signValue;
  }

  public void setSignValue(String signValue) {
    this.signValue = signValue;
  }

  public Date getOpTime() {
    return opTime;
  }

  public void setOpTime(Date opTime) {
    this.opTime = opTime;
  }

  public Date getCreateTime() {
    return createTime;
  }

  public void setCreateTime(Date createTime) {
    this.createTime = createTime;
  }

  /** 仅追加表：save 恒为 persist（应用侧雪花主键不走 merge 前置查询） */
  @Override
  public boolean isNew() {
    return true;
  }

  /** {@link Persistable} 契约：主键即 traceId */
  @Override
  public Long getId() {
    return traceId;
  }
}
