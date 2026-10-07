package com.scaffold.message.api.impl;

import com.scaffold.common.core.utils.poi.ExcelUtil;
import com.scaffold.common.core.web.controller.BaseController;
import com.scaffold.common.core.web.domain.AjaxResult;
import com.scaffold.common.core.web.page.TableDataInfo;
import com.scaffold.common.core.web.page.TableSupport;
import com.scaffold.common.log.annotation.Log;
import com.scaffold.common.log.enums.BusinessType;
import com.scaffold.common.security.annotation.RequiresPermissions;
import com.scaffold.message.api.SmsLogResource;
import com.scaffold.message.domain.MsgSmsLog;
import com.scaffold.message.service.IMessageSendService;
import com.scaffold.message.service.IMsgSmsLogService;
import java.util.List;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 短信发送日志实现（Triple REST；导出手机号中间四位脱敏）
 *
 * @author ct
 */
@DubboService
public class SmsLogResourceImpl extends BaseController implements SmsLogResource {
  private final IMsgSmsLogService logService;

  private final IMessageSendService sendService;

  public SmsLogResourceImpl(IMsgSmsLogService logService, IMessageSendService sendService) {
    this.logService = logService;
    this.sendService = sendService;
  }

  @Override
  @RequiresPermissions("message:smsLog:list")
  public TableDataInfo list(MsgSmsLog query) {
    return logService.queryPage(query, TableSupport.buildPageRequest());
  }

  @Override
  @RequiresPermissions("message:smsLog:query")
  public AjaxResult getInfo(@PathVariable("id") Long id) {
    return success(logService.queryById(id));
  }

  @Override
  @RequiresPermissions("message:smsLog:edit")
  @Log(title = "短信失败重发", businessType = BusinessType.INSERT)
  public AjaxResult retry(@PathVariable("id") Long id) {
    return success(sendService.retrySms(id));
  }

  @Override
  @RequiresPermissions("message:smsLog:remove")
  @Log(title = "短信日志清理", businessType = BusinessType.DELETE)
  public AjaxResult remove(@PathVariable("ids") Long[] ids) {
    return toAjax(logService.deleteByIds(ids));
  }

  @Override
  @RequiresPermissions("message:smsLog:export")
  @Log(title = "短信日志导出", businessType = BusinessType.EXPORT)
  public byte[] export(MsgSmsLog query) {
    List<MsgSmsLog> list = logService.queryList(query);
    for (MsgSmsLog log : list) {
      log.setMobile(maskMobile(log.getMobile()));
    }
    ExcelUtil<MsgSmsLog> util = new ExcelUtil<MsgSmsLog>(MsgSmsLog.class);
    return util.exportExcel(list, "短信发送日志");
  }

  /** 手机号中间四位脱敏（138****5678） */
  static String maskMobile(String mobile) {
    if (mobile == null || mobile.length() < 7) {
      return mobile;
    }
    return mobile.substring(0, 3) + "****" + mobile.substring(mobile.length() - 4);
  }
}
