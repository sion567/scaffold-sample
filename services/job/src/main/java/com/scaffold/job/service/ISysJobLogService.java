package com.scaffold.job.service;

import java.util.List;
import com.scaffold.job.domain.SysJobLog;

/**
 * 定时任务调度日志信息信息 服务层
 * 
 * @author ct
 */
public interface ISysJobLogService
{
    /**
     * 获取quartz调度器日志的计划任务
     * 
     * @param jobLog 调度日志信息
     * @return 调度任务日志集合
     */
    public List<SysJobLog> selectJobLogList(SysJobLog jobLog);

    /**
     * 分页查询调度日志（JPA PageRequest 分页）
     *
     * @param jobLog 查询条件
     * @param page 分页参数
     * @return 分页结果
     */
    public com.scaffold.common.core.web.page.TableDataInfo selectJobLogPage(SysJobLog jobLog, com.scaffold.common.core.web.page.PageDomain page);

    /**
     * 通过调度任务日志ID查询调度信息
     * 
     * @param jobLogId 调度任务日志ID
     * @return 调度任务日志对象信息
     */
    public SysJobLog selectJobLogById(Long jobLogId);

    /**
     * 新增任务日志
     * 
     * @param jobLog 调度日志信息
     */
    public void addJobLog(SysJobLog jobLog);

    /**
     * 批量删除调度日志信息
     * 
     * @param logIds 需要删除的日志ID
     * @return 结果
     */
    public int deleteJobLogByIds(Long[] logIds);

    /**
     * 删除任务日志
     * 
     * @param jobId 调度日志ID
     * @return 结果
     */
    public int deleteJobLogById(Long jobId);

    /**
     * 清空任务日志
     */
    public void cleanJobLog();
}
