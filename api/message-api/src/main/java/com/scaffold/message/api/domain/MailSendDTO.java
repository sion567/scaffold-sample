package com.scaffold.message.api.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 邮件发送入参（跨模块契约）
 *
 * @author ct
 */
public class MailSendDTO implements Serializable {
  private static final long serialVersionUID = 1L;

  /** 模板编码 */
  private String templateCode;

  /** 模板变量 */
  private Map<String, String> params;

  /** 收件人邮箱 */
  private List<String> to;

  /** 抄送邮箱 */
  private List<String> cc;

  /** 附件 fileId 列表（ct-file，≤10 个） */
  private List<Long> attachments;

  /** 业务关联类型 */
  private String bizType;

  /** 业务关联 ID */
  private String bizId;

  /** 幂等键 */
  private String idempotentKey;

  /** 操作人 */
  private String operator;

  @NotBlank(message = "模板编码不能为空")
  public String getTemplateCode() {
    return templateCode;
  }

  public void setTemplateCode(String templateCode) {
    this.templateCode = templateCode;
  }

  public Map<String, String> getParams() {
    return params;
  }

  public void setParams(Map<String, String> params) {
    this.params = params;
  }

  @NotEmpty(message = "收件人不能为空")
  public List<String> getTo() {
    return to;
  }

  public void setTo(List<String> to) {
    this.to = to;
  }

  public List<String> getCc() {
    return cc;
  }

  public void setCc(List<String> cc) {
    this.cc = cc;
  }

  public List<Long> getAttachments() {
    return attachments;
  }

  public void setAttachments(List<Long> attachments) {
    this.attachments = attachments;
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
