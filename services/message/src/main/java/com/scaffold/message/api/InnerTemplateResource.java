package com.scaffold.message.api;

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
  /**
   * 模板列表。过滤字段摊平为 @RequestParam：Dubbo Triple REST 的
   * ModelAttributeArgumentResolver 会读取注解的 required 属性，而 Spring
   * {@code @ModelAttribute} 无该属性，显式标注必 500（本服务 HTTP 由 Triple 承载）。
   */
  @GetMapping("/list")
  TableDataInfo list(@RequestParam(value = "templateCode", required = false) String templateCode,
                     @RequestParam(value = "templateName", required = false) String templateName,
                     @RequestParam(value = "category", required = false) String category,
                     @RequestParam(value = "status", required = false) String status);

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
