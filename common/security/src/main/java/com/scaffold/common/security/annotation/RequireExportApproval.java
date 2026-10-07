package com.scaffold.common.security.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据导出审批校验：标注在导出端点上，执行前校验 Redis 中是否存在
 * 对应 scope 的有效审批凭证（由数据导出审批流终级通过时签发，24h 有效）。
 *
 * <p>无凭证抛 ServiceException（未获批准语义），凭证由审批流终级通过时按
 * {@code scaffold:export:approval:{scope}} 写入。仅约束 value 指定的导出范围键，
 * 与审批单 scope_key 对应。
 *
 * @author ct
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireExportApproval
{
    /**
     * 导出范围键（与审批单 scope_key 一致，如 audit-oper-log）
     */
    String value();
}
