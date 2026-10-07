package com.scaffold.message.domain;

import com.scaffold.common.core.web.domain.VersionedEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 短信渠道对象 msg_sms_channel（厂商账号配置，同类型可多渠道、按优先级 failover）
 *
 * @author ct
 */
@Entity
@Table(name = "msg_sms_channel")
public class MsgSmsChannel extends VersionedEntity {
  private static final long serialVersionUID = 1L;

  /** 雪花 ID */
  @Id
  private Long id;

  /** 渠道名称（如"阿里云-主用"） */
  private String channelName;

  /** 类型：ALIYUN/TENCENT/HUAWEI/CTYUN/MOCK（字典 message_sms_channel_type） */
  private String channelType;

  /** 厂商 AccessKey（非敏感，明文） */
  private String accessKey;

  /** 厂商 SecretKey（SM4 密文落库，列表/详情脱敏不回显） */
  private String secretKey;

  /** 默认短信签名 */
  private String signName;

  /** 接入点 */
  private String endpoint;

  /** 地域 */
  private String region;

  /** 优先级（小者先用，同类型 failover） */
  private Integer priority;

  /** 0 启用 1 停用 */
  private String status;

  /** 逻辑删除：0 存在 2 删除 */
  private String delFlag;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public void setChannelName(String channelName) {
    this.channelName = channelName;
  }

  @NotBlank(message = "渠道名称不能为空")
  @Size(min = 0, max = 64, message = "渠道名称不能超过64个字符")
  public String getChannelName() {
    return channelName;
  }

  public void setChannelType(String channelType) {
    this.channelType = channelType;
  }

  @NotBlank(message = "渠道类型不能为空")
  public String getChannelType() {
    return channelType;
  }

  public String getAccessKey() {
    return accessKey;
  }

  public void setAccessKey(String accessKey) {
    this.accessKey = accessKey;
  }

  public String getSecretKey() {
    return secretKey;
  }

  public void setSecretKey(String secretKey) {
    this.secretKey = secretKey;
  }

  public String getSignName() {
    return signName;
  }

  public void setSignName(String signName) {
    this.signName = signName;
  }

  public String getEndpoint() {
    return endpoint;
  }

  public void setEndpoint(String endpoint) {
    this.endpoint = endpoint;
  }

  public String getRegion() {
    return region;
  }

  public void setRegion(String region) {
    this.region = region;
  }

  public Integer getPriority() {
    return priority;
  }

  public void setPriority(Integer priority) {
    this.priority = priority;
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
