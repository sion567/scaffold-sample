package com.scaffold.common.mq.kafka;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.KafkaTemplate;

import com.scaffold.common.mq.MessageProducer;

/**
 * 消息发送门面自动装配：宿主有 KafkaTemplate 即注册 {@link MessageProducer}，
 * 业务侧注入门面发消息、不再直接持有 KafkaTemplate；
 * ct.mq.producer.enabled=false 可关闭（发送侧回退自管）。
 *
 * @author ct
 */
@AutoConfiguration(afterName = "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration")
@ConditionalOnClass(KafkaTemplate.class)
@ConditionalOnBean(KafkaTemplate.class)
@ConditionalOnProperty(prefix = "scaffold.mq.producer", name = "enabled", havingValue = "true", matchIfMissing = true)
public class KafkaProducerConfiguration
{
    @Bean
    public MessageProducer messageProducer(KafkaOperations<Object, Object> kafkaOperations)
    {
        return new KafkaMessageProducer(kafkaOperations);
    }
}
