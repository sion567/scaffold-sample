package com.scaffold.message.api;

import com.scaffold.common.core.web.domain.AjaxResult;
import com.scaffold.common.core.web.page.TableDataInfo;
import com.scaffold.message.domain.MsgMailTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 邮件模板接口（§6.1：CRUD + 启停）
 *
 * @author ct
 */
@RequestMapping("/message/mail/template")
public interface MailTemplateResource {
  /**
   * 模板列表。过滤字段摊平为 @RequestParam：Dubbo Triple REST 对显式
   * {@code @ModelAttribute} 会误读 required 属性而 500（同 system 服务约定）。
   */
  @GetMapping("/list")
  TableDataInfo list(@RequestParam(value = "templateCode", required = false) String templateCode,
                     @RequestParam(value = "templateName", required = false) String templateName,
                     @RequestParam(value = "accountId", required = false) Long accountId,
                     @RequestParam(value = "category", required = false) String category,
                     @RequestParam(value = "status", required = false) String status);

  @GetMapping("/{id}")
  AjaxResult getInfo(@PathVariable("id") Long id);

  @PostMapping
  AjaxResult add(@RequestBody MsgMailTemplate template);

  @PutMapping
  AjaxResult edit(@RequestBody MsgMailTemplate template);

  @DeleteMapping("/{ids}")
  AjaxResult remove(@PathVariable("ids") Long[] ids);

  @PutMapping("/status")
  AjaxResult changeStatus(@RequestParam("id") Long id, @RequestParam("status") String status);
}
