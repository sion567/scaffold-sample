package com.scaffold.common.job;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import com.scaffold.common.redis.lock.DistributedLock;

/**
 * 定时任务 starter 自动装配：scaffold.job.enabled=false 可整体关闭（注解保留但不生效）。
 *
 * <p>引入即生效：宿主的 @Scheduled 方法加 @ScaffoldJob 即获「分布式锁防重 + 执行日志
 * 回写任务中心」；未装配 Redis 锁时自动降级为仅留痕。
 *
 * @author ct
 */
@AutoConfiguration
@ConditionalOnProperty(
        prefix = "scaffold.job",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
@EnableConfigurationProperties(JobProperties.class)
public class JobAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public JobRegistry jobRegistry(ApplicationContext applicationContext, Environment environment) {
        return new JobRegistry(
                applicationContext, environment.getProperty("spring.application.name", "unknown"));
    }

    @Bean
    @ConditionalOnMissingBean
    public JobLogReporter jobLogReporter(JobProperties properties, Environment environment) {
        return new JobLogReporter(
                properties, environment.getProperty("spring.application.name", "unknown"));
    }

    @Bean
    @ConditionalOnMissingBean
    public JobLockAspect jobLockAspect(
            JobRegistry registry,
            JobLogReporter reporter,
            JobProperties properties,
            ObjectProvider<DistributedLock> lockProvider) {
        return new JobLockAspect(registry, reporter, properties, lockProvider);
    }
}
