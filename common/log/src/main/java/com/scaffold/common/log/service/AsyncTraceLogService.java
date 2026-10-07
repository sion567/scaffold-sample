package com.scaffold.common.log.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.scaffold.common.log.domain.TraceLogEntry;
import com.scaffold.common.log.persist.LogPersister;

/**
 * 异步保存业务留痕：经 {@link LogPersister} SPI 落库
 * （默认 SLF4J 输出，接入方注册自己的 LogPersister Bean 即可替换为远程审计服务）。
 *
 * <p>镜像 {@link AsyncLogService}：@Async 不占业务线程，
 * 审计侧不可用时留痕静默降级（远端异常由异步未捕获处理器记日志），
 * 业务事务与留痕投递解耦。</p>
 *
 * @author scaffold
 */
@Service
public class AsyncTraceLogService
{
    private final LogPersister logPersister;

    public AsyncTraceLogService(LogPersister logPersister)
    {
        this.logPersister = logPersister;
    }

    /**
     * 异步保存业务留痕条目
     */
    @Async
    public void saveTrace(TraceLogEntry entry)
    {
        logPersister.saveTrace(entry);
    }
}
