package com.scaffold.common.security.idempotent;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口幂等（P1-6）：防止双击提交/超时重试导致的重复处理，Redis SETNX + expire 实现。
 * <p>
 * 用法示例：
 * <pre>
 * // 参数级：同一手机号 60s 内只允许提交一次
 * &#64;Idempotent(mode = Idempotent.Mode.PARAM, key = "#req.username", expireSeconds = 60)
 * public R&lt;Void&gt; register(RegisterRequest req) { ... }
 *
 * // 令牌级：前端先取一次性 token 放请求头 X-Idempotent-Token（仅 Triple REST 入口生效）
 * &#64;Idempotent(mode = Idempotent.Mode.TOKEN, expireSeconds = 120)
 * public R&lt;Order&gt; submit(...) { ... }
 * </pre>
 *
 * @author ct
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent
{
    enum Mode
    {
        /** 请求头 X-Idempotent-Token 作为幂等键（仅 Triple REST 入口生效，纯 dubbo 内部调用自动跳过） */
        TOKEN,

        /** SpEL 指定业务参数作为幂等键（引用方法参数，如 "#req.username"），工程已开 -parameters */
        PARAM,

        /** 接口本身（类名.方法名）作为幂等键 */
        PATH
    }

    Mode mode() default Mode.PATH;

    /** PARAM 模式的 SpEL 表达式 */
    String key() default "";

    /** 幂等窗口（秒）：成功后 key 保留至窗口结束；业务异常时立即删除允许重试 */
    long expireSeconds() default 300;

    /** 重复请求的提示文案 */
    String message() default "请求已提交，请勿重复操作";
}
