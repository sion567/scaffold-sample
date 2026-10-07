package com.scaffold.common.core.config;

import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.task.ThreadPoolTaskExecutorCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskDecorator;

/**
 * @EnableAsync 默认线程池治理（P1-4，starter 化：scaffold.async.enabled=false 可关闭恢复 Boot 默认行为）：
 * <ul>
 * <li>Boot 默认 applicationTaskExecutor 队列无界，高峰堆积有 OOM 风险——
 *     队列容量等参数由 scaffold-defaults.yml 的 spring.task.execution.* 统一收敛为有界；</li>
 * <li>拒绝策略：队列满时告警并由调用线程执行（不丢任务、不向业务抛异常）；</li>
 * <li>TaskDecorator：@Async 跨线程时透传 traceId（MDC），异步日志也能串联链路。</li>
 * </ul>
 *
 * @author ct
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "scaffold.async", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AsyncExecutorConfiguration
{
    @Bean
    public ThreadPoolTaskExecutorCustomizer asyncTaskExecutorCustomizer()
    {
        // Boot 3.4+ 自动把 ThreadPoolTaskExecutorCustomizer Bean 应用到默认 applicationTaskExecutor
        return executor -> {
            executor.setRejectedExecutionHandler(new LoggedCallerRunsPolicy());
            executor.setTaskDecorator(new TraceMdcTaskDecorator());
        };
    }

    /**
     * 队列满：记录告警后退化为调用线程执行（不丢任务；CallerRuns 天然形成背压）
     */
    static class LoggedCallerRunsPolicy extends ThreadPoolExecutor.CallerRunsPolicy
    {
        private static final Logger log = LoggerFactory.getLogger(LoggedCallerRunsPolicy.class);

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor)
        {
            log.warn("[异步线程池] 队列已满（size={}），任务交由调用线程执行: {}",
                    executor.getQueue().size(), r.getClass().getSimpleName());
            super.rejectedExecution(r, executor);
        }
    }

    /**
     * 提交时快照 MDC，执行时恢复、结束后还原，避免池化线程上下文串号
     */
    static class TraceMdcTaskDecorator implements TaskDecorator
    {
        @Override
        public Runnable decorate(Runnable runnable)
        {
            Map<String, String> captured = MDC.getCopyOfContextMap();
            return () -> {
                Map<String, String> previous = MDC.getCopyOfContextMap();
                if (captured != null)
                {
                    MDC.setContextMap(captured);
                }
                else
                {
                    MDC.clear();
                }
                try
                {
                    runnable.run();
                }
                finally
                {
                    if (previous != null)
                    {
                        MDC.setContextMap(previous);
                    }
                    else
                    {
                        MDC.clear();
                    }
                }
            };
        }
    }
}
