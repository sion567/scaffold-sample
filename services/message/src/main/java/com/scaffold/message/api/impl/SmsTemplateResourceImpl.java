package com.scaffold.message.api.impl;

import com.scaffold.common.core.web.controller.BaseController;
import com.scaffold.common.core.web.domain.AjaxResult;
import com.scaffold.common.core.web.page.TableDataInfo;
import com.scaffold.common.core.web.page.TableSupport;
import com.scaffold.common.log.annotation.Log;
import com.scaffold.common.log.enums.BusinessType;
import com.scaffold.common.security.annotation.RequiresPermissions;
import com.scaffold.message.api.SmsTemplateResource;
import com.scaffold.message.domain.MsgSmsTemplate;
import com.scaffold.message.service.IMsgSmsTemplateService;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 短信模板实现（Triple REST）
 *
 * @author ct
 */
@DubboService
public class SmsTemplateResourceImpl extends BaseController implements SmsTemplateResource {
  private final IMsgSmsTemplateService templateService;

  public SmsTemplateResourceImpl(IMsgSmsTemplateService templateService) {
    this.templateService = templateService;
  }

  @Override
  @RequiresPermissions("message:smsTemplate:list")
  public TableDataInfo list(String templateCode, String templateName, Long channelId, String category, String status) {
    MsgSmsTemplate query = new MsgSmsTemplate();
    query.setTemplateCode(templateCode);
    query.setTemplateName(templateName);
    query.setChannelId(channelId);
    query.setCategory(category);
    query.setStatus(status);
    return templateService.queryPage(query, TableSupport.buildPageRequest());
  }

  @Override
  @RequiresPermissions("message:smsTemplate:query")
  public AjaxResult getInfo(@PathVariable("id") Long id) {
    return success(templateService.queryById(id));
  }

  @Override
  @RequiresPermissions("message:smsTemplate:add")
  @Log(title = "短信模板", businessType = BusinessType.INSERT)
  public AjaxResult add(@RequestBody MsgSmsTemplate template) {
    return toAjax(templateService.insert(template));
  }

  @Override
  @RequiresPermissions("message:smsTemplate:edit")
  @Log(title = "短信模板", businessType = BusinessType.UPDATE)
  public AjaxResult edit(@RequestBody MsgSmsTemplate template) {
    return toAjax(templateService.update(template));
  }

  @Override
  @RequiresPermissions("message:smsTemplate:remove")
  @Log(title = "短信模板", businessType = BusinessType.DELETE)
  public AjaxResult remove(@PathVariable("ids") Long[] ids) {
    return toAjax(templateService.deleteByIds(ids));
  }

  @Override
  @RequiresPermissions("message:smsTemplate:edit")
  @Log(title = "短信模板启停", businessType = BusinessType.UPDATE)
  public AjaxResult changeStatus(
      @RequestParam("id") Long id, @RequestParam("status") String status) {
    return toAjax(templateService.changeStatus(id, status));
  }
}
