package com.scaffold.message.domain;

import com.scaffold.common.core.web.domain.VersionedEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 邮件模板对象 msg_mail_template（HTML 富文本，主题与正文均支持 ${key}）
 *
 * @author ct
 */
@Entity
@Table(name = "msg_mail_template")
public class MsgMailTemplate extends VersionedEntity {
  private static final long serialVersionUID = 1L;

  @Id
  private Long id;

  private String templateCode;

  private String templateName;

  /** 绑定发件账号（空=取默认账号） */
  private Long accountId;

  /** 邮件主题（支持 ${key}） */
  private String subject;

  /** HTML 正文（富文本编辑器产出） */
  private String templateContent;

  /** 1 HTML 0 纯文本 */
  private String isHtml;

  /** 变量名清单（逗号分隔） */
  private String paramNames;

  /** 用途分类（字典 message_mail_category） */
  private String category;

  /** 0 启用 1 停用 */
  private String status;

  private String delFlag;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public void setTemplateCode(String templateCode) {
    this.templateCode = templateCode;
  }

  @NotBlank(message = "模板编码不能为空")
  @Size(min = 0, max = 64, message = "模板编码不能超过64个字符")
  public String getTemplateCode() {
    return templateCode;
  }

  public void setTemplateName(String templateName) {
    this.templateName = templateName;
  }

  @NotBlank(message = "模板名称不能为空")
  @Size(min = 0, max = 64, message = "模板名称不能超过64个字符")
  public String getTemplateName() {
    return templateName;
  }

  public Long getAccountId() {
    return accountId;
  }

  public void setAccountId(Long accountId) {
    this.accountId = accountId;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }

  @NotBlank(message = "邮件主题不能为空")
  @Size(min = 0, max = 200, message = "邮件主题不能超过200个字符")
  public String getSubject() {
    return subject;
  }

  public void setTemplateContent(String templateContent) {
    this.templateContent = templateContent;
  }

  @NotBlank(message = "邮件正文不能为空")
  public String getTemplateContent() {
    return templateContent;
  }

  public String getIsHtml() {
    return isHtml;
  }

  public void setIsHtml(String isHtml) {
    this.isHtml = isHtml;
  }

  public String getParamNames() {
    return paramNames;
  }

  public void setParamNames(String paramNames) {
    this.paramNames = paramNames;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getDelFlag() {
    return delFlag;
  }

  public void setDelFlag(String delFlag) {
    this.delFlag = delFlag;
  }
}
