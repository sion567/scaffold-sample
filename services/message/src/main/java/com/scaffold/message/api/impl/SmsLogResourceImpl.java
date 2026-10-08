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
  public TableDataInfo list(String mobile, String sendStatus, String templateCode, String channelType,
                            String bizType, String beginTime, String endTime) {
    return logService.queryPage(buildLogQuery(mobile, sendStatus, templateCode, channelType, bizType, beginTime, endTime),
        TableSupport.buildPageRequest());
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
  public byte[] export(String mobile, String sendStatus, String templateCode, String channelType,
                       String bizType, String beginTime, String endTime) {
    List<MsgSmsLog> list = logService.queryList(
        buildLogQuery(mobile, sendStatus, templateCode, channelType, bizType, beginTime, endTime));
    for (MsgSmsLog log : list) {
      log.setMobile(maskMobile(log.getMobile()));
    }
    ExcelUtil<MsgSmsLog> util = new ExcelUtil<MsgSmsLog>(MsgSmsLog.class);
    return util.exportExcel(list, "短信日志");
  }

  /** 日志查询条件（接口层过滤字段摊平后的装配；时间走 params 供 dateRangeIf 消费） */
  private MsgSmsLog buildLogQuery(String mobile, String sendStatus, String templateCode, String channelType,
                                  String bizType, String beginTime, String endTime) {
    MsgSmsLog query = new MsgSmsLog();
    query.setMobile(mobile);
    query.setSendStatus(sendStatus);
    query.setTemplateCode(templateCode);
    query.setChannelType(channelType);
    query.setBizType(bizType);
    query.getParams().put("beginTime", beginTime);
    query.getParams().put("endTime", endTime);
    return query;
  }

  /** 手机号中间四位脱敏（138****5678） */
  static String maskMobile(String mobile) {
    if (mobile == null || mobile.length() < 7) {
      return mobile;
    }
    return mobile.substring(0, 3) + "****" + mobile.substring(mobile.length() - 4);
  }
}
