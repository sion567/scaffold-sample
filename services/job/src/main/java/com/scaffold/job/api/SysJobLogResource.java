package com.scaffold.job.api;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.scaffold.common.core.web.domain.AjaxResult;
import com.scaffold.common.core.web.page.TableDataInfo;
import com.scaffold.job.domain.SysJobLog;

/**
 * 定时任务调度日志信息服务（Triple REST 对外接口）
 *
 * @author ct
 */
@RequestMapping("/job/log")
public interface SysJobLogResource
{
    /**
     * 查询定时任务调度日志列表
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
     * 导出定时任务调度日志列表
     */
    @PostMapping("/export")
    @RequestMapping(value = "/export", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    byte[] export(@RequestParam(value = "jobName", required = false) String jobName,
                  @RequestParam(value = "jobGroup", required = false) String jobGroup,
                  @RequestParam(value = "status", required = false) String status);

    /**
     * 根据调度编号获取详细信息
     */
    @GetMapping("/{jobLogId}")
    AjaxResult getInfo(@PathVariable("jobLogId") Long jobLogId);

    /**
     * 删除定时任务调度日志
     */
    @DeleteMapping("/{jobLogIds}")
    AjaxResult remove(@RequestParam("jobLogIds") Long[] jobLogIds);

    /**
     * 清空定时任务调度日志
     */
    @DeleteMapping("/clean")
    AjaxResult clean();
}
