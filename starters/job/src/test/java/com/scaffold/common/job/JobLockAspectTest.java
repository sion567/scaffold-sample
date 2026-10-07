package com.scaffold.common.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.scaffold.common.redis.lock.DistributedLock;
import com.scaffold.common.redis.lock.LockHandle;
import com.scaffold.job.api.JobExecLog;
import com.scaffold.job.api.RemoteJobLogApi;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;

/**
 * 防重切面单测：抢到锁才执行、抢不到跳过、异常照常上抛并回写失败日志、
 * 无 Redis 锁环境降级为仅留痕。
 *
 * @author ct
 */
class JobLockAspectTest {
    /** 被扫描样例：@ScaffoldJob 标注在本地任务方法上 */
    static class SampleJob {
        @ScaffoldJob(value = "demoJob", group = "demo-group", description = "单测任务")
        public void work() {}
    }

    private final ApplicationContext applicationContext = mock(ApplicationContext.class);

    private final DistributedLock lock = mock(DistributedLock.class);

    private final LockHandle handle = mock(LockHandle.class);

    @SuppressWarnings("unchecked")
    private final ObjectProvider<DistributedLock> lockProvider = mock(ObjectProvider.class);

    private final RemoteJobLogApi jobLogApi = mock(RemoteJobLogApi.class);

    private final ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);

    private JobRegistry registry;

    private JobLockAspect aspect;

    @BeforeEach
    void setUp() throws Exception {
        when(applicationContext.getBeanDefinitionNames()).thenReturn(new String[] {"sampleJob"});
        when(applicationContext.getType("sampleJob")).thenAnswer(inv -> SampleJob.class);
        registry = new JobRegistry(applicationContext, "test-app");
        registry.afterSingletonsInstantiated();

        JobProperties properties = new JobProperties();
        JobLogReporter reporter = new JobLogReporter(properties, "test-app");
        reporter.setJobLogApi(jobLogApi);
        aspect = new JobLockAspect(registry, reporter, properties, lockProvider);
    }

    private ScaffoldJob annotation() throws Exception {
        return SampleJob.class.getDeclaredMethod("work").getAnnotation(ScaffoldJob.class);
    }

    @Test
    @DisplayName("抢到锁：执行任务并回写成功日志，锁照常释放")
    void executes_and_reports_success_when_lock_acquired() throws Throwable {
        when(lockProvider.getIfAvailable()).thenReturn(lock);
        when(lock.acquire(anyString(), anyLong(), anyLong())).thenReturn(handle);

        aspect.around(pjp, annotation());

        verify(pjp).proceed();
        verify(handle).close();
        ArgumentCaptor<JobExecLog> captor = ArgumentCaptor.forClass(JobExecLog.class);
        verify(jobLogApi).record(captor.capture());
        JobExecLog entry = captor.getValue();
        assertThat(entry.getStatus()).isEqualTo(JobLogReporter.STATUS_SUCCESS);
        assertThat(entry.getJobName()).isEqualTo("demoJob");
        assertThat(entry.getJobGroup()).isEqualTo("demo-group");
        assertThat(entry.getInvokeTarget()).contains("SampleJob.work");
    }

    @Test
    @DisplayName("抢不到锁：另一实例持锁执行中，本次跳过且不回写日志")
    void skips_when_lock_not_acquired() throws Throwable {
        when(lockProvider.getIfAvailable()).thenReturn(lock);
        when(lock.acquire(anyString(), anyLong(), anyLong())).thenReturn(null);

        Object result = aspect.around(pjp, annotation());

        assertThat(result).isNull();
        verify(pjp, never()).proceed();
        verifyNoInteractions(jobLogApi);
    }

    @Test
    @DisplayName("执行异常：回写失败日志、锁照常释放、异常原样上抛")
    void reports_failure_and_rethrows_when_job_fails() throws Throwable {
        when(lockProvider.getIfAvailable()).thenReturn(lock);
        when(lock.acquire(anyString(), anyLong(), anyLong())).thenReturn(handle);
        when(pjp.proceed()).thenThrow(new IllegalStateException("boom"));

        assertThatThrownBy(() -> aspect.around(pjp, annotation()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("boom");

        verify(handle).close();
        ArgumentCaptor<JobExecLog> captor = ArgumentCaptor.forClass(JobExecLog.class);
        verify(jobLogApi).record(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(JobLogReporter.STATUS_FAILURE);
        assertThat(captor.getValue().getExceptionInfo()).contains("boom");
    }

    @Test
    @DisplayName("无 Redis 锁环境：降级为仅留痕，任务照常执行")
    void executes_without_lock_when_redis_absent() throws Throwable {
        when(lockProvider.getIfAvailable()).thenReturn(null);

        aspect.around(pjp, annotation());

        verify(pjp).proceed();
        verify(jobLogApi).record(ArgumentCaptor.forClass(JobExecLog.class).capture());
    }
}
