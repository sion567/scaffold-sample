package com.scaffold.message.domain;

import com.scaffold.common.core.web.domain.VersionedEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Date;

/**
 * 站内信消息记录对象 msg_inner_message（按接收人一行一条，含已读状态）
 *
 * @author ct
 */
@Entity
@Table(name = "msg_inner_message")
public class MsgInnerMessage extends VersionedEntity {
  private static final long serialVersionUID = 1L;

  /** 已读：0 未读 1 已读 */
  public static final String READ_NO = "0";

  public static final String READ_YES = "1";

  @Id
  private Long id;

  private Long templateId;

  /** 冗余 code */
  private String templateCode;

  /** 渲染后标题快照 */
  private String title;

  /** 渲染后正文快照 */
  private String content;

  /** 跳转路由快照 */
  private String jumpUrl;

  /** 接收人用户 ID */
  private Long receiverId;

  /** 冗余姓名（列表免关联） */
  private String receiverName;

  /** 0 未读 1 已读 */
  private String readFlag;

  private Date readTime;

  /** 0 系统自动 1 人工 */
  private String senderType;

  private Long senderId;

  /** 发起人（系统场景传 "system(xxx)"） */
  private String senderName;

  private String bizType;

  private String bizId;

  /** 幂等键（按接收人粒度） */
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

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public String getJumpUrl() {
    return jumpUrl;
  }

  public void setJumpUrl(String jumpUrl) {
    this.jumpUrl = jumpUrl;
  }

  public Long getReceiverId() {
    return receiverId;
  }

  public void setReceiverId(Long receiverId) {
    this.receiverId = receiverId;
  }

  public String getReceiverName() {
    return receiverName;
  }

  public void setReceiverName(String receiverName) {
    this.receiverName = receiverName;
  }

  public String getReadFlag() {
    return readFlag;
  }

  public void setReadFlag(String readFlag) {
    this.readFlag = readFlag;
  }

  public Date getReadTime() {
    return readTime;
  }

  public void setReadTime(Date readTime) {
    this.readTime = readTime;
  }

  public String getSenderType() {
    return senderType;
  }

  public void setSenderType(String senderType) {
    this.senderType = senderType;
  }

  public Long getSenderId() {
    return senderId;
  }

  public void setSenderId(Long senderId) {
    this.senderId = senderId;
  }

  public String getSenderName() {
    return senderName;
  }

  public void setSenderName(String senderName) {
    this.senderName = senderName;
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
