package com.scaffold.job.service;

import com.scaffold.common.core.web.page.PageDomain;
import com.scaffold.common.core.web.page.TableDataInfo;

import java.util.List;
import org.quartz.SchedulerException;
import com.scaffold.common.core.exception.job.TaskException;
import com.scaffold.job.domain.SysJob;

/**
 * 定时任务调度信息信息 服务层
 * 
 * @author ct
 */
public interface ISysJobService
{
    /**
     * 获取quartz调度器的计划任务
     * 
     * @param job 调度信息
     * @return 调度任务集合
     */
    public List<SysJob> selectJobList(SysJob job);

    /**
     * 分页查询调度任务（JPA PageRequest 分页）
     *
     * @param job 查询条件
     * @param page 分页参数
     * @return 分页结果
     */
    public com.scaffold.common.core.web.page.TableDataInfo selectJobPage(SysJob job, com.scaffold.common.core.web.page.PageDomain page);

    /**
     * 通过调度任务ID查询调度信息
     * 
     * @param jobId 调度任务ID
     * @return 调度任务对象信息
     */
    public SysJob selectJobById(Long jobId);

    /**
     * 暂停任务
     * 
     * @param job 调度信息
     * @return 结果
     */
    public int pauseJob(SysJob job) throws SchedulerException;

    /**
     * 恢复任务
     * 
     * @param job 调度信息
     * @return 结果
     */
    public int resumeJob(SysJob job) throws SchedulerException;

    /**
     * 删除任务后，所对应的trigger也将被删除
     * 
     * @param job 调度信息
     * @return 结果
     */
    public int deleteJob(SysJob job) throws SchedulerException;

    /**
     * 批量删除调度信息
     * 
     * @param jobIds 需要删除的任务ID
     * @return 结果
     */
    public void deleteJobByIds(Long[] jobIds) throws SchedulerException;

    /**
     * 任务调度状态修改
     * 
     * @param job 调度信息
     * @return 结果
     */
    public int changeStatus(SysJob job) throws SchedulerException;

    /**
     * 立即运行任务
     * 
     * @param job 调度信息
     * @return 结果
     */
    public boolean run(SysJob job) throws SchedulerException;

    /**
     * 新增任务
     * 
     * @param job 调度信息
     * @return 结果
     */
    public int insertJob(SysJob job) throws SchedulerException, TaskException;

    /**
     * 更新任务
     * 
     * @param job 调度信息
     * @return 结果
     */
    public int updateJob(SysJob job) throws SchedulerException, TaskException;

    /**
     * 校验cron表达式是否有效
     * 
     * @param cronExpression 表达式
     * @return 结果
     */
    public boolean checkCronExpressionIsValid(String cronExpression);
}