package com.scaffold.message.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 消息发送线程池（§4.5：核心 8 / 队列 1000 / 池满降级同步执行，不阻塞网关线程）
 *
 * @author ct
 */
@Configuration
public class MessageSendExecutorConfig {
  @Bean("messageSendExecutor")
  public Executor messageSendExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(8);
    executor.setMaxPoolSize(16);
    executor.setQueueCapacity(1000);
    executor.setThreadNamePrefix("msg-send-");
    // 池满降级：CallerRuns 由调用线程同步执行（必达落库已在前置同步段完成，此处只影响通道发送时延）
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    executor.initialize();
    return executor;
  }
}
