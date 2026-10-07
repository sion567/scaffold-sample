package com.scaffold.common.job;

import com.scaffold.job.api.JobExecLog;
import com.scaffold.job.api.RemoteJobLogApi;
import java.util.Date;
import org.apache.dubbo.config.annotation.DubboReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 执行日志回写器：任务执行结果经 Dubbo 落库任务中心 sys_job_log，控制台统一可见。
 *
 * <p>best-effort 语义：任务中心不可达 / 未引 Dubbo / scaffold.job.report.enabled=false
 * 一律静默降级（本地日志留痕），绝不阻断任务执行本身。
 *
 * @author ct
 */
public class JobLogReporter {
    private static final Logger log = LoggerFactory.getLogger(JobLogReporter.class);

    /** 执行状态：正常 */
    public static final String STATUS_SUCCESS = "0";

    /** 执行状态：失败 */
    public static final String STATUS_FAILURE = "1";

    private final JobProperties properties;

    private final String appName;

    /** 宿主服务自带 Dubbo 消费端；check=false 注册中心不可达不阻断启动 */
    @DubboReference(check = false, retries = 0)
    private RemoteJobLogApi jobLogApi;

    public JobLogReporter(JobProperties properties, String appName) {
        this.properties = properties;
        this.appName = appName;
    }

    /** 供单测注入 mock 实现 */
    void setJobLogApi(RemoteJobLogApi jobLogApi) {
        this.jobLogApi = jobLogApi;
    }

    public void reportSuccess(JobDefinition job, long startMillis) {
        report(
                job,
                STATUS_SUCCESS,
                "执行成功（耗时 " + (System.currentTimeMillis() - startMillis) + "ms）",
                null,
                new Date(startMillis));
    }

    public void reportFailure(JobDefinition job, long startMillis, Throwable ex) {
        String message = "执行失败（耗时 " + (System.currentTimeMillis() - startMillis) + "ms）";
        report(job, STATUS_FAILURE, message, String.valueOf(ex), new Date(startMillis));
    }

    private void report(
            JobDefinition job, String status, String message, String exceptionInfo, Date start) {
        if (!properties.getReport().isEnabled() || jobLogApi == null) {
            return;
        }
        try {
            JobExecLog entry = new JobExecLog();
            entry.setJobName(job.getName());
            entry.setJobGroup(job.getGroup().isEmpty() ? appName : job.getGroup());
            entry.setInvokeTarget(job.invokeTarget());
            entry.setJobMessage(message);
            entry.setStatus(status);
            entry.setExceptionInfo(exceptionInfo);
            entry.setStartTime(start);
            entry.setEndTime(new Date());
            jobLogApi.record(entry);
        } catch (Throwable t) {
            log.warn("[ScaffoldJob] {} 执行日志回写任务中心失败（不阻断任务）: {}", job.getName(), t.getMessage());
        }
    }
}
