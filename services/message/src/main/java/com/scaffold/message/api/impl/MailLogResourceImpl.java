package com.scaffold.message.api.impl;

import com.scaffold.common.core.utils.poi.ExcelUtil;
import com.scaffold.common.core.web.controller.BaseController;
import com.scaffold.common.core.web.domain.AjaxResult;
import com.scaffold.common.core.web.page.TableDataInfo;
import com.scaffold.common.core.web.page.TableSupport;
import com.scaffold.common.log.annotation.Log;
import com.scaffold.common.log.enums.BusinessType;
import com.scaffold.common.security.annotation.RequiresPermissions;
import com.scaffold.message.api.MailLogResource;
import com.scaffold.message.domain.MsgMailLog;
import com.scaffold.message.service.IMessageSendService;
import com.scaffold.message.service.IMsgMailLogService;
import java.util.List;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 邮件发送记录实现（Triple REST）
 *
 * @author ct
 */
@DubboService
public class MailLogResourceImpl extends BaseController implements MailLogResource {
  private final IMsgMailLogService logService;

  private final IMessageSendService sendService;

  public MailLogResourceImpl(IMsgMailLogService logService, IMessageSendService sendService) {
    this.logService = logService;
    this.sendService = sendService;
  }

  @Override
  @RequiresPermissions("message:mailLog:list")
  public TableDataInfo list(MsgMailLog query) {
    return logService.queryPage(query, TableSupport.buildPageRequest());
  }

  @Override
  @RequiresPermissions("message:mailLog:query")
  public AjaxResult getInfo(@PathVariable("id") Long id) {
    return success(logService.queryById(id));
  }

  @Override
  @RequiresPermissions("message:mailLog:edit")
  @Log(title = "邮件失败重发", businessType = BusinessType.INSERT)
  public AjaxResult retry(@PathVariable("id") Long id) {
    return success(sendService.retryMail(id));
  }

  @Override
  @RequiresPermissions("message:mailLog:remove")
  @Log(title = "邮件记录清理", businessType = BusinessType.DELETE)
  public AjaxResult remove(@PathVariable("ids") Long[] ids) {
    return toAjax(logService.deleteByIds(ids));
  }

  @Override
  @RequiresPermissions("message:mailLog:export")
  @Log(title = "邮件记录导出", businessType = BusinessType.EXPORT)
  public byte[] export(MsgMailLog query) {
    List<MsgMailLog> list = logService.queryList(query);
    ExcelUtil<MsgMailLog> util = new ExcelUtil<MsgMailLog>(MsgMailLog.class);
    return util.exportExcel(list, "邮件发送记录");
  }
}
