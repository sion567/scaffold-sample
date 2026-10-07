package com.scaffold.common.log.aspect;

import java.util.Collection;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.ArrayUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NamedThreadLocal;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;
import com.alibaba.fastjson2.JSON;
import com.scaffold.common.core.text.Convert;
import com.scaffold.common.core.utils.ExceptionUtil;
import com.scaffold.common.core.utils.ServletUtils;
import com.scaffold.common.core.utils.StringUtils;
import com.scaffold.common.core.utils.ip.IpUtils;
import com.scaffold.common.log.annotation.Log;
import com.scaffold.common.log.enums.BusinessStatus;
import com.scaffold.common.log.filter.PropertyPreExcludeFilter;
import com.scaffold.common.log.service.AsyncLogService;
import com.scaffold.common.security.utils.SecurityUtils;
import com.scaffold.system.api.domain.SysOperLog;

/**
 * 操作日志记录处理
 * 
 * @author ct
 */
@Aspect
@Component
public class LogAspect
{
    private static final Logger log = LoggerFactory.getLogger(LogAspect.class);

    /** 排除敏感属性字段 */
    public static final String[] EXCLUDE_PROPERTIES = { "password", "oldPassword", "newPassword", "confirmPassword" };

    /** 计算操作消耗时间 */
    private static final ThreadLocal<Long> TIME_THREADLOCAL = new NamedThreadLocal<Long>("Cost Time");

    /** 参数最大长度限制 */
    private static final int PARAM_MAX_LENGTH = 2000;

    private final AsyncLogService asyncLogService;

    public LogAspect(AsyncLogService asyncLogService)
    {
        this.asyncLogService = asyncLogService;
    }

    /**
     * 处理请求前执行
     */
    @Before(value = "@annotation(controllerLog)")
    public void doBefore(JoinPoint joinPoint, Log controllerLog)
    {
        TIME_THREADLOCAL.set(System.currentTimeMillis());
    }

    /**
     * 处理完请求后执行
     *
     * @param joinPoint 切点
     */
    @AfterReturning(pointcut = "@annotation(controllerLog)", returning = "jsonResult")
    public void doAfterReturning(JoinPoint joinPoint, Log controllerLog, Object jsonResult)
    {
        handleLog(joinPoint, controllerLog, null, jsonResult);
    }

    /**
     * 拦截异常操作
     * 
     * @param joinPoint 切点
     * @param e 异常
     */
    @AfterThrowing(value = "@annotation(controllerLog)", throwing = "e")
    public void doAfterThrowing(JoinPoint joinPoint, Log controllerLog, Exception e)
    {
        handleLog(joinPoint, controllerLog, e, null);
    }

    protected void handleLog(final JoinPoint joinPoint, Log controllerLog, final Exception e, Object jsonResult)
    {
        try
        {
            // *========数据库日志=========*//
            SysOperLog operLog = new SysOperLog();
            operLog.setStatus(BusinessStatus.SUCCESS.ordinal());
            // 请求的地址
            String ip = IpUtils.getIpAddr();
            operLog.setOperIp(ip);
            operLog.setRequestIp(ip);
            operLog.setOperUrl(StringUtils.substring(Convert.toStr(ServletUtils.getRequest() != null
                    ? ServletUtils.getRequest().getRequestURI() : null, ""), 0, 255));
            // 等保 8.1.4 b：审计要素补充（用户/会话/事件/结果码/业务类型）
            Long userId = SecurityUtils.getUserId();
            operLog.setUserId(userId != null ? userId : 0L);
            operLog.setSessionId(SecurityUtils.getSessionId());
            operLog.setEventType(determineEventType(joinPoint));
            operLog.setBizType(determineBizType(joinPoint));
            operLog.setResultCode(e != null ? 500 : 200);
            String username = SecurityUtils.getUsername();
            if (StringUtils.isNotBlank(username))
            {
                operLog.setOperName(username);
            }

            if (e != null)
            {
                operLog.setStatus(BusinessStatus.FAIL.ordinal());
                operLog.setErrorMsg(StringUtils.substring(Convert.toStr(e.getMessage(), ExceptionUtil.getExceptionMessage(e)), 0, 2000));
            }
            // 设置方法名称
            String className = joinPoint.getTarget().getClass().getName();
            String methodName = joinPoint.getSignature().getName();
            operLog.setMethod(className + "." + methodName + "()");
            // 设置请求方式
            operLog.setRequestMethod(ServletUtils.getRequest() != null ? ServletUtils.getRequest().getMethod() : "");
            // 处理设置注解上的参数
            getControllerMethodDescription(joinPoint, controllerLog, operLog, jsonResult);
            // 设置消耗时间
            operLog.setCostTime(System.currentTimeMillis() - TIME_THREADLOCAL.get());
            // 等保 8.1.4 c：重要安全事件记录操作参数快照（请求参数近似“前值”，供审计比对）
            if (isSecurityEvent(operLog.getEventType()) && StringUtils.isNotEmpty(operLog.getOperParam()))
            {
                operLog.setBeforeValue(StringUtils.substring(operLog.getOperParam(), 0, PARAM_MAX_LENGTH));
            }
            // 保存数据库
            asyncLogService.saveSysLog(operLog);
        }
        catch (Exception exp)
        {
            // 记录本地异常日志
            log.error("异常信息:{}", exp.getMessage());
            exp.printStackTrace();
        }
        finally
        {
            TIME_THREADLOCAL.remove();
        }
    }

