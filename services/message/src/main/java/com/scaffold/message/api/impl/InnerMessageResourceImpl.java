package com.scaffold.message.api.impl;

import com.scaffold.common.core.utils.StringUtils;
import com.scaffold.common.core.web.controller.BaseController;
import com.scaffold.common.core.web.domain.AjaxResult;
import com.scaffold.common.core.web.page.TableDataInfo;
import com.scaffold.common.core.web.page.TableSupport;
import com.scaffold.common.log.annotation.Log;
import com.scaffold.common.log.enums.BusinessType;
import com.scaffold.common.security.annotation.RequiresLogin;
import com.scaffold.common.security.annotation.RequiresPermissions;
import com.scaffold.common.security.utils.SecurityUtils;
import com.scaffold.message.api.InnerMessageResource;
import com.scaffold.message.domain.MsgInnerMessage;
import com.scaffold.message.service.IMsgInnerMessageService;
import com.scaffold.message.service.impl.UserDirectoryClient;
import com.scaffold.system.api.UserDirectoryItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 站内信消息实现（Triple REST）：管理端权限点 + 用户端 @RequiresLogin
 *
 * @author ct
 */
@DubboService
public class InnerMessageResourceImpl extends BaseController implements InnerMessageResource {
  private final IMsgInnerMessageService messageService;

  private final UserDirectoryClient userDirectoryClient;

  public InnerMessageResourceImpl(
      IMsgInnerMessageService messageService, UserDirectoryClient userDirectoryClient) {
    this.messageService = messageService;
    this.userDirectoryClient = userDirectoryClient;
  }

  @Override
  @RequiresPermissions("message:innerMessage:list")
  public TableDataInfo list(MsgInnerMessage query) {
    return messageService.queryPage(query, TableSupport.buildPageRequest());
  }

  @Override
  @RequiresPermissions("message:innerMessage:send")
  @Log(title = "站内信人工发送", businessType = BusinessType.INSERT)
  public AjaxResult send(@RequestBody Map<String, Object> body) {
    String templateCode =
        body.get("templateCode") == null ? null : String.valueOf(body.get("templateCode"));
    if (StringUtils.isEmpty(templateCode)) {
      return error("templateCode 不能为空");
    }
    Map<String, String> params = new HashMap<>();
    Object rawParams = body.get("params");
    if (rawParams instanceof Map<?, ?> paramMap) {
      for (Map.Entry<?, ?> entry : paramMap.entrySet()) {
        params.put(
            String.valueOf(entry.getKey()),
            entry.getValue() == null ? "" : String.valueOf(entry.getValue()));
      }
    }
    List<Long> receiverIds = new ArrayList<>();
    Object rawIds = body.get("receiverIds");
    if (rawIds instanceof List<?> idList) {
      for (Object item : idList) {
        if (item instanceof Number number) {
          receiverIds.add(number.longValue());
        }
      }
    }
    String jumpUrl = body.get("jumpUrl") == null ? null : String.valueOf(body.get("jumpUrl"));
    return success(
        messageService.sendManual(
            templateCode, params, receiverIds, jumpUrl, SecurityUtils.getUsername()));
  }

  @Override
  @RequiresPermissions("message:innerMessage:remove")
  @Log(title = "站内信撤回", businessType = BusinessType.DELETE)
  public AjaxResult remove(@PathVariable("ids") Long[] ids) {
    return toAjax(messageService.deleteByIds(ids));
  }

  @Override
  @RequiresLogin
  public TableDataInfo mine(String readFlag) {
    // 当前用户自有数据分页（PageDomain 取自请求上下文）
    return messageService.queryMinePage(readFlag, TableSupport.buildPageRequest());
  }

  @Override
  @RequiresLogin
  public AjaxResult unreadCount() {
    return success(messageService.unreadCount());
  }

  @Override
  @RequiresLogin
  public AjaxResult read(@PathVariable("id") Long id) {
    return toAjax(messageService.markRead(id));
  }

  @Override
  @RequiresLogin
  public AjaxResult readAll() {
    return toAjax(messageService.markAllRead());
  }

  @Override
  @RequiresPermissions("message:innerMessage:send")
  public AjaxResult userOptions(String keyword) {
    // 人工发送对话框远程搜索：全员表按关键词过滤（nickName/userName），上限 50 条；
    // 输出保持原大写键结构（USER_ID/USER_NAME/NICK_NAME），前端契约不变
    List<Map<String, Object>> result = new ArrayList<>();
    for (UserDirectoryItem user : userDirectoryClient.listAll()) {
      String name =
          user.getNickName() == null ? user.getUserName() : user.getNickName();
      if (StringUtils.isEmpty(keyword)
          || name.contains(keyword)
          || user.getUserName().contains(keyword)) {
        Map<String, Object> option = new LinkedHashMap<>();
        option.put("USER_ID", user.getUserId());
        option.put("USER_NAME", user.getUserName());
        option.put("NICK_NAME", user.getNickName());
        result.add(option);
        if (result.size() >= 50) {
          break;
        }
      }
    }
    return success(result);
  }
}
