package com.scaffold.common.job;

import com.scaffold.common.redis.lock.DistributedLock;
import com.scaffold.common.redis.lock.LockHandle;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;

/**
 * 任务防重切面：@ScaffoldJob 方法执行前抢分布式锁（键 = scaffold:job:lock:{任务名}），
 * 抢不到说明另一实例正在执行，本次直接跳过；抢到则执行并回写执行日志。
 *
 * <p>锁租期由 {@link JobProperties.Lock#getLeaseMillis()} 控制，执行期间自动续约，
 * 实例宕机后最迟一个租期释放、他节点下个周期接管。宿主无 Redis（未装配 DistributedLock）
 * 时降级为仅留痕不防重——启动时告警一次。
 *
 * @author ct
 */
@Aspect
public class JobLockAspect {
    private static final Logger log = LoggerFactory.getLogger(JobLockAspect.class);

    private final JobRegistry registry;

    private final JobLogReporter reporter;

    private final JobProperties properties;

    private final ObjectProvider<DistributedLock> lockProvider;

    private volatile boolean lockMissingWarned;

    public JobLockAspect(
            JobRegistry registry,
            JobLogReporter reporter,
            JobProperties properties,
            ObjectProvider<DistributedLock> lockProvider) {
        this.registry = registry;
        this.reporter = reporter;
        this.properties = properties;
        this.lockProvider = lockProvider;
    }

    @Around("@annotation(scaffoldJob)")
    public Object around(ProceedingJoinPoint pjp, ScaffoldJob scaffoldJob) throws Throwable {
        JobDefinition job = registry.get(scaffoldJob.value());
        DistributedLock lock = lockProvider.getIfAvailable();
        if (lock == null || !properties.getLock().isEnabled()) {
            warnLockMissingOnce();
            return executeAndReport(pjp, job);
        }
        String key = properties.getLock().getKeyPrefix() + scaffoldJob.value();
        // waitMillis=0 表示只尝试一次（另一实例持锁即放弃本次，不等）
        long waitMillis = Math.max(properties.getLock().getWaitMillis(), 1);
        LockHandle handle = lock.acquire(key, waitMillis, properties.getLock().getLeaseMillis());
        if (handle == null) {
            log.info("[ScaffoldJob] {} 另一实例持锁执行中，本次跳过", scaffoldJob.value());
            return null;
        }
        try (LockHandle h = handle) {
            return executeAndReport(pjp, job);
        }
    }

    private Object executeAndReport(ProceedingJoinPoint pjp, JobDefinition job) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            Object result = pjp.proceed();
            if (job != null) {
                reporter.reportSuccess(job, start);
            }
            return result;
        } catch (Throwable t) {
            if (job != null) {
                reporter.reportFailure(job, start, t);
            }
            throw t;
        }
    }

    private void warnLockMissingOnce() {
        if (!lockMissingWarned) {
            lockMissingWarned = true;
            log.warn(
                    "[ScaffoldJob] 未装配 DistributedLock（或 scaffold.job.lock.enabled=false），"
                            + "@ScaffoldJob 任务仅留痕不防重，多实例部署会重复执行");
        }
    }
}
