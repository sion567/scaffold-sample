package com.scaffold.job.api.impl;

import java.util.Date;

import org.apache.dubbo.config.annotation.DubboService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.scaffold.job.api.JobExecLog;
import com.scaffold.job.api.RemoteJobLogApi;
import com.scaffold.job.domain.SysJobLog;
import com.scaffold.job.service.ISysJobLogService;

/**
 * 任务执行日志回写实现（scaffold-api-job 契约，Triple REST 对外）：
 * 业务服务本地定时任务的执行结果落库 sys_job_log，任务中心统一可见。
 *
 * <p>best-effort：落库失败只记本服务日志，不向调用方抛异常（调用方本身也是
 * 吞异常降级，双向兜底保证调度不被日志链路阻断）。</p>
 *
 * @author scaffold
 */
@DubboService
public class RemoteJobLogApiImpl implements RemoteJobLogApi
{
    private static final Logger log = LoggerFactory.getLogger(RemoteJobLogApiImpl.class);

    private final ISysJobLogService jobLogService;

    public RemoteJobLogApiImpl(ISysJobLogService jobLogService)
    {
        this.jobLogService = jobLogService;
    }

    @Override
    public void record(JobExecLog jobLog)
    {
        try
        {
            SysJobLog entry = new SysJobLog();
            entry.setJobName(jobLog.getJobName());
            entry.setJobGroup(jobLog.getJobGroup());
            entry.setInvokeTarget(jobLog.getInvokeTarget());
            entry.setJobMessage(jobLog.getJobMessage());
            entry.setStatus(jobLog.getStatus());
            entry.setExceptionInfo(jobLog.getExceptionInfo());
            entry.setStartTime(jobLog.getStartTime() == null ? new Date() : jobLog.getStartTime());
            entry.setEndTime(jobLog.getEndTime() == null ? new Date() : jobLog.getEndTime());
            jobLogService.addJobLog(entry);
        }
        catch (Exception e)
        {
            log.error("[remote-job-log] 任务执行日志落库失败: {}", jobLog, e);
        }
    }
}
