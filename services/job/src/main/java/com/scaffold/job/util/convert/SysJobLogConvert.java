package com.scaffold.job.util.convert;

import com.scaffold.job.domain.SysJob;
import com.scaffold.job.domain.SysJobLog;

/**
 * SysJob → SysJobLog 映射（手写实现；本仓库未接入 MapStruct 注解处理器，勿用 org.mapstruct.Mapper）。
 *
 * <p>映射语义：仅拷贝 jobName/jobGroup/invokeTarget；日志主键、执行结果
 * （jobMessage/status/exceptionInfo/时间）与审计字段由调用方在任务执行后填写。</p>
 *
 * @author ct
 */
public final class SysJobLogConvert
{
    private SysJobLogConvert()
    {
    }

    public static SysJobLog fromSysJob(SysJob sysJob)
    {
        SysJobLog jobLog = new SysJobLog();
        jobLog.setJobName(sysJob.getJobName());
        jobLog.setJobGroup(sysJob.getJobGroup());
        jobLog.setInvokeTarget(sysJob.getInvokeTarget());
        return jobLog;
    }
}
