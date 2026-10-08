package com.scaffold.message.api.impl;

import com.scaffold.common.core.web.controller.BaseController;
import com.scaffold.common.core.web.domain.AjaxResult;
import com.scaffold.common.core.web.page.TableDataInfo;
import com.scaffold.common.core.web.page.TableSupport;
import com.scaffold.common.log.annotation.Log;
import com.scaffold.common.log.enums.BusinessType;
import com.scaffold.common.security.annotation.RequiresPermissions;
import com.scaffold.message.api.InnerTemplateResource;
import com.scaffold.message.domain.MsgInnerTemplate;
import com.scaffold.message.service.IMsgInnerTemplateService;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 站内信模板实现（Triple REST）
 *
 * @author ct
 */
@DubboService
public class InnerTemplateResourceImpl extends BaseController implements InnerTemplateResource {
  private final IMsgInnerTemplateService templateService;

  public InnerTemplateResourceImpl(IMsgInnerTemplateService templateService) {
    this.templateService = templateService;
  }

  @Override
  @RequiresPermissions("message:innerTemplate:list")
  public TableDataInfo list(String templateCode, String templateName, String category, String status) {
    MsgInnerTemplate query = new MsgInnerTemplate();
    query.setTemplateCode(templateCode);
    query.setTemplateName(templateName);
    query.setCategory(category);
    query.setStatus(status);
    return templateService.queryPage(query, TableSupport.buildPageRequest());
  }

  @Override
  @RequiresPermissions("message:innerTemplate:query")
  public AjaxResult getInfo(@PathVariable("id") Long id) {
    return success(templateService.queryById(id));
  }

  @Override
  @RequiresPermissions("message:innerTemplate:add")
  @Log(title = "站内信模板", businessType = BusinessType.INSERT)
  public AjaxResult add(@RequestBody MsgInnerTemplate template) {
    return toAjax(templateService.insert(template));
  }

  @Override
  @RequiresPermissions("message:innerTemplate:edit")
  @Log(title = "站内信模板", businessType = BusinessType.UPDATE)
  public AjaxResult edit(@RequestBody MsgInnerTemplate template) {
    return toAjax(templateService.update(template));
  }

  @Override
  @RequiresPermissions("message:innerTemplate:remove")
  @Log(title = "站内信模板", businessType = BusinessType.DELETE)
  public AjaxResult remove(@PathVariable("ids") Long[] ids) {
    return toAjax(templateService.deleteByIds(ids));
  }

  @Override
  @RequiresPermissions("message:innerTemplate:edit")
  @Log(title = "站内信模板启停", businessType = BusinessType.UPDATE)
  public AjaxResult changeStatus(
      @RequestParam("id") Long id, @RequestParam("status") String status) {
    return toAjax(templateService.changeStatus(id, status));
  }
}
