package com.scaffold.message.api;

import org.springframework.web.bind.annotation.ModelAttribute;
import com.scaffold.common.core.web.domain.AjaxResult;
import com.scaffold.common.core.web.page.TableDataInfo;
import com.scaffold.message.domain.MsgMailLog;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * 邮件发送记录接口（§6.1）
 *
 * @author ct
 */
@RequestMapping("/message/mail/log")
public interface MailLogResource {
  @GetMapping("/list")
  TableDataInfo list(@ModelAttribute MsgMailLog query);

  @GetMapping("/{id}")
  AjaxResult getInfo(@PathVariable("id") Long id);

  @PostMapping("/retry/{id}")
  AjaxResult retry(@PathVariable("id") Long id);

  @DeleteMapping("/{ids}")
  AjaxResult remove(@PathVariable("ids") Long[] ids);

  @RequestMapping(
      value = "/export",
      method = {RequestMethod.GET, RequestMethod.POST},
      produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
  byte[] export(@ModelAttribute MsgMailLog query);
}
