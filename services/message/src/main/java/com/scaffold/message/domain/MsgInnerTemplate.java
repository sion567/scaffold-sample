package com.scaffold.message.domain;

import com.scaffold.common.core.web.domain.VersionedEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 站内信模板对象 msg_inner_template（点对点，与广播型公告互补）
 *
 * @author ct
 */
@Entity
@Table(name = "msg_inner_template")
public class MsgInnerTemplate extends VersionedEntity {
  private static final long serialVersionUID = 1L;

  /** 缺参策略：0 报错 1 保留占位 2 置空 */
  public static final String MISSING_ERROR = "0";

  public static final String MISSING_KEEP = "1";
  public static final String MISSING_BLANK = "2";

  @Id
  private Long id;

  private String templateCode;

  private String templateName;

  /** 标题模板（${key}） */
  private String title;

  /** 正文模板（纯文本） */
  private String templateContent;

  /** 缺参策略 */
  private String missingParamMode;

  /** 点击跳转路由（前端站内路由，可空） */
  private String jumpUrl;

  /** 变量名清单（逗号分隔） */
  private String paramNames;

  /** 用途分类（字典 message_inner_category） */
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

  public void setTitle(String title) {
    this.title = title;
  }

  @NotBlank(message = "标题模板不能为空")
  @Size(min = 0, max = 100, message = "标题不能超过100个字符")
  public String getTitle() {
    return title;
  }

  public void setTemplateContent(String templateContent) {
    this.templateContent = templateContent;
  }

  @NotBlank(message = "正文模板不能为空")
  @Size(min = 0, max = 2000, message = "正文不能超过2000个字符")
  public String getTemplateContent() {
    return templateContent;
  }

  public String getMissingParamMode() {
    return missingParamMode;
  }

  public void setMissingParamMode(String missingParamMode) {
    this.missingParamMode = missingParamMode;
  }

  public String getJumpUrl() {
    return jumpUrl;
  }

  public void setJumpUrl(String jumpUrl) {
    this.jumpUrl = jumpUrl;
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
