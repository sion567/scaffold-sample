package com.scaffold.message.api;

import org.springframework.web.bind.annotation.ModelAttribute;
import com.scaffold.common.core.web.domain.AjaxResult;
import com.scaffold.common.core.web.page.TableDataInfo;
import com.scaffold.message.domain.MsgSmsLog;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * 短信发送日志接口（§6.1：查询/详情/失败重发/清理/导出）
 *
 * @author ct
 */
@RequestMapping("/message/sms/log")
public interface SmsLogResource {
  @GetMapping("/list")
  TableDataInfo list(@ModelAttribute MsgSmsLog query);

  @GetMapping("/{id}")
  AjaxResult getInfo(@PathVariable("id") Long id);

  /** 失败重发（RETRY_COUNT<5） */
  @PostMapping("/retry/{id}")
  AjaxResult retry(@PathVariable("id") Long id);

  @DeleteMapping("/{ids}")
  AjaxResult remove(@PathVariable("ids") Long[] ids);

  /** 导出（手机号中间四位脱敏） */
  @RequestMapping(
      value = "/export",
      method = {RequestMethod.GET, RequestMethod.POST},
      produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
  byte[] export(@ModelAttribute MsgSmsLog query);
}
