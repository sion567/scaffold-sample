package com.scaffold.common.log.persist;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * 日志落库 SPI 装配入口：容器内无 {@link LogPersister} Bean 时
 * 注册默认的 {@link Slf4jLogPersister}（仅打日志）；接入方注册自己的实现即自动覆盖。
 *
 * @author scaffold
 */
@AutoConfiguration
public class LogPersisterAutoConfiguration
{
    @Bean
    @ConditionalOnMissingBean(LogPersister.class)
    public LogPersister logPersister()
    {
        return new Slf4jLogPersister();
    }
}
