package com.scaffold.job.api.impl;

import java.util.List;
import org.springframework.web.bind.annotation.RequestParam;
import org.apache.dubbo.config.annotation.DubboService;
import com.scaffold.common.core.utils.poi.ExcelUtil;
import com.scaffold.common.core.web.controller.BaseController;
import com.scaffold.common.core.web.domain.AjaxResult;
import com.scaffold.common.core.web.page.PageDomain;
import com.scaffold.common.core.web.page.TableDataInfo;
import com.scaffold.common.log.annotation.Log;
import com.scaffold.common.log.enums.BusinessType;
import com.scaffold.common.security.annotation.RequiresPermissions;
import com.scaffold.job.api.SysJobLogResource;
import com.scaffold.job.domain.SysJobLog;
import com.scaffold.job.service.ISysJobLogService;

/**
 * 定时任务调度日志信息服务实现（Triple REST）
 *
 * @author ct
 */
@DubboService
public class SysJobLogResourceImpl extends BaseController implements SysJobLogResource
{
    private final ISysJobLogService jobLogService;

    public SysJobLogResourceImpl(ISysJobLogService jobLogService)
    {
        this.jobLogService = jobLogService;
    }

    @RequiresPermissions("monitor:job:list")
    @Override
    public TableDataInfo list(Integer pageNum, Integer pageSize, String orderByColumn, String isAsc, String jobName, String jobGroup, String status)
    {
        PageDomain page = new PageDomain();
        page.setPageNum(pageNum);
        page.setPageSize(pageSize);
        page.setOrderByColumn(orderByColumn);
        page.setIsAsc(isAsc);
        SysJobLog jobLog = new SysJobLog();
        jobLog.setJobName(jobName);
        jobLog.setJobGroup(jobGroup);
        jobLog.setStatus(status);
        return jobLogService.selectJobLogPage(jobLog, page);
    }

    @RequiresPermissions("monitor:job:export")
    @Log(title = "任务调度日志", businessType = BusinessType.EXPORT)
    @Override
    public byte[] export(String jobName, String jobGroup, String status)
    {
        SysJobLog jobLog = new SysJobLog();
        jobLog.setJobName(jobName);
        jobLog.setJobGroup(jobGroup);
        jobLog.setStatus(status);
        List<SysJobLog> list = jobLogService.selectJobLogList(jobLog);
        ExcelUtil<SysJobLog> util = new ExcelUtil<SysJobLog>(SysJobLog.class);
        return util.exportExcel(list, "调度日志");
    }

    @RequiresPermissions("monitor:job:query")
    @Override
    public AjaxResult getInfo(Long jobLogId)
    {
        return success(jobLogService.selectJobLogById(jobLogId));
    }

    @RequiresPermissions("monitor:job:remove")
    @Log(title = "定时任务调度日志", businessType = BusinessType.DELETE)
    @Override
    public AjaxResult remove(Long[] jobLogIds)
    {
        return toAjax(jobLogService.deleteJobLogByIds(jobLogIds));
    }

    @RequiresPermissions("monitor:job:remove")
    @Log(title = "调度日志", businessType = BusinessType.CLEAN)
    @Override
    public AjaxResult clean()
    {
        jobLogService.cleanJobLog();
        return success();
    }
}
