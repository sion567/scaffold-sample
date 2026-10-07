package com.scaffold.job.api;

/**
 * 任务执行日志回写 Dubbo 契约：业务服务把本地定时任务的
 * 执行结果落库到任务中心 sys_job_log，控制台统一可见。
 *
 * <p>实现方：scaffold-job。调用方为系统侧自动触发（无登录态），best-effort 语义——
 * 实现方不得抛出异常阻断调度（落库失败只记日志，下个周期覆盖）。</p>
 *
 * @author ct
 */
public interface RemoteJobLogApi
{
    /**
     * 写入一条任务执行日志（sys_job_log）
     *
     * @param jobLog 执行日志条目（jobName/jobGroup/status/起止时间必填）
     */
    void record(JobExecLog jobLog);
}
