package com.scaffold.job.service;

import java.util.Arrays;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.scaffold.common.core.jpa.JpaSpecs;
import com.scaffold.common.core.utils.PageUtils;
import com.scaffold.common.core.web.page.TableDataInfo;
import org.springframework.stereotype.Service;
import com.scaffold.job.domain.SysJobLog;
import com.scaffold.job.repository.SysJobLogRepository;

/**
 * 定时任务调度日志信息 服务层
 * 
 * @author ct
 */
@Service
public class SysJobLogServiceImpl implements ISysJobLogService
{
    private final SysJobLogRepository jobLogRepository;

    public SysJobLogServiceImpl(SysJobLogRepository jobLogRepository)
    {
        this.jobLogRepository = jobLogRepository;
    }

    /**
     * 获取quartz调度器日志的计划任务
     * 
     * @param jobLog 调度日志信息
     * @return 调度任务日志集合
     */
    @Override
    public List<SysJobLog> selectJobLogList(SysJobLog jobLog)
    {
        return jobLogRepository.list(toSpec(jobLog), Sort.by(Sort.Direction.DESC, "createTime"));
    }

    /**
     * 分页查询调度日志（JPA PageRequest 分页，create_time 倒序对齐原 SQL）
     */
    @Override
    public TableDataInfo selectJobLogPage(SysJobLog jobLog, com.scaffold.common.core.web.page.PageDomain page)
    {
        org.springframework.data.domain.PageRequest pageable = PageUtils.toPageRequest(page);
        if (pageable.getSort().isUnsorted())
        {
            pageable = org.springframework.data.domain.PageRequest.of(
                    pageable.getPageNumber(), pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "createTime"));
        }
        return TableDataInfo.from(jobLogRepository.page(toSpec(jobLog), pageable));
    }

    /**
     * 动态条件（对齐原 SysJobLogMapper.xml selectJobLogList）
     */
    private Specification<Object> toSpec(SysJobLog jobLog)
    {
        if (jobLog == null)
        {
            return JpaSpecs.alwaysTrue();
        }
        return JpaSpecs.likeIf("jobName", jobLog.getJobName())
                .and(JpaSpecs.eqIfNotBlank("jobGroup", jobLog.getJobGroup()))
                .and(JpaSpecs.eqIfNotBlank("status", jobLog.getStatus()))
                .and(JpaSpecs.likeIf("invokeTarget", jobLog.getInvokeTarget()))
                .and(JpaSpecs.dateRangeIf("createTime", jobLog.getParams()));
    }

    /**
     * 通过调度任务日志ID查询调度信息
     * 
     * @param jobLogId 调度任务日志ID
     * @return 调度任务日志对象信息
     */
    @Override
    public SysJobLog selectJobLogById(Long jobLogId)
    {
        return jobLogRepository.findById(jobLogId).orElse(null);
    }

    /**
     * 新增任务日志
     * 
     * @param jobLog 调度日志信息
     */
    @Override
    public void addJobLog(SysJobLog jobLog)
    {
        jobLogRepository.save(jobLog);
    }

    /**
     * 批量删除调度日志信息
     * 
     * @param logIds 需要删除的数据ID
     * @return 结果
     */
    @Override
    public int deleteJobLogByIds(Long[] logIds)
    {
        jobLogRepository.deleteAllById(Arrays.asList(logIds));
        return logIds.length;
    }

    /**
     * 删除任务日志
     * 
     * @param jobId 调度日志ID
     */
    @Override
    public int deleteJobLogById(Long jobId)
    {
        if (jobLogRepository.existsById(jobId))
        {
            jobLogRepository.deleteById(jobId);
            return 1;
        }
        return 0;
    }

    /**
     * 清空任务日志
     */
    @Override
    public void cleanJobLog()
    {
        // 等价原 truncate：整表一次性删除，不逐行
        jobLogRepository.deleteAllInBatch();
    }
}
