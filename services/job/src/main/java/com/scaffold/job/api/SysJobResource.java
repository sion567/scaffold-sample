package com.scaffold.job.api;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.quartz.SchedulerException;
import com.scaffold.common.core.exception.job.TaskException;
import com.scaffold.common.core.web.domain.AjaxResult;
import com.scaffold.common.core.web.page.TableDataInfo;
import com.scaffold.job.domain.SysJob;

/**
 * 定时任务信息服务（Triple REST 对外接口）
 *
 * @author ct
 */
@RequestMapping("/job")
public interface SysJobResource
{
    /**
     * 查询定时任务列表
     */
    @GetMapping("/list")
    TableDataInfo list(@RequestParam(value = "pageNum", required = false) Integer pageNum,
                       @RequestParam(value = "pageSize", required = false) Integer pageSize,
                       @RequestParam(value = "orderByColumn", required = false) String orderByColumn,
                       @RequestParam(value = "isAsc", required = false) String isAsc,
                       @RequestParam(value = "jobName", required = false) String jobName,
                       @RequestParam(value = "jobGroup", required = false) String jobGroup,
                       @RequestParam(value = "status", required = false) String status);

    /**
     * 导出定时任务列表
     */
    @PostMapping("/export")
    @RequestMapping(value = "/export", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    byte[] export(@RequestParam(value = "jobName", required = false) String jobName,
                   @RequestParam(value = "jobGroup", required = false) String jobGroup,
                   @RequestParam(value = "status", required = false) String status);

    /**
     * 获取定时任务详细信息
     */
    @GetMapping("/{jobId}")
    AjaxResult getInfo(@PathVariable("jobId") Long jobId);

    /**
     * 新增定时任务
     */
    @PostMapping
    AjaxResult add(@RequestBody SysJob job) throws SchedulerException, TaskException;

    /**
     * 修改定时任务
     */
    @PutMapping
    AjaxResult edit(@RequestBody SysJob job) throws SchedulerException, TaskException;

    /**
     * 定时任务状态修改
     */
    @PutMapping("/changeStatus")
    AjaxResult changeStatus(@RequestBody SysJob job) throws SchedulerException;

    /**
     * 定时任务立即执行一次
     */
    @PutMapping("/run")
    AjaxResult run(@RequestBody SysJob job) throws SchedulerException;

    /**
     * 删除定时任务
     */
    @DeleteMapping("/{jobIds}")
    AjaxResult remove(@RequestParam("jobIds") Long[] jobIds) throws SchedulerException;
}
