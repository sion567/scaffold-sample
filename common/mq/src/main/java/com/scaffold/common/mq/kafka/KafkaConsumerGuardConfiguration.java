package com.scaffold.common.mq.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.RetryListener;
import org.springframework.util.backoff.ExponentialBackOff;

/**
 * Kafka 消费容错装配（P1-3）：指数退避重试 + 死信队列。
 * <p>
 * Boot 会自动把 CommonErrorHandler Bean 挂到所有 @KafkaListener 容器工厂。行为：
 * <ul>
 * <li>消费异常按 200ms→400ms→…→5s 指数退避重试（期间该分区暂停，不丢消息也不阻塞其他分区）；</li>
 * <li>重试耗尽：发往 {原topic}.dlq（保留原分区号，可人工/程序回放），offset 正常提交——
 *     毒消息不再阻塞分区、也不再静默丢弃；</li>
 * <li>无 KafkaTemplate 时退化为"重试耗尽后 ERROR 日志 + 跳过"。</li>
 * </ul>
 * 监听器注意：不要在业务里 try-catch 吞异常——异常必须抛给错误处理器才能进入重试/DLQ；
 * 确实容忍丢失的高频流（如 AIS 报文）可维持原地 catch，二者不冲突。
 * 通过 ct.mq.consumer.error-handler.enabled=false 可整体关闭。
 *
 * @author ct
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name = "org.springframework.kafka.core.KafkaTemplate")
@ConditionalOnProperty(prefix = "scaffold.mq.consumer.error-handler", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(MqConsumerProperties.class)
public class KafkaConsumerGuardConfiguration
{
    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerGuardConfiguration.class);

    @Bean
    public DltNoticeAlerter dltNoticeAlerter(MqConsumerProperties properties)
    {
        return new DltNoticeAlerter(properties);
    }

    @Bean
    public DefaultErrorHandler kafkaDefaultErrorHandler(ObjectProvider<KafkaOperations<Object, Object>> kafkaOperations,
                                                        MqConsumerProperties properties,
                                                        DltNoticeAlerter dltNoticeAlerter)
    {
        MqConsumerProperties.Retry retry = properties.getRetry();
        ExponentialBackOff backOff = new ExponentialBackOff(retry.getInitialIntervalMillis(), retry.getMultiplier());
        backOff.setMaxInterval(retry.getMaxIntervalMillis());

        KafkaOperations<Object, Object> template = kafkaOperations.getIfAvailable();
        DefaultErrorHandler handler;
        if (template != null)
        {
            DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(template,
                    (record, ex) -> new TopicPartition(record.topic() + properties.getDlqSuffix(), record.partition()));
            handler = new DefaultErrorHandler(recoverer, backOff);
        }
        else
        {
            handler = new DefaultErrorHandler((record, ex) ->
                    log.error("[Kafka] 重试耗尽且无 KafkaTemplate，消息跳过: topic={}, offset={}, {}",
                            record.topic(), record.offset(), ex.toString()), backOff);
        }
        handler.setRetryListeners(new RetryListener()
        {
            @Override
            public void failedDelivery(ConsumerRecord<?, ?> record, Exception ex, int attempt)
            {
                log.warn("[Kafka] 消费第 {} 次投递失败（将按指数退避重试）: topic={}, partition={}, offset={}, {}",
                        attempt, record.topic(), record.partition(), record.offset(), ex.toString());
            }

            @Override
            public void recovered(ConsumerRecord<?, ?> record, Exception ex)
            {
                // recoverer（死信发布）成功后回调：ERROR 留痕 + 节流站内通知
                dltNoticeAlerter.onDeadLetter(record, ex, record.topic() + properties.getDlqSuffix());
            }

            @Override
            public void recoveryFailed(ConsumerRecord<?, ?> record, Exception ex, Exception recoveryEx)
            {
                log.error("[Kafka] 死信发布失败（下轮重试会再次尝试死信）: topic={}, partition={}, offset={}, {}",
                        record.topic(), record.partition(), record.offset(), recoveryEx.toString());
            }
        });
        return handler;
    }
}
