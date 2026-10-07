package com.scaffold.common.log.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.scaffold.common.log.persist.LogPersister;
import com.scaffold.system.api.domain.SysOperLog;

/**
 * 异步保存操作日志：采集与投递解耦，经 {@link LogPersister} SPI 落库
 * （默认 SLF4J 输出，接入方注册自己的 LogPersister Bean 即可替换为远程审计服务）。
 *
 * <p>@Async 不占业务线程；落库异常由异步未捕获处理器记日志，
 * 业务事务与日志投递解耦。</p>
 *
 * @author scaffold
 */
@Service
public class AsyncLogService
{
    private final LogPersister logPersister;

    public AsyncLogService(LogPersister logPersister)
    {
        this.logPersister = logPersister;
    }

    /**
     * 保存系统日志记录
     */
    @Async
    public void saveSysLog(SysOperLog sysOperLog)
    {
        logPersister.saveLog(sysOperLog);
    }
}
