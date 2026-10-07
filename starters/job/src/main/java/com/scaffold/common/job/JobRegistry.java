package com.scaffold.common.job;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.ClassUtils;

/**
 * @ScaffoldJob 任务注册表：单例就绪后扫描全部 bean 的 @ScaffoldJob 方法，
 * 任务重名直接 fail-fast（锁键与控制台都以任务名为主键，重名必乱）。
 *
 * @author ct
 */
public class JobRegistry implements SmartInitializingSingleton {
    private static final Logger log = LoggerFactory.getLogger(JobRegistry.class);

    private final ApplicationContext applicationContext;

    private final String defaultGroup;

    private final Map<String, JobDefinition> jobs = new LinkedHashMap<>();

    public JobRegistry(ApplicationContext applicationContext, String defaultGroup) {
        this.applicationContext = applicationContext;
        this.defaultGroup = defaultGroup;
    }

    @Override
    public void afterSingletonsInstantiated() {
        for (String beanName : applicationContext.getBeanDefinitionNames()) {
            Class<?> beanType;
            try {
                beanType = ClassUtils.getUserClass(applicationContext.getType(beanName));
            } catch (Exception e) {
                continue;
            }
            for (java.lang.reflect.Method method : beanType.getDeclaredMethods()) {
                ScaffoldJob scaffoldJob = AnnotatedElementUtils.findMergedAnnotation(method, ScaffoldJob.class);
                if (scaffoldJob == null) {
                    continue;
                }
                JobDefinition existed = jobs.get(scaffoldJob.value());
                if (existed != null) {
                    throw new IllegalStateException(
                            "@ScaffoldJob 任务名重复: \""
                                    + scaffoldJob.value()
                                    + "\" 已由 "
                                    + existed.invokeTarget()
                                    + " 注册，"
                                    + beanName
                                    + "."
                                    + method.getName()
                                    + " 请换一个任务名");
                }
                jobs.put(
                        scaffoldJob.value(),
                        new JobDefinition(
                                scaffoldJob.value(),
                                scaffoldJob.group().isEmpty() ? defaultGroup : scaffoldJob.group(),
                                scaffoldJob.description(),
                                beanName,
                                beanType.getName(),
                                method.getName()));
            }
        }
        if (jobs.isEmpty()) {
            log.info("[ScaffoldJob] 未发现 @ScaffoldJob 任务（starter 已装配，无任务可管）");
        } else {
            log.info("[ScaffoldJob] 已登记 {} 个定时任务: {}", jobs.size(), jobs.keySet());
        }
    }

    /** 取任务定义（切面回写日志用；未登记返回 null，不阻断执行） */
    public JobDefinition get(String jobName) {
        return jobs.get(jobName);
    }

    public Map<String, JobDefinition> all() {
        return Collections.unmodifiableMap(jobs);
    }
}
