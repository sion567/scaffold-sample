package com.scaffold.audit.api;

import java.io.Serializable;
import java.util.Date;

/**
 * 全链路业务留痕条目（详细设计 §2.2/§3.4：audit_trace 承载）
 *
 * <p>由 scaffold-common-log 的 @TraceLog 切面采集，scaffold-audit 落库并计算 SM3 摘要链、 关键动作 SM2 签名。字段即快照——落库侧不做二次解析。
 *
 * @author ct
 */
public class TraceLogEntry implements Serializable {
  private static final long serialVersionUID = 1L;

  /** 场景（指令下发/签收/改派/情报查询…，业务方在注解上声明） */
  private String scene;

  /** 业务对象类型（instruction/dispatch_order/alert_rule…） */
  private String bizType;

  /** 业务对象 ID（切面按 SpEL 解析，可空） */
  private String bizId;

  /** 操作人账号（无登录态的系统动作为空） */
  private String operator;

  /** 操作 IP */
  private String operateIp;

  /** 终端类型（WEB/警务通…，取 X-Terminal 头，缺省 WEB） */
  private String terminal;

  /** 方法（类名#方法名） */
  private String methodName;

  /** 入参快照 JSON（截断，敏感字段由序列化器排除） */
  private String snapshotJson;

  /** 返回值快照 JSON（截断；异常时为空） */
  private String resultJson;

  /** 异常摘要（成功为空） */
  private String errorMsg;

  /** 耗时毫秒 */
  private Long costMs;

  /** 是否关键动作（指令下发/签收/改派/撤回/导出 → SM2 签名） */
  private boolean keyAction;

  /** 操作发生时间（切面采集时刻） */
  private Date operateTime;

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

  public boolean isKeyAction() {
    return keyAction;
  }

  public void setKeyAction(boolean keyAction) {
    this.keyAction = keyAction;
  }

  public Date getOperateTime() {
    return operateTime;
  }

  public void setOperateTime(Date operateTime) {
    this.operateTime = operateTime;
  }
}
