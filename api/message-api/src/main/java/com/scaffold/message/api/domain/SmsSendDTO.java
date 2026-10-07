package com.scaffold.message.api.domain;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.Map;

/**
 * 短信发送入参（跨模块契约，字段校验注解见各 getter）
 *
 * @author ct
 */
public class SmsSendDTO implements Serializable {
  private static final long serialVersionUID = 1L;

  /** 模板编码（业务调用键） */
  private String templateCode;

  /** 模板变量（占位符 ${key} 的键值对） */
  private Map<String, String> params;

  /** 接收手机号 */
  private String mobile;

  /** 业务关联类型（如 ALERT；幂等四件套之一；测试发送传 TEST） */
  private String bizType;

  /** 业务关联 ID */
  private String bizId;

  /** 幂等键（缺省取参数摘要） */
  private String idempotentKey;

  /** 操作人（系统场景传 "system(xxx)"） */
  private String operator;

  public void setTemplateCode(String templateCode) {
    this.templateCode = templateCode;
  }

  public Map<String, String> getParams() {
    return params;
  }

  public void setParams(Map<String, String> params) {
    this.params = params;
  }

  public void setMobile(String mobile) {
    this.mobile = mobile;
  }

  @NotBlank(message = "模板编码不能为空")
  public String getTemplateCode() {
    return templateCode;
  }

  @NotBlank(message = "接收手机号不能为空")
  public String getMobile() {
    return mobile;
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

  public String getIdempotentKey() {
    return idempotentKey;
  }

  public void setIdempotentKey(String idempotentKey) {
    this.idempotentKey = idempotentKey;
  }

  public String getOperator() {
    return operator;
  }

  public void setOperator(String operator) {
    this.operator = operator;
  }
}
