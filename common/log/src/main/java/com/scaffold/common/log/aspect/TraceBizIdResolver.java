package com.scaffold.common.log.aspect;

import java.lang.reflect.Method;

import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import com.scaffold.common.core.utils.StringUtils;

/**
 * @TraceLog bizIdEl 表达式解析：对方法入参（#p0/#pN、-parameters 编译时可用参数名）
 * 与 #result 求值。任何解析失败一律返回 null——bizId 缺失只影响检索，不影响留痕写入。
 *
 * @author ct
 */
final class TraceBizIdResolver
{
    private static final ExpressionParser PARSER = new SpelExpressionParser();

    private static final ParameterNameDiscoverer NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

    private TraceBizIdResolver()
    {
    }

    static String resolve(String el, Method method, Object[] args, Object result)
    {
        if (StringUtils.isBlank(el))
        {
            return null;
        }
        try
        {
            MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(
                    null, method, args == null ? new Object[0] : args, NAME_DISCOVERER);
            Object[] safeArgs = args == null ? new Object[0] : args;
            for (int i = 0; i < safeArgs.length; i++)
            {
                context.setVariable("p" + i, safeArgs[i]);
            }
            context.setVariable("result", result);
            Object value = PARSER.parseExpression(el).getValue(context);
            return value == null ? null : String.valueOf(value);
        }
        catch (Exception e)
        {
            return null;
        }
    }
}
