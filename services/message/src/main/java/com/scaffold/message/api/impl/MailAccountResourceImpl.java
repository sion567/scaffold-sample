package com.scaffold.message.api.impl;

import com.scaffold.common.core.web.controller.BaseController;
import com.scaffold.common.core.web.domain.AjaxResult;
import com.scaffold.common.core.web.page.TableDataInfo;
import com.scaffold.common.core.web.page.TableSupport;
import com.scaffold.common.log.annotation.Log;
import com.scaffold.common.log.enums.BusinessType;
import com.scaffold.common.security.annotation.RequiresPermissions;
import com.scaffold.message.api.MailAccountResource;
import com.scaffold.message.domain.MsgMailAccount;
import com.scaffold.message.service.IMsgMailAccountService;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 邮箱账号实现（Triple REST）
 *
 * @author ct
 */
@DubboService
public class MailAccountResourceImpl extends BaseController implements MailAccountResource {
  private final IMsgMailAccountService accountService;

  public MailAccountResourceImpl(IMsgMailAccountService accountService) {
    this.accountService = accountService;
  }

  @Override
  @RequiresPermissions("message:mailAccount:list")
  public TableDataInfo list(MsgMailAccount query) {
    return accountService.queryPage(query, TableSupport.buildPageRequest());
  }

  @Override
  @RequiresPermissions("message:mailAccount:query")
  public AjaxResult getInfo(@PathVariable("id") Long id) {
    return success(accountService.queryById(id));
  }

  @Override
  @RequiresPermissions("message:mailAccount:add")
  @Log(title = "邮箱账号", businessType = BusinessType.INSERT)
  public AjaxResult add(@RequestBody MsgMailAccount account) {
    return toAjax(accountService.insert(account));
  }

  @Override
  @RequiresPermissions("message:mailAccount:edit")
  @Log(title = "邮箱账号", businessType = BusinessType.UPDATE)
  public AjaxResult edit(@RequestBody MsgMailAccount account) {
    return toAjax(accountService.update(account));
  }

  @Override
  @RequiresPermissions("message:mailAccount:remove")
  @Log(title = "邮箱账号", businessType = BusinessType.DELETE)
  public AjaxResult remove(@PathVariable("ids") Long[] ids) {
    return toAjax(accountService.deleteByIds(ids));
  }

  @Override
  @RequiresPermissions("message:mailAccount:edit")
  @Log(title = "邮箱账号启停", businessType = BusinessType.UPDATE)
  public AjaxResult changeStatus(
      @RequestParam("id") Long id, @RequestParam("status") String status) {
    return toAjax(accountService.changeStatus(id, status));
  }

  @Override
  @RequiresPermissions("message:mailAccount:edit")
  @Log(title = "邮箱账号连通测试", businessType = BusinessType.INSERT)
  public AjaxResult test(@PathVariable("id") Long id, @RequestParam("toAddr") String toAddr) {
    return success(accountService.testAccount(id, toAddr));
  }
}
