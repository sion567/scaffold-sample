package com.scaffold.message.api.impl;

import com.scaffold.common.core.web.controller.BaseController;
import com.scaffold.common.core.web.domain.AjaxResult;
import com.scaffold.common.core.web.page.TableDataInfo;
import com.scaffold.common.core.web.page.TableSupport;
import com.scaffold.common.log.annotation.Log;
import com.scaffold.common.log.enums.BusinessType;
import com.scaffold.common.security.annotation.RequiresPermissions;
import com.scaffold.message.api.SmsChannelResource;
import com.scaffold.message.domain.MsgSmsChannel;
import com.scaffold.message.service.IMsgSmsChannelService;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 短信渠道实现（Triple REST）
 *
 * @author ct
 */
@DubboService
public class SmsChannelResourceImpl extends BaseController implements SmsChannelResource {
  private final IMsgSmsChannelService channelService;

  public SmsChannelResourceImpl(IMsgSmsChannelService channelService) {
    this.channelService = channelService;
  }

  @Override
  @RequiresPermissions("message:smsChannel:list")
  public TableDataInfo list(MsgSmsChannel query) {
    return channelService.queryPage(query, TableSupport.buildPageRequest());
  }

  @Override
  @RequiresPermissions("message:smsChannel:query")
  public AjaxResult getInfo(@PathVariable("id") Long id) {
    return success(channelService.queryById(id));
  }

  @Override
  @RequiresPermissions("message:smsChannel:add")
  @Log(title = "短信渠道", businessType = BusinessType.INSERT)
  public AjaxResult add(@RequestBody MsgSmsChannel channel) {
    return toAjax(channelService.insert(channel));
  }

  @Override
  @RequiresPermissions("message:smsChannel:edit")
  @Log(title = "短信渠道", businessType = BusinessType.UPDATE)
  public AjaxResult edit(@RequestBody MsgSmsChannel channel) {
    return toAjax(channelService.update(channel));
  }

  @Override
  @RequiresPermissions("message:smsChannel:remove")
  @Log(title = "短信渠道", businessType = BusinessType.DELETE)
  public AjaxResult remove(@PathVariable("ids") Long[] ids) {
    return toAjax(channelService.deleteByIds(ids));
  }

  @Override
  @RequiresPermissions("message:smsChannel:edit")
  @Log(title = "短信渠道启停", businessType = BusinessType.UPDATE)
  public AjaxResult changeStatus(
      @RequestParam("id") Long id, @RequestParam("status") String status) {
    return toAjax(channelService.changeStatus(id, status));
  }

  @Override
  @RequiresPermissions("message:smsChannel:edit")
  @Log(title = "短信渠道连通测试", businessType = BusinessType.INSERT)
  public AjaxResult test(@PathVariable("id") Long id, @RequestParam("mobile") String mobile) {
    return success(channelService.testChannel(id, mobile));
  }
}
