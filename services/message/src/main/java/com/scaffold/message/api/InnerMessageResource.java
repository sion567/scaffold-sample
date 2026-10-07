package com.scaffold.message.api;

import org.springframework.web.bind.annotation.ModelAttribute;
import com.scaffold.common.core.web.domain.AjaxResult;
import com.scaffold.common.core.web.page.TableDataInfo;
import com.scaffold.message.domain.MsgInnerMessage;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 站内信消息接口（§6.1：管理端全量分页/人工发送/撤回 + 用户端我的消息/未读数/已读）
 *
 * @author ct
 */
@RequestMapping("/message/inner/message")
public interface InnerMessageResource {
  @GetMapping("/list")
  TableDataInfo list(@ModelAttribute MsgInnerMessage query);

  /** 人工发送：body {templateCode, params: {k:v}, receiverIds: [], jumpUrl} */
  @PostMapping("/send")
  AjaxResult send(@RequestBody Map<String, Object> body);

  /** 撤回（未读才可删，已读留痕） */
  @DeleteMapping("/{ids}")
  AjaxResult remove(@PathVariable("ids") Long[] ids);

  // ==== 用户端（@RequiresLogin，无权限点） ====

  /** 我的消息分页（readFlag 筛选） */
  @GetMapping("/mine")
  TableDataInfo mine(@RequestParam(value = "readFlag", required = false) String readFlag);

  /** 未读数（铃铛轮询） */
  @GetMapping("/unread-count")
  AjaxResult unreadCount();

  /** 标记已读 */
  @PutMapping("/read/{id}")
  AjaxResult read(@PathVariable("id") Long id);

  /** 全部已读 */
  @PutMapping("/read-all")
  AjaxResult readAll();

  /** 接收范围选择支持：按关键词查用户（人工发送对话框） */
  @GetMapping("/user-options")
  AjaxResult userOptions(@RequestParam(value = "keyword", required = false) String keyword);
}
