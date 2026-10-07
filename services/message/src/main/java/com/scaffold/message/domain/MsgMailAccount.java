package com.scaffold.message.domain;

import com.scaffold.common.core.web.domain.VersionedEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 邮箱账号对象 msg_mail_account（SMTP 发件账号，授权码 SM4 密文落库）
 *
 * @author ct
 */
@Entity
@Table(name = "msg_mail_account")
public class MsgMailAccount extends VersionedEntity {
  private static final long serialVersionUID = 1L;

  @Id
  private Long id;

  /** 账号显示名（如"平台通知邮箱"） */
  private String accountName;

  /** 发件邮箱地址 */
  private String emailAddr;

  /** SMTP 服务器 */
  private String smtpHost;

  /** SMTP 端口（默认 465 SSL） */
  private Integer smtpPort;

  /** 1 SSL 0 非SSL */
  private String smtpSsl;

  /** 认证用户名 */
  private String authUser;

  /** 授权码/密码（SM4 密文落库，列表/详情脱敏不回显） */
  private String authCode;

  /** 发件人显示名 */
  private String nickName;

  /** 日发送上限（0 不限） */
  private Integer dailyLimit;

  /** 0 启用 1 停用 */
  private String status;

  private String delFlag;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public void setAccountName(String accountName) {
    this.accountName = accountName;
  }

  @NotBlank(message = "账号名称不能为空")
  @Size(min = 0, max = 64, message = "账号名称不能超过64个字符")
  public String getAccountName() {
    return accountName;
  }

  public void setEmailAddr(String emailAddr) {
    this.emailAddr = emailAddr;
  }

  @NotBlank(message = "发件邮箱不能为空")
  @Size(min = 0, max = 128, message = "发件邮箱不能超过128个字符")
  public String getEmailAddr() {
    return emailAddr;
  }

  public void setSmtpHost(String smtpHost) {
    this.smtpHost = smtpHost;
  }

  @NotBlank(message = "SMTP服务器不能为空")
  public String getSmtpHost() {
    return smtpHost;
  }

  public Integer getSmtpPort() {
    return smtpPort;
  }

  public void setSmtpPort(Integer smtpPort) {
    this.smtpPort = smtpPort;
  }

  public String getSmtpSsl() {
    return smtpSsl;
  }

  public void setSmtpSsl(String smtpSsl) {
    this.smtpSsl = smtpSsl;
  }

  public void setAuthUser(String authUser) {
    this.authUser = authUser;
  }

  @NotBlank(message = "认证用户名不能为空")
  public String getAuthUser() {
    return authUser;
  }

  public String getAuthCode() {
    return authCode;
  }

  public void setAuthCode(String authCode) {
    this.authCode = authCode;
  }

  public String getNickName() {
    return nickName;
  }

  public void setNickName(String nickName) {
    this.nickName = nickName;
  }

  public Integer getDailyLimit() {
    return dailyLimit;
  }

  public void setDailyLimit(Integer dailyLimit) {
    this.dailyLimit = dailyLimit;
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
