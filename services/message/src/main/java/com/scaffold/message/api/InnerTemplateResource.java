package com.scaffold.message.api;

import org.springframework.web.bind.annotation.ModelAttribute;
import com.scaffold.common.core.web.domain.AjaxResult;
import com.scaffold.common.core.web.page.TableDataInfo;
import com.scaffold.message.domain.MsgInnerTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 站内信模板接口（§6.1：CRUD + 启停）
 *
 * @author ct
 */
@RequestMapping("/message/inner/template")
public interface InnerTemplateResource {
  @GetMapping("/list")
  TableDataInfo list(@ModelAttribute MsgInnerTemplate query);

  @GetMapping("/{id}")
  AjaxResult getInfo(@PathVariable("id") Long id);

  @PostMapping
  AjaxResult add(@RequestBody MsgInnerTemplate template);

  @PutMapping
  AjaxResult edit(@RequestBody MsgInnerTemplate template);

  @DeleteMapping("/{ids}")
  AjaxResult remove(@PathVariable("ids") Long[] ids);

  @PutMapping("/status")
  AjaxResult changeStatus(@RequestParam("id") Long id, @RequestParam("status") String status);
}
