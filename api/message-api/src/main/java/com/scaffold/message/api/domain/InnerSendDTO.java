package com.scaffold.message.api.domain;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 站内信发送入参（跨模块契约）：接收范围 receiverIds / roleKeys / allUser 三选一
 *
 * @author ct
 */
public class InnerSendDTO implements Serializable {
  private static final long serialVersionUID = 1L;

  /** 模板编码 */
  private String templateCode;

  /** 模板变量 */
  private Map<String, String> params;

  /** 指定接收人用户 ID 列表 */
  private List<Long> receiverIds;

  /** 按角色展开（role_key 列表） */
  private List<String> roleKeys;

  /** 是否全员 */
  private Boolean allUser;

  /** 业务关联类型 */
  private String bizType;

  /** 业务关联 ID */
  private String bizId;

  /** 幂等键（按接收人粒度展开） */
  private String idempotentKey;

  /** 操作人（系统场景传 "system(xxx)"） */
  private String operator;

  /** 跳转路由覆盖位（空则取模板 JUMP_URL） */
  private String jumpUrl;

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

  public List<Long> getReceiverIds() {
    return receiverIds;
  }

  public void setReceiverIds(List<Long> receiverIds) {
    this.receiverIds = receiverIds;
  }

  public List<String> getRoleKeys() {
    return roleKeys;
  }

  public void setRoleKeys(List<String> roleKeys) {
    this.roleKeys = roleKeys;
  }

  public Boolean getAllUser() {
    return allUser;
  }

  public void setAllUser(Boolean allUser) {
    this.allUser = allUser;
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

  public String getJumpUrl() {
    return jumpUrl;
  }

  public void setJumpUrl(String jumpUrl) {
    this.jumpUrl = jumpUrl;
  }
}
