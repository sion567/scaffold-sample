package com.scaffold.common.log.aspect;

import java.util.Date;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;

import com.alibaba.fastjson2.JSON;
import com.scaffold.common.core.utils.ServletUtils;
import com.scaffold.common.core.utils.StringUtils;
import com.scaffold.common.core.utils.ip.IpUtils;
import com.scaffold.common.log.annotation.TraceLog;
import com.scaffold.common.log.domain.TraceLogEntry;
import com.scaffold.common.log.service.AsyncTraceLogService;
import com.scaffold.common.security.utils.SecurityUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 业务留痕切面：拦截 @TraceLog 方法，采集操作人/IP/终端/前后快照，
 * 异步交 {@code LogPersister} 落库（默认 SLF4J 留痕，接入方可替换为远程审计实现）。
 *
 * <p>铁律：留痕是旁路——采集/投递任何异常只记日志，业务方法结果不受影响
 * （与 LogAspect 的操作日志同级，走独立留痕链路）。</p>
 *
 * @author scaffold
 */
@Aspect
@Component
public class TraceLogAspect
{
    private static final Logger log = LoggerFactory.getLogger(TraceLogAspect.class);

    /** 快照截断长度（超长 JSON 截断，防留痕表被大报文撑爆） */
    private static final int SNAPSHOT_MAX_LENGTH = 2000;

    private final AsyncTraceLogService asyncTraceLogService;

    public TraceLogAspect(AsyncTraceLogService asyncTraceLogService)
    {
        this.asyncTraceLogService = asyncTraceLogService;
    }

    @Around(value = "@annotation(traceLog)")
    public Object around(ProceedingJoinPoint joinPoint, TraceLog traceLog) throws Throwable
    {
        long start = System.currentTimeMillis();
        try
        {
            Object result = joinPoint.proceed();
            try
            {
                record(joinPoint, traceLog, start, null, result);
            }
            catch (Exception ex)
            {
                log.error("[业务留痕] 采集失败（不影响业务）: {}", joinPoint.getSignature().toShortString(), ex);
            }
            return result;
        }
        catch (Throwable e)
        {
            try
            {
                record(joinPoint, traceLog, start, e, null);
            }
            catch (Exception ex)
            {
                log.error("[业务留痕] 采集失败（不影响业务）: {}", joinPoint.getSignature().toShortString(), ex);
            }
            throw e;
        }
    }

    private void record(ProceedingJoinPoint joinPoint, TraceLog traceLog, long start, Throwable error, Object result)
    {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Object[] args = joinPoint.getArgs();

        TraceLogEntry entry = new TraceLogEntry();
        entry.setScene(traceLog.scene());
        entry.setBizType(traceLog.bizType());
        entry.setBizId(TraceBizIdResolver.resolve(traceLog.bizIdEl(), signature.getMethod(), args, result));
        entry.setOperator(SecurityUtils.getUsername());
        entry.setOperateIp(IpUtils.getIpAddr());
        entry.setTerminal(resolveTerminal());
        entry.setMethodName(signature.getDeclaringTypeName() + "#" + signature.getName());
        if (traceLog.saveArgs())
        {
            entry.setSnapshotJson(truncate(toJson(filterArgs(args))));
        }
        if (traceLog.saveResult() && result != null)
        {
            entry.setResultJson(truncate(toJson(result)));
        }
        entry.setErrorMsg(error != null ? StringUtils.substring(error.getMessage(), 0, 1000) : null);
        entry.setCostMs(System.currentTimeMillis() - start);
        entry.setKeyAction(traceLog.keyAction());
        entry.setOperateTime(new Date(start));
        asyncTraceLogService.saveTrace(entry);
    }

    private static String resolveTerminal()
    {
        HttpServletRequest request = ServletUtils.getRequest();
        if (request != null)
        {
            String terminal = request.getHeader("X-Terminal");
            if (StringUtils.isNotBlank(terminal))
            {
                return terminal;
            }
        }
        return "WEB";
    }

    /** 过滤不可序列化参数（流/表单绑定/servlet 容器对象） */
    private static Object[] filterArgs(Object[] args)
    {
        if (args == null)
        {
            return new Object[0];
        }
        Object[] filtered = new Object[args.length];
        for (int i = 0; i < args.length; i++)
        {
            Object arg = args[i];
            if (arg instanceof HttpServletRequest || arg instanceof HttpServletResponse
                    || arg instanceof MultipartFile || arg instanceof BindingResult)
            {
                filtered[i] = "[" + arg.getClass().getSimpleName() + "]";
            }
            else
            {
                filtered[i] = arg;
            }
        }
        return filtered;
    }

    private static String toJson(Object value)
    {
        if (value == null)
        {
            return null;
        }
        try
        {
            return JSON.toJSONString(value);
        }
        catch (Exception e)
        {
            return String.valueOf(value);
        }
    }

    private static String truncate(String value)
    {
        return StringUtils.substring(value, 0, SNAPSHOT_MAX_LENGTH);
    }
}
