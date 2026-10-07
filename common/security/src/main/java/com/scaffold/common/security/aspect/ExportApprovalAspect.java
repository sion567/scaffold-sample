package com.scaffold.common.security.aspect;

import com.scaffold.common.core.exception.ServiceException;
import com.scaffold.common.redis.service.RedisService;
import com.scaffold.common.security.annotation.RequireExportApproval;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

/**
 * 导出审批凭证切面：@RequireExportApproval 标注的导出端点执行前校验 Redis
 * 凭证（scaffold:export:approval:{scope}，审批终级通过时签发、24h 过期）。
 *
 * @author ct
 */
@Aspect
public class ExportApprovalAspect
{
    private static final String CERT_KEY_PREFIX = "scaffold:export:approval:";

    private final RedisService redisService;

    public ExportApprovalAspect(RedisService redisService)
    {
        this.redisService = redisService;
    }

    @Before("@annotation(annotation)")
    public void checkApproval(JoinPoint joinPoint, RequireExportApproval annotation)
    {
        String scope = annotation.value();
        if (redisService.getCacheObject(CERT_KEY_PREFIX + scope) == null)
        {
            throw new ServiceException("导出审批未通过或已过期（scope=" + scope + "），请先提交导出审批申请");
        }
    }
}
