package com.scaffold.common.log.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 全链路业务留痕注解（详细设计 §2.2，G1：业务零侵入留痕）
 *
 * <p>与 {@link Log}（操作日志）互补：Log 记"谁点了什么按钮"，TraceLog 记
 * "业务动作的前后快照 + SM3 摘要链（防篡改）"，关键动作额外 SM2 签名。
 * 切面采集失败只告警，绝不影响业务方法执行。</p>
 *
 * @author ct
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TraceLog
{
    /**
     * 业务场景（必填语义，如：指令下发、指令签收、改派、撤回、情报查询、规则发布）
     */
    String scene();

    /**
     * 业务对象类型（如：instruction、dispatch_order、alert_rule）
     */
    String bizType() default "";

    /**
     * 业务对象 ID 的 SpEL 表达式（对方法入参求值）：支持 #p0/#pN 索引与
     * 参数名（编译保留 -parameters 时）、#result 返回值；解析失败留空不报错
     */
    String bizIdEl() default "";

    /**
     * 是否关键动作（指令下发/签收/改派/撤回/导出，G2：SM2 签名，签名值落留痕表）
     */
    boolean keyAction() default false;

    /**
     * 是否保存入参快照
     */
    boolean saveArgs() default true;

    /**
     * 是否保存返回值快照
     */
    boolean saveResult() default true;
}
