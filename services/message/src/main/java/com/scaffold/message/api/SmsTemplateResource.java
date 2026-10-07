package com.scaffold.message.api;

import org.springframework.web.bind.annotation.ModelAttribute;
import com.scaffold.common.core.web.domain.AjaxResult;
import com.scaffold.common.core.web.page.TableDataInfo;
import com.scaffold.message.domain.MsgSmsTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 短信模板接口（§6.1：标准 CRUD + 启停）
 *
 * @author ct
 */
@RequestMapping("/message/sms/template")
public interface SmsTemplateResource {
  @GetMapping("/list")
  TableDataInfo list(@ModelAttribute MsgSmsTemplate query);

  @GetMapping("/{id}")
  AjaxResult getInfo(@PathVariable("id") Long id);

  @PostMapping
  AjaxResult add(@RequestBody MsgSmsTemplate template);

  @PutMapping
  AjaxResult edit(@RequestBody MsgSmsTemplate template);

  @DeleteMapping("/{ids}")
  AjaxResult remove(@PathVariable("ids") Long[] ids);

  @PutMapping("/status")
  AjaxResult changeStatus(@RequestParam("id") Long id, @RequestParam("status") String status);
}
