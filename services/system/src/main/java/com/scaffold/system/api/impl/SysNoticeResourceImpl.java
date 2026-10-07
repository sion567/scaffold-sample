package com.scaffold.system.api.impl;

import java.util.List;
import org.springframework.web.bind.annotation.RequestParam;
import org.apache.dubbo.config.annotation.DubboService;
import com.scaffold.common.core.text.Convert;
import com.scaffold.common.core.utils.PageUtils;
import com.scaffold.common.core.web.controller.BaseController;
import com.scaffold.common.core.web.domain.AjaxResult;
import com.scaffold.common.core.web.page.PageDomain;
import com.scaffold.common.core.web.page.TableDataInfo;
import com.scaffold.common.log.annotation.Log;
import com.scaffold.common.log.enums.BusinessType;
import com.scaffold.common.security.annotation.RequiresPermissions;
import com.scaffold.common.security.utils.SecurityUtils;
import com.scaffold.system.api.SysNoticeResource;
import com.scaffold.system.domain.SysNotice;
import com.scaffold.system.service.ISysNoticeReadService;
import com.scaffold.system.service.ISysNoticeService;

/**
 * 通知公告信息服务实现（Triple REST）
 *
 * @author scaffold
 */
@DubboService
public class SysNoticeResourceImpl extends BaseController implements SysNoticeResource
{
    private final ISysNoticeService noticeService;
    private final ISysNoticeReadService noticeReadService;

    public SysNoticeResourceImpl(ISysNoticeService noticeService, ISysNoticeReadService noticeReadService)
    {
        this.noticeService = noticeService;
        this.noticeReadService = noticeReadService;
    }

    @RequiresPermissions("system:notice:list")
    @Override
    public TableDataInfo list(Integer pageNum, Integer pageSize, String orderByColumn, String isAsc,
                              Long noticeId, String noticeTitle, String noticeType, String status)
    {
        PageDomain page = new PageDomain();
        page.setPageNum(pageNum);
        page.setPageSize(pageSize);
        page.setOrderByColumn(orderByColumn);
        page.setIsAsc(isAsc);
        SysNotice notice = new SysNotice();
        notice.setNoticeId(noticeId);
        notice.setNoticeTitle(noticeTitle);
        notice.setNoticeType(noticeType);
        notice.setStatus(status);
        return noticeService.selectNoticePage(notice, page);
    }

    @Override
    public AjaxResult getInfo(Long noticeId)
    {
        return success(noticeService.selectNoticeById(noticeId));
    }

    @RequiresPermissions("system:notice:add")
    @Log(title = "通知公告", businessType = BusinessType.INSERT)
    @Override
    public AjaxResult add(SysNotice notice)
    {
        notice.setCreateBy(SecurityUtils.getUsername());
        return toAjax(noticeService.insertNotice(notice));
    }

    @RequiresPermissions("system:notice:edit")
    @Log(title = "通知公告", businessType = BusinessType.UPDATE)
    @Override
    public AjaxResult edit(SysNotice notice)
    {
        notice.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(noticeService.updateNotice(notice));
    }

    @Override
    public AjaxResult listTop()
    {
        Long userId = SecurityUtils.getUserId();
        List<SysNotice> list = noticeReadService.selectNoticeListWithReadStatus(userId, 5);
        long unreadCount = list.stream().filter(n -> !Boolean.TRUE.equals(n.getIsRead())).count();
        AjaxResult result = AjaxResult.success(list);
        result.put("unreadCount", unreadCount);
        return result;
    }

    @Override
    public AjaxResult markRead(Long noticeId)
    {
        Long userId = SecurityUtils.getUserId();
        noticeReadService.markRead(noticeId, userId);
        return success();
    }

    @Override
    public AjaxResult markReadAll(String ids)
    {
        Long userId = SecurityUtils.getUserId();
        Long[] noticeIds = Convert.toLongArray(ids);
        noticeReadService.markReadBatch(userId, noticeIds);
        return success();
    }

    @RequiresPermissions("system:notice:list")
    @Override
    public TableDataInfo readUsersList(Integer pageNum, Integer pageSize, String orderByColumn, String isAsc,
                                       Long noticeId, String searchValue)
    {
        PageDomain page = new PageDomain();
        page.setPageNum(pageNum);
        page.setPageSize(pageSize);
        page.setOrderByColumn(orderByColumn);
        page.setIsAsc(isAsc);
        return noticeReadService.selectReadUsersPage(noticeId, searchValue, page);
    }

    @RequiresPermissions("system:notice:remove")
    @Log(title = "通知公告", businessType = BusinessType.DELETE)
    @Override
    public AjaxResult remove(Long[] noticeIds)
    {
        noticeReadService.deleteByNoticeIds(noticeIds);
        return toAjax(noticeService.deleteNoticeByIds(noticeIds));
    }
}
