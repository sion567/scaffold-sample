package com.scaffold.message.domain;

import com.scaffold.common.core.web.domain.VersionedEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 短信模板对象 msg_sms_template（占位符 ${key}，双轨：本平台渲染整文 / 厂商模板code+参数）
 *
 * @author ct
 */
@Entity
@Table(name = "msg_sms_template")
public class MsgSmsTemplate extends VersionedEntity {
  private static final long serialVersionUID = 1L;

  /** 模板模式：0 本平台渲染整文 1 厂商模板code+参数 */
  public static final String MODE_LOCAL_RENDER = "0";

  public static final String MODE_VENDOR_CODE = "1";

  @Id
  private Long id;

  /** 模板编码（业务调用键，全局唯一） */
  private String templateCode;

  /** 模板名称 */
  private String templateName;

  /** 绑定渠道（空=按渠道优先级自动路由） */
  private Long channelId;

  /** 模板模式 */
  private String templateMode;

  /** 厂商侧模板 ID（MODE=1 必填） */
  private String vendorCode;

  /** 签名（空=取渠道默认） */
  private String signName;

  /** 模板内容，占位符 ${key}（MODE=1 时存参数说明） */
  private String templateContent;

  /** 变量名清单（逗号分隔，录入时解析生成） */
  private String paramNames;

  /** 用途分类：VERIFY/NOTIFY/MARKETING（字典 message_sms_category） */
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

  public Long getChannelId() {
    return channelId;
  }

  public void setChannelId(Long channelId) {
    this.channelId = channelId;
  }

  public String getTemplateMode() {
    return templateMode;
  }

  public void setTemplateMode(String templateMode) {
    this.templateMode = templateMode;
  }

  public String getVendorCode() {
    return vendorCode;
  }

  public void setVendorCode(String vendorCode) {
    this.vendorCode = vendorCode;
  }

  public String getSignName() {
    return signName;
  }

  public void setSignName(String signName) {
    this.signName = signName;
  }

  public void setTemplateContent(String templateContent) {
    this.templateContent = templateContent;
  }

  @NotBlank(message = "模板内容不能为空")
  @Size(min = 0, max = 1000, message = "模板内容不能超过1000个字符")
  public String getTemplateContent() {
    return templateContent;
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
