package com.scaffold.message.domain;

import com.scaffold.common.core.annotation.Excel;
import com.scaffold.common.core.web.domain.VersionedEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Date;

/**
 * 短信发送日志对象 msg_sms_log（状态机 PENDING→SENDING→SUCCESS/FAILED，回执可负向修正）
 *
 * @author ct
 */
@Entity
@Table(name = "msg_sms_log")
public class MsgSmsLog extends VersionedEntity {
  private static final long serialVersionUID = 1L;

  /** 发送状态机（字典 message_send_status） */
  public static final String STATUS_PENDING = "PENDING";

  public static final String STATUS_SENDING = "SENDING";
  public static final String STATUS_SUCCESS = "SUCCESS";
  public static final String STATUS_FAILED = "FAILED";

  @Id
  private Long id;

  private Long templateId;

  /** 冗余 code，模板删除后日志仍可读 */
  private String templateCode;

  private Long channelId;

  /** 实际发送渠道类型 */
  private String channelType;

  /** 手机号（导出时中间四位脱敏） */
  @Excel(name = "手机号")
  private String mobile;

  /** 本次签名 */
  @Excel(name = "短信签名")
  private String signName;

  /** 渲染后正文快照 */
  @Excel(name = "短信内容")
  private String content;

  /** 渲染入参 JSON（重发/审计用） */
  private String paramJson;

  /** PENDING/SENDING/SUCCESS/FAILED */
  @Excel(name = "发送状态")
  private String sendStatus;

  /** 通道方消息 ID（回执对账键） */
  private String channelMsgId;

  /** 失败原因 */
  private String failReason;

  /** 重发次数（上限 5） */
  private Integer retryCount;

  /** 提交时间 */
  @Excel(name = "提交时间", dateFormat = "yyyy-MM-dd HH:mm:ss")
  private Date sendTime;

  /** 回执时间 */
  @Excel(name = "回执时间", dateFormat = "yyyy-MM-dd HH:mm:ss")
  private Date receiptTime;

  /** 业务关联类型（TEST=管理端测试发送，跳过幂等） */
  private String bizType;

  private String bizId;

  /** 幂等键（缺省取参数摘要） */
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

  public Long getChannelId() {
    return channelId;
  }

  public void setChannelId(Long channelId) {
    this.channelId = channelId;
  }

  public String getChannelType() {
    return channelType;
  }

  public void setChannelType(String channelType) {
    this.channelType = channelType;
  }

  public String getMobile() {
    return mobile;
  }

  public void setMobile(String mobile) {
    this.mobile = mobile;
  }

  public String getSignName() {
    return signName;
  }

  public void setSignName(String signName) {
    this.signName = signName;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public String getParamJson() {
    return paramJson;
  }

  public void setParamJson(String paramJson) {
    this.paramJson = paramJson;
  }

  public String getSendStatus() {
    return sendStatus;
  }

  public void setSendStatus(String sendStatus) {
    this.sendStatus = sendStatus;
  }

  public String getChannelMsgId() {
    return channelMsgId;
  }

  public void setChannelMsgId(String channelMsgId) {
    this.channelMsgId = channelMsgId;
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

  public Date getReceiptTime() {
    return receiptTime;
  }

  public void setReceiptTime(Date receiptTime) {
    this.receiptTime = receiptTime;
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