    /**
     * 获取注解中对方法的描述信息 用于Controller层注解
     * 
     * @param log 日志
     * @param operLog 操作日志
     * @throws Exception
     */
    public void getControllerMethodDescription(JoinPoint joinPoint, Log log, SysOperLog operLog, Object jsonResult) throws Exception
    {
        // 设置action动作
        operLog.setBusinessType(log.businessType().ordinal());
        // 设置标题
        operLog.setTitle(log.title());
        // 设置操作人类别
        operLog.setOperatorType(log.operatorType().ordinal());
        // 是否需要保存request，参数和值
        if (log.isSaveRequestData())
        {
            // 获取参数的信息，传入到数据库中。
            setRequestValue(joinPoint, operLog, log.excludeParamNames());
        }
        // 是否需要保存response，参数和值
        if (log.isSaveResponseData() && StringUtils.isNotNull(jsonResult))
        {
            operLog.setJsonResult(StringUtils.substring(JSON.toJSONString(jsonResult), 0, 2000));
        }
    }

    /**
     * 获取请求的参数，放到log中
     * 
     * @param operLog 操作日志
     * @throws Exception 异常
     */
    private void setRequestValue(JoinPoint joinPoint, SysOperLog operLog, String[] excludeParamNames) throws Exception
    {
        String requestMethod = operLog.getRequestMethod();
        Map<?, ?> paramsMap = ServletUtils.getRequest() != null ? ServletUtils.getParamMap(ServletUtils.getRequest()) : java.util.Collections.emptyMap();
        if (StringUtils.isEmpty(paramsMap) && StringUtils.equalsAny(requestMethod, HttpMethod.PUT.name(), HttpMethod.POST.name(), HttpMethod.DELETE.name()))
        {
            String params = argsArrayToString(joinPoint.getArgs(), excludeParamNames);
            operLog.setOperParam(params);
        }
        else
        {
            operLog.setOperParam(StringUtils.substring(JSON.toJSONString(paramsMap, excludePropertyPreFilter(excludeParamNames)), 0, PARAM_MAX_LENGTH));
        }
    }

    /**
     * 参数拼装
     */
    private String argsArrayToString(Object[] paramsArray, String[] excludeParamNames)
    {
        StringBuilder params = new StringBuilder();
        if (paramsArray != null && paramsArray.length > 0)
        {
            for (Object o : paramsArray)
            {
                if (StringUtils.isNotNull(o) && !isFilterObject(o))
                {
                    try
                    {
                        String jsonObj = JSON.toJSONString(o, excludePropertyPreFilter(excludeParamNames));
                        params.append(jsonObj).append(" ");
                        if (params.length() >= PARAM_MAX_LENGTH)
                        {
                            return StringUtils.substring(params.toString(), 0, PARAM_MAX_LENGTH);
                        }
                    }
                    catch (Exception e)
                    {
                        log.error("请求参数拼装异常 msg:{}, 参数:{}", e.getMessage(), paramsArray, e);
                    }
                }
            }
        }
        return params.toString();
    }

