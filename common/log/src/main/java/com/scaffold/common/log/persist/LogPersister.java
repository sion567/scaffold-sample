package com.scaffold.common.log.persist;

import com.scaffold.common.log.domain.TraceLogEntry;
import com.scaffold.system.api.domain.SysLogininfor;
import com.scaffold.system.api.domain.SysOperLog;

/**
 * 日志落库 SPI：操作日志 / 登录日志 / 业务留痕的持久化抽象。
 *
 * <p>脚手架本身不携带审计服务——切面（{@code LogAspect}/{@code TraceLogAspect}）
 * 采集完成后经本接口投递，默认实现 {@link Slf4jLogPersister} 仅打 INFO 日志、
 * 不做任何远程调用；接入方注册自己的 {@code LogPersister} Bean（写库/写远程审计）
 * 即可整体替换，切面与服务层代码零改动。</p>
 *
 * @author scaffold
 */
public interface LogPersister
{
    /**
     * 保存操作日志（{@code @Log} 注解切面采集）
     *
     * @param operLog 操作日志
     */
    void saveLog(SysOperLog operLog);

    /**
     * 保存登录日志（登录/登出/注册/失败等认证事件）
     *
     * @param logininfor 登录日志
     */
    void saveLogininfor(SysLogininfor logininfor);

    /**
     * 保存业务留痕条目（{@code @TraceLog} 注解切面采集）
     *
     * @param entry 留痕条目
     */
    void saveTrace(TraceLogEntry entry);
}
