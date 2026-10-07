package com.scaffold.message.api;

import org.springframework.web.bind.annotation.ModelAttribute;
import com.scaffold.common.core.web.domain.AjaxResult;
import com.scaffold.common.core.web.page.TableDataInfo;
import com.scaffold.message.domain.MsgMailAccount;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 邮箱账号接口（§6.1：CRUD + 启停 + SMTP 连通测试）
 *
 * @author ct
 */
@RequestMapping("/message/mail/account")
public interface MailAccountResource {
  @GetMapping("/list")
  TableDataInfo list(@ModelAttribute MsgMailAccount query);

  @GetMapping("/{id}")
  AjaxResult getInfo(@PathVariable("id") Long id);

  @PostMapping
  AjaxResult add(@RequestBody MsgMailAccount account);

  @PutMapping
  AjaxResult edit(@RequestBody MsgMailAccount account);

  @DeleteMapping("/{ids}")
  AjaxResult remove(@PathVariable("ids") Long[] ids);

  @PutMapping("/status")
  AjaxResult changeStatus(@RequestParam("id") Long id, @RequestParam("status") String status);

  /** SMTP 连通测试 */
  @PostMapping("/test/{id}")
  AjaxResult test(@PathVariable("id") Long id, @RequestParam("toAddr") String toAddr);
}
