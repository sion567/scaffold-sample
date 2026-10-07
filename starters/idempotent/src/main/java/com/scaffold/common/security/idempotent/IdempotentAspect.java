package com.scaffold.common.security.idempotent;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.rpc.RpcContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import com.scaffold.common.core.exception.ServiceException;
import com.scaffold.common.redis.service.RedisService;

/**
 * 幂等切面（P1-6）：Redis SETNX + expire 原子占位；业务异常时删除幂等键允许重试。
 * <p>
 * 幂等键构成：
 * - PATH：类名.方法名
 * - PARAM：类名.方法名:SpEL 计算值
 * - TOKEN：类名.方法名:请求头 X-Idempotent-Token（无 HTTP 上下文的内部调用自动跳过）
 * <p>
 * 生效范围：@DubboService 实现与普通 Spring Bean 的公开方法均可标注
 * （与 InnerAuthAspect 同一机制，Bean 由 AutoConfiguration.imports 注册）。
 *
 * @author ct
 */
@Aspect
public class IdempotentAspect
{
    private static final Logger log = LoggerFactory.getLogger(IdempotentAspect.class);

    private static final String KEY_PREFIX = "scaffold:idempotent:";

    private static final String TOKEN_HEADER = "X-Idempotent-Token";

    private static final SpelExpressionParser PARSER = new SpelExpressionParser();

    private static final ParameterNameDiscoverer NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

    private final RedisService redisService;

    public IdempotentAspect(RedisService redisService)
    {
        this.redisService = redisService;
    }

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint point, Idempotent idempotent) throws Throwable
    {
        String key = buildKey(point, idempotent);
        if (key == null)
        {
            return point.proceed();
        }
        boolean first = redisService.setIfAbsent(KEY_PREFIX + key, "1", idempotent.expireSeconds(), TimeUnit.SECONDS);
        if (!first)
        {
            throw new ServiceException(idempotent.message());
        }
        try
        {
            return point.proceed();
        }
        catch (Throwable e)
        {
            // 业务失败立即删除幂等键，允许调用方重试（重试成功后 key 重新占位）
            redisService.deleteObject(KEY_PREFIX + key);
            throw e;
        }
    }

    /**
     * @return 幂等键；null = 本次调用不启用幂等（TOKEN 无 HTTP 上下文 / SpEL 计算失败降级）
     */
    private String buildKey(ProceedingJoinPoint point, Idempotent idempotent)
    {
        Method method = ((MethodSignature) point.getSignature()).getMethod();
        String base = method.getDeclaringClass().getSimpleName() + "." + method.getName();
        switch (idempotent.mode())
        {
            case PARAM:
                String expr = idempotent.key().trim();
                if (expr.isEmpty())
                {
                    return base;
                }
                Object value = eval(point, method, expr);
                return value == null ? null : base + ":" + value;
            case TOKEN:
                String token = currentToken();
                if (token == null || token.isEmpty())
                {
                    log.debug("[幂等] TOKEN 模式无 HTTP 上下文或请求头，跳过: {}", base);
                    return null;
                }
                return base + ":" + token;
            case PATH:
            default:
                return base;
        }
    }

    private Object eval(ProceedingJoinPoint point, Method method, String expr)
    {
        try
        {
            Expression expression = PARSER.parseExpression(expr);
            MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(
                    null, method, point.getArgs(), NAME_DISCOVERER);
            return expression.getValue(context);
        }
        catch (Exception e)
        {
            // 表达式配置错误时宁可不幂等也不误拦正常请求
            log.warn("[幂等] SpEL 计算失败，本次跳过幂等: key={}, {}", expr, e.getMessage());
            return null;
        }
    }

    private String currentToken()
    {
        HttpRequest request = RpcContext.getServiceContext().getRequest(HttpRequest.class);
        return request == null ? null : request.header(TOKEN_HEADER);
    }
}
