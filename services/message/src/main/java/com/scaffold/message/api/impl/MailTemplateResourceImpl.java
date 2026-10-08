package com.scaffold.message.api.impl;

import com.scaffold.common.core.web.controller.BaseController;
import com.scaffold.common.core.web.domain.AjaxResult;
import com.scaffold.common.core.web.page.TableDataInfo;
import com.scaffold.common.core.web.page.TableSupport;
import com.scaffold.common.log.annotation.Log;
import com.scaffold.common.log.enums.BusinessType;
import com.scaffold.common.security.annotation.RequiresPermissions;
import com.scaffold.message.api.MailTemplateResource;
import com.scaffold.message.domain.MsgMailTemplate;
import com.scaffold.message.service.IMsgMailTemplateService;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 邮件模板实现（Triple REST）
 *
 * @author ct
 */
@DubboService
public class MailTemplateResourceImpl extends BaseController implements MailTemplateResource {
  private final IMsgMailTemplateService templateService;

  public MailTemplateResourceImpl(IMsgMailTemplateService templateService) {
    this.templateService = templateService;
  }

  @Override
  @RequiresPermissions("message:mailTemplate:list")
  public TableDataInfo list(String templateCode, String templateName, Long accountId, String category, String status) {
    MsgMailTemplate query = new MsgMailTemplate();
    query.setTemplateCode(templateCode);
    query.setTemplateName(templateName);
    query.setAccountId(accountId);
    query.setCategory(category);
    query.setStatus(status);
    return templateService.queryPage(query, TableSupport.buildPageRequest());
  }

  @Override
  @RequiresPermissions("message:mailTemplate:query")
  public AjaxResult getInfo(@PathVariable("id") Long id) {
    return success(templateService.queryById(id));
  }

  @Override
  @RequiresPermissions("message:mailTemplate:add")
  @Log(title = "邮件模板", businessType = BusinessType.INSERT)
  public AjaxResult add(@RequestBody MsgMailTemplate template) {
    return toAjax(templateService.insert(template));
  }

  @Override
  @RequiresPermissions("message:mailTemplate:edit")
  @Log(title = "邮件模板", businessType = BusinessType.UPDATE)
  public AjaxResult edit(@RequestBody MsgMailTemplate template) {
    return toAjax(templateService.update(template));
  }

  @Override
  @RequiresPermissions("message:mailTemplate:remove")
  @Log(title = "邮件模板", businessType = BusinessType.DELETE)
  public AjaxResult remove(@PathVariable("ids") Long[] ids) {
    return toAjax(templateService.deleteByIds(ids));
  }

  @Override
  @RequiresPermissions("message:mailTemplate:edit")
  @Log(title = "邮件模板启停", businessType = BusinessType.UPDATE)
  public AjaxResult changeStatus(
      @RequestParam("id") Long id, @RequestParam("status") String status) {
    return toAjax(templateService.changeStatus(id, status));
  }
}
