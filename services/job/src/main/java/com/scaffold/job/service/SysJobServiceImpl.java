package com.scaffold.job.service;

import java.util.List;
import jakarta.annotation.PostConstruct;
import org.quartz.JobDataMap;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import com.scaffold.common.core.constant.ScheduleConstants;
import com.scaffold.common.core.exception.job.TaskException;
import com.scaffold.common.core.jpa.JpaSpecs;
import com.scaffold.common.core.utils.PageUtils;
import com.scaffold.common.core.web.page.TableDataInfo;
import com.scaffold.job.domain.SysJob;
import com.scaffold.job.repository.SysJobRepository;
import com.scaffold.job.util.CronUtils;
import com.scaffold.job.util.ScheduleUtils;

/**
 * 定时任务调度信息 服务层
 * 
 * @author ct
 */
@Service
public class SysJobServiceImpl implements ISysJobService
{
    private final Scheduler scheduler;
    private final SysJobRepository jobRepository;

    public SysJobServiceImpl(Scheduler scheduler, SysJobRepository jobRepository)
    {
        this.scheduler = scheduler;
        this.jobRepository = jobRepository;
    }

    /**
     * 项目启动时，初始化定时器 主要是防止手动修改数据库导致未同步到定时任务处理（注：不能手动修改数据库ID和任务组名，否则会导致脏数据）
     */
    @PostConstruct
    public void init() throws SchedulerException, TaskException
    {
        scheduler.clear();
        List<SysJob> jobList = jobRepository.findAll();
        for (SysJob job : jobList)
        {
            ScheduleUtils.createScheduleJob(scheduler, job);
        }
    }

    /**
     * 获取quartz调度器的计划任务列表
     * 
     * @param job 调度信息
     * @return
     */
    @Override
    public List<SysJob> selectJobList(SysJob job)
    {
        return jobRepository.list(toSpec(job), Sort.unsorted());
    }

    /**
     * 分页查询调度任务（JPA PageRequest 分页）
     */
    @Override
    public TableDataInfo selectJobPage(SysJob job, com.scaffold.common.core.web.page.PageDomain page)
    {
        return TableDataInfo.from(jobRepository.page(toSpec(job), PageUtils.toPageRequest(page)));
    }

    /**
     * 动态条件（对齐原 SysJobMapper.xml selectJobList）
     */
    private Specification<Object> toSpec(SysJob job)
    {
        if (job == null)
        {
            return JpaSpecs.alwaysTrue();
        }
        return JpaSpecs.likeIf("jobName", job.getJobName())
                .and(JpaSpecs.eqIfNotBlank("jobGroup", job.getJobGroup()))
                .and(JpaSpecs.eqIfNotBlank("status", job.getStatus()))
                .and(JpaSpecs.likeIf("invokeTarget", job.getInvokeTarget()));
    }

    /**
     * 通过调度任务ID查询调度信息
     * 
     * @param jobId 调度任务ID
     * @return 调度任务对象信息
     */
    @Override
    public SysJob selectJobById(Long jobId)
    {
        return jobRepository.findById(jobId).orElse(null);
    }

