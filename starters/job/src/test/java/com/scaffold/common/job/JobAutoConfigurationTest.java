package com.scaffold.common.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.scaffold.common.redis.lock.DistributedLock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * 自动装配验证：引入 starter 即生效（三件套 + 任务登记）、scaffold.job.enabled=false 可整体拔除、
 * 任务重名 fail-fast。
 *
 * @author ct
 */
class JobAutoConfigurationTest {
    static class SampleJob {
        @ScaffoldJob("demoJob")
        public void work() {}
    }

    /** 与 SampleJob 撞任务名，注册表必须拒绝装配 */
    static class DuplicateJob {
        @ScaffoldJob("demoJob")
        public void work() {}
    }

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(JobAutoConfiguration.class));

    @Test
    @DisplayName("默认装配：注册表/回写器/防重切面三件套齐全，@ScaffoldJob 任务已登记")
    void registers_all_beans_by_default() {
        runner
                .withBean(SampleJob.class)
                .withBean("distributedLock", DistributedLock.class, () -> mock(DistributedLock.class))
                .run(
                        context -> {
                            assertThat(context).hasSingleBean(JobRegistry.class);
                            assertThat(context).hasSingleBean(JobLogReporter.class);
                            assertThat(context).hasSingleBean(JobLockAspect.class);
                            assertThat(context.getBean(JobRegistry.class).all()).containsKey("demoJob");
                        });
    }

    @Test
    @DisplayName("scaffold.job.enabled=false：注解保留但 starter 完全不装配")
    void disabled_by_property() {
        runner
                .withPropertyValues("scaffold.job.enabled=false")
                .withBean(SampleJob.class)
                .run(
                        context ->
                                assertThat(context)
                                        .doesNotHaveBean(JobRegistry.class)
                                        .doesNotHaveBean(JobLockAspect.class));
    }

    @Test
    @DisplayName("任务名重复：启动 fail-fast（锁键与控制台主键冲突）")
    void duplicate_job_name_fails_fast() {
        runner
                .withBean(SampleJob.class)
                .withBean(DuplicateJob.class)
                .run(
                        context ->
                                assertThat(context.getStartupFailure())
                                        .isInstanceOf(IllegalStateException.class)
                                        .hasMessageContaining("demoJob"));
    }
}
