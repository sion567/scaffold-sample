package com.scaffold.message.domain;

import com.scaffold.common.core.annotation.Excel;
import com.scaffold.common.core.web.domain.VersionedEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Date;

/**
 * 邮件发送记录对象 msg_mail_log（SMTP 异常即终态，无回执链路）
 *
 * @author ct
 */
@Entity
@Table(name = "msg_mail_log")
public class MsgMailLog extends VersionedEntity {
  private static final long serialVersionUID = 1L;

  /** 发送状态机（与短信共用字典 message_send_status） */
  public static final String STATUS_PENDING = "PENDING";

  public static final String STATUS_SENDING = "SENDING";
  public static final String STATUS_SUCCESS = "SUCCESS";
  public static final String STATUS_FAILED = "FAILED";

  @Id
  private Long id;

  private Long templateId;

  private String templateCode;

  private Long accountId;

  /** 收件人（逗号分隔，≤50） */
  @Excel(name = "收件人")
  private String toAddrs;

  /** 抄送 */
  private String ccAddrs;

  /** 渲染后主题快照 */
  @Excel(name = "邮件主题")
  private String subject;

  /** 渲染后正文快照 */
  private String content;

  /** 1 HTML 0 纯文本（发送时正文形态） */
  private String isHtml;

  /** 附件 fileId 列表（ct-file，逗号分隔 ≤10） */
  private String attachFileIds;

  /** PENDING/SENDING/SUCCESS/FAILED */
  @Excel(name = "发送状态")
  private String sendStatus;

  private String failReason;

  private Integer retryCount;

  @Excel(name = "提交时间", dateFormat = "yyyy-MM-dd HH:mm:ss")
  private Date sendTime;

  private String bizType;

  private String bizId;

  private String idempotentKey;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getTemplateId() {
    return templateId;
  }

  public void setTemplateId(Long templateId) {
    this.templateId = templateId;
  }

  public String getTemplateCode() {
    return templateCode;
  }

  public void setTemplateCode(String templateCode) {
    this.templateCode = templateCode;
  }

  public Long getAccountId() {
    return accountId;
  }

  public void setAccountId(Long accountId) {
    this.accountId = accountId;
  }

  public String getToAddrs() {
    return toAddrs;
  }

  public void setToAddrs(String toAddrs) {
    this.toAddrs = toAddrs;
  }

  public String getCcAddrs() {
    return ccAddrs;
  }

  public void setCcAddrs(String ccAddrs) {
    this.ccAddrs = ccAddrs;
  }

  public String getSubject() {
    return subject;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public String getIsHtml() {
    return isHtml;
  }

  public void setIsHtml(String isHtml) {
    this.isHtml = isHtml;
  }

  public String getAttachFileIds() {
    return attachFileIds;
  }

  public void setAttachFileIds(String attachFileIds) {
    this.attachFileIds = attachFileIds;
  }

  public String getSendStatus() {
    return sendStatus;
  }

  public void setSendStatus(String sendStatus) {
    this.sendStatus = sendStatus;
  }

  public String getFailReason() {
    return failReason;
  }

  public void setFailReason(String failReason) {
    this.failReason = failReason;
  }

  public Integer getRetryCount() {
    return retryCount;
  }

  public void setRetryCount(Integer retryCount) {
    this.retryCount = retryCount;
  }

  public Date getSendTime() {
    return sendTime;
  }

  public void setSendTime(Date sendTime) {
    this.sendTime = sendTime;
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
}
