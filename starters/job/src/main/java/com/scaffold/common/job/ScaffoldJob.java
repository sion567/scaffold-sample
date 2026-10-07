package com.scaffold.common.job;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 定时任务登记注解：标在本地 @Scheduled 任务方法上，即接入任务中心——
 * <ul>
 *   <li>执行时抢占分布式锁，多实例部署仅一节点真正执行（其余跳过）；</li>
 *   <li>执行结果（成功/失败 + 耗时 + 异常）回写任务中心 sys_job_log，控制台统一可见。</li>
 * </ul>
 *
 * <p>调度本身仍由宿主的 @Scheduled 驱动（cron/fixedDelay 语义不变），
 * 本注解只做「防重 + 留痕」，不加即可拔。</p>
 *
 * @author ct
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ScaffoldJob
{
    /**
     * 任务名（回写日志与分布式锁键均用它，同一应用内必须唯一）
     */
    String value();

    /**
     * 任务组名（缺省取 spring.application.name，便于控制台按服务过滤）
     */
    String group() default "";

    /**
     * 任务说明（控制台展示）
     */
    String description() default "";
}