    /**
     * 忽略敏感属性
     */
    public PropertyPreExcludeFilter excludePropertyPreFilter(String[] excludeParamNames)
    {
        return new PropertyPreExcludeFilter().addExcludes(ArrayUtils.addAll(EXCLUDE_PROPERTIES, excludeParamNames));
    }

    /**
     * 判断是否需要过滤的对象。
     * 
     * @param o 对象信息。
     * @return 如果是需要过滤的对象，则返回true；否则返回false。
     */
    @SuppressWarnings("rawtypes")
    public boolean isFilterObject(final Object o)
    {
        Class<?> clazz = o.getClass();
        if (clazz.isArray())
        {
            return clazz.getComponentType().isAssignableFrom(MultipartFile.class);
        }
        else if (Collection.class.isAssignableFrom(clazz))
        {
            Collection collection = (Collection) o;
            for (Object value : collection)
            {
                return value instanceof MultipartFile;
            }
        }
        else if (Map.class.isAssignableFrom(clazz))
        {
            Map map = (Map) o;
            for (Object value : map.entrySet())
            {
                Map.Entry entry = (Map.Entry) value;
                return entry.getValue() instanceof MultipartFile;
            }
        }
        return o instanceof MultipartFile || o instanceof HttpServletRequest || o instanceof HttpServletResponse
                || o instanceof BindingResult;
    }

    /**
     * 推断事件类型（等保 8.1.4 b/c）
     * 依据方法名关键词识别重要用户行为与安全事件
     */
    private String determineEventType(JoinPoint joinPoint)
    {
        String methodName = joinPoint.getSignature().getName().toLowerCase();
        if (methodName.contains("login"))
        {
            return "LOGIN";
        }
        if (methodName.contains("delete") || methodName.contains("remove") || methodName.contains("clean")
                || methodName.contains("clear"))
        {
            return "DELETE";
        }
        if (methodName.contains("reset") || methodName.contains("password") || methodName.contains("pwd"))
        {
            return "PWD_CHANGE";
        }
        if (methodName.contains("assign") || methodName.contains("auth") || methodName.contains("grant"))
        {
            return "ROLE_ASSIGN";
        }
        if (methodName.contains("export") || methodName.contains("download"))
        {
            return "EXPORT";
        }
        if (methodName.contains("config") || methodName.contains("dict"))
        {
            return "CONFIG_CHANGE";
        }
        if (methodName.contains("update") || methodName.contains("edit"))
        {
            return "UPDATE";
        }
        if (methodName.contains("add") || methodName.contains("insert") || methodName.contains("create")
                || methodName.contains("save"))
        {
            return "INSERT";
        }
        return "OPERATION";
    }

    /**
     * 推断业务类型（ORDER/USER/PAY/CONFIG/AUDIT 等）
     * 依据目标 Controller 类名
     */
    private String determineBizType(JoinPoint joinPoint)
    {
        String className = joinPoint.getTarget().getClass().getName().toLowerCase();
        if (className.contains("user"))
        {
            return "USER";
        }
        if (className.contains("role"))
        {
            return "ROLE";
        }
        if (className.contains("menu"))
        {
            return "MENU";
        }
        if (className.contains("config"))
        {
            return "CONFIG";
        }
        if (className.contains("dict"))
        {
            return "DICT";
        }
        if (className.contains("job"))
        {
            return "JOB";
        }
        if (className.contains("operlog") || className.contains("logininfor"))
        {
            return "AUDIT";
        }
        if (className.contains("order"))
        {
            return "ORDER";
        }
        if (className.contains("pay"))
        {
            return "PAY";
        }
        if (className.contains("member"))
        {
            return "MEMBER";
        }
        return "GENERAL";
    }

    /**
     * 是否重要安全事件（等保 8.1.4 c）
     */
    private boolean isSecurityEvent(String eventType)
    {
        if (StringUtils.isEmpty(eventType))
        {
            return false;
        }
        return StringUtils.equalsAny(eventType, "DELETE", "PWD_CHANGE", "ROLE_ASSIGN", "CONFIG_CHANGE", "EXPORT");
    }
}