    /**
     * 暂停任务
     * 
     * @param job 调度信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int pauseJob(SysJob job) throws SchedulerException
    {
        Long jobId = job.getJobId();
        String jobGroup = job.getJobGroup();
        job.setStatus(ScheduleConstants.Status.PAUSE.getValue());
        int rows = updateRow(job);
        if (rows > 0)
        {
            scheduler.pauseJob(ScheduleUtils.getJobKey(jobId, jobGroup));
        }
        return rows;
    }

    /**
     * 恢复任务
     * 
     * @param job 调度信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int resumeJob(SysJob job) throws SchedulerException
    {
        Long jobId = job.getJobId();
        String jobGroup = job.getJobGroup();
        job.setStatus(ScheduleConstants.Status.NORMAL.getValue());
        int rows = updateRow(job);
        if (rows > 0)
        {
            scheduler.resumeJob(ScheduleUtils.getJobKey(jobId, jobGroup));
        }
        return rows;
    }

    /**
     * 删除任务后，所对应的trigger也将被删除
     * 
     * @param job 调度信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteJob(SysJob job) throws SchedulerException
    {
        Long jobId = job.getJobId();
        String jobGroup = job.getJobGroup();
        int rows = deleteRow(jobId);
        if (rows > 0)
        {
            scheduler.deleteJob(ScheduleUtils.getJobKey(jobId, jobGroup));
        }
        return rows;
    }

    /**
     * 批量删除调度信息
     * 
     * @param jobIds 需要删除的任务ID
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteJobByIds(Long[] jobIds) throws SchedulerException
    {
        for (Long jobId : jobIds)
        {
            SysJob job = jobRepository.findById(jobId).orElse(null);
            deleteJob(job);
        }
    }

    /**
     * 任务调度状态修改
     * 
     * @param job 调度信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int changeStatus(SysJob job) throws SchedulerException
    {
        int rows = 0;
        String status = job.getStatus();
        if (ScheduleConstants.Status.NORMAL.getValue().equals(status))
        {
            rows = resumeJob(job);
        }
        else if (ScheduleConstants.Status.PAUSE.getValue().equals(status))
        {
            rows = pauseJob(job);
        }
        return rows;
    }

    /**
     * 立即运行任务
     * 
     * @param job 调度信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean run(SysJob job) throws SchedulerException
    {
        boolean result = false;
        Long jobId = job.getJobId();
        String jobGroup = job.getJobGroup();
        SysJob properties = selectJobById(job.getJobId());
        // 参数
        JobDataMap dataMap = new JobDataMap();
        dataMap.put(ScheduleConstants.TASK_PROPERTIES, properties);
        JobKey jobKey = ScheduleUtils.getJobKey(jobId, jobGroup);
        if (scheduler.checkExists(jobKey))
        {
            result = true;
            scheduler.triggerJob(jobKey, dataMap);
        }
        return result;
    }

    /**
     * 新增任务
     * 
     * @param job 调度信息 调度信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertJob(SysJob job) throws SchedulerException, TaskException
    {
        job.setStatus(ScheduleConstants.Status.PAUSE.getValue());
        // IDENTITY 主键：save 即持久化并回填 jobId
        int rows = insertRow(job);
        if (rows > 0)
        {
            ScheduleUtils.createScheduleJob(scheduler, job);
        }
        return rows;
    }

    /**
     * 更新任务的时间表达式
     * 
     * @param job 调度信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateJob(SysJob job) throws SchedulerException, TaskException
    {
        SysJob properties = selectJobById(job.getJobId());
        int rows = updateRow(job);
        if (rows > 0)
        {
            updateSchedulerJob(job, properties.getJobGroup());
        }
        return rows;
    }

    /**
     * 更新任务
     * 
     * @param job 任务对象
     * @param jobGroup 任务组名
     */
    public void updateSchedulerJob(SysJob job, String jobGroup) throws SchedulerException, TaskException
    {
        Long jobId = job.getJobId();
        // 判断是否存在
        JobKey jobKey = ScheduleUtils.getJobKey(jobId, jobGroup);
        if (scheduler.checkExists(jobKey))
        {
            // 防止创建时存在数据问题 先移除，然后在执行创建操作
            scheduler.deleteJob(jobKey);
        }
        ScheduleUtils.createScheduleJob(scheduler, job);
    }

    /**
     * 新增（原 insertJob 动态插入语义对齐：IDENTITY 回填主键，恒 1 行）
     */
    private int insertRow(SysJob job)
    {
        jobRepository.save(job);
        return 1;
    }

    /**
     * 更新（@Version 乐观锁，对齐原 update 的 version 条件：冲突返回 0 而非抛异常）
     */
    private int updateRow(SysJob job)
    {
        try
        {
            // flush 让 @Version 冲突在事务内即暴露（对齐原 update 的 version 条件语义）
            jobRepository.saveAndFlush(job);
            return 1;
        }
        catch (ObjectOptimisticLockingFailureException e)
        {
            return 0;
        }
        catch (jakarta.persistence.OptimisticLockException e)
        {
            return 0;
        }
    }

    /**
     * 删除（存在性检查对齐原 delete 的受影响行数语义）
     */
    private int deleteRow(Long jobId)
    {
        if (jobRepository.existsById(jobId))
        {
            jobRepository.deleteById(jobId);
            return 1;
        }
        return 0;
    }

    /**
     * 校验cron表达式是否有效
     * 
     * @param cronExpression 表达式
     * @return 结果
     */
    @Override
    public boolean checkCronExpressionIsValid(String cronExpression)
    {
        return CronUtils.isValid(cronExpression);
    }
}