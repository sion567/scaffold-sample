package com.scaffold.job.api;

import java.io.Serializable;
import java.util.Date;

/**
 * 任务执行日志条目（业务服务 → scaffold-job 回写 sys_job_log 的载体）
 *
 * <p>由任务宿主服务在任务执行前后组装，{@link RemoteJobLogApi#record(JobExecLog)}
 * 落库到任务中心 sys_job_log，控制台即可看到全系统任务的执行历史。</p>
 *
 * @author ct
 */
public class JobExecLog implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 任务名称 */
    private String jobName;

    /** 任务组名（发起方服务名 spring.application.name） */
    private String jobGroup;

    /** 调用目标（发起方 类名.方法名，便于定位代码） */
    private String invokeTarget;

    /** 日志信息（执行结果摘要，含耗时） */
    private String jobMessage;

    /** 执行状态（0正常 1失败） */
    private String status;

    /** 异常信息（失败时） */
    private String exceptionInfo;

    /** 开始时间 */
    private Date startTime;

    /** 结束时间 */
    private Date endTime;

    public String getJobName() { return jobName; }
    public void setJobName(String jobName) { this.jobName = jobName; }

    public String getJobGroup() { return jobGroup; }
    public void setJobGroup(String jobGroup) { this.jobGroup = jobGroup; }

    public String getInvokeTarget() { return invokeTarget; }
    public void setInvokeTarget(String invokeTarget) { this.invokeTarget = invokeTarget; }

    public String getJobMessage() { return jobMessage; }
    public void setJobMessage(String jobMessage) { this.jobMessage = jobMessage; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getExceptionInfo() { return exceptionInfo; }
    public void setExceptionInfo(String exceptionInfo) { this.exceptionInfo = exceptionInfo; }

    public Date getStartTime() { return startTime; }
    public void setStartTime(Date startTime) { this.startTime = startTime; }

    public Date getEndTime() { return endTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }

    @Override
    public String toString()
    {
        return "JobExecLog{jobName='" + jobName + "', jobGroup='" + jobGroup
                + "', invokeTarget='" + invokeTarget + "', status='" + status
                + "', message='" + jobMessage + "'}";
    }
}
