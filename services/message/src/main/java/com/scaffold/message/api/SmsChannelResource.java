package com.scaffold.message.api;

import com.scaffold.common.core.web.domain.AjaxResult;
import com.scaffold.common.core.web.page.TableDataInfo;
import com.scaffold.message.domain.MsgSmsChannel;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 短信渠道接口（§6.1）
 *
 * @author ct
 */
@RequestMapping("/message/sms/channel")
public interface SmsChannelResource {
  /**
   * 渠道列表。过滤字段摊平为 @RequestParam：Dubbo Triple REST 对显式
   * {@code @ModelAttribute} 会误读 required 属性而 500（同 system 服务约定）。
   */
  @GetMapping("/list")
  TableDataInfo list(@RequestParam(value = "channelName", required = false) String channelName,
                     @RequestParam(value = "channelType", required = false) String channelType,
                     @RequestParam(value = "status", required = false) String status);

  @GetMapping("/{id}")
  AjaxResult getInfo(@PathVariable("id") Long id);

  @PostMapping
  AjaxResult add(@RequestBody MsgSmsChannel channel);

  @PutMapping
  AjaxResult edit(@RequestBody MsgSmsChannel channel);

  @DeleteMapping("/{ids}")
  AjaxResult remove(@PathVariable("ids") Long[] ids);

  @PutMapping("/status")
  AjaxResult changeStatus(@RequestParam("id") Long id, @RequestParam("status") String status);

  /** 连通测试（向指定手机发测试短信） */
  @PostMapping("/test/{id}")
  AjaxResult test(@PathVariable("id") Long id, @RequestParam("mobile") String mobile);
}
