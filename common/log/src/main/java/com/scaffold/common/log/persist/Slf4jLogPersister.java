package com.scaffold.common.log.persist;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.scaffold.common.log.domain.TraceLogEntry;
import com.scaffold.system.api.domain.SysLogininfor;
import com.scaffold.system.api.domain.SysOperLog;

/**
 * {@link LogPersister} 默认实现：仅输出 INFO 日志，不做远程调用与落库。
 *
 * <p>由 {@link LogPersisterAutoConfiguration} 经 {@code @ConditionalOnMissingBean}
 * 注册——接入方提供自己的 {@code LogPersister} Bean 即自动覆盖本实现。</p>
 *
 * @author scaffold
 */
public class Slf4jLogPersister implements LogPersister
{
    private static final Logger log = LoggerFactory.getLogger(Slf4jLogPersister.class);

    @Override
    public void saveLog(SysOperLog operLog)
    {
        log.info("[操作日志] title={}, operName={}, method={}, status={}, costTime={}ms, operIp={}, url={}, error={}",
                operLog.getTitle(), operLog.getOperName(), operLog.getMethod(), operLog.getStatus(),
                operLog.getCostTime(), operLog.getOperIp(), operLog.getOperUrl(), operLog.getErrorMsg());
    }

    @Override
    public void saveLogininfor(SysLogininfor logininfor)
    {
        log.info("[登录日志] userName={}, status={}, ipaddr={}, msg={}",
                logininfor.getUserName(), logininfor.getStatus(), logininfor.getIpaddr(), logininfor.getMsg());
    }

    @Override
    public void saveTrace(TraceLogEntry entry)
    {
        log.info("[业务留痕] scene={}, bizType={}, bizId={}, operator={}, method={}, costMs={}, error={}",
                entry.getScene(), entry.getBizType(), entry.getBizId(), entry.getOperator(),
                entry.getMethodName(), entry.getCostMs(), entry.getErrorMsg());
    }
}
