package com.scaffold.common.mq.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaOperations;

import com.alibaba.fastjson2.JSON;
import com.scaffold.common.mq.MessageEnvelope;
import com.scaffold.common.mq.MessageProducer;

/**
 * {@link MessageProducer} 的 Kafka 实现：信封包装 + fastjson2 序列化 + 异步发送失败留痕。
 *
 * <p>载荷统一序列化为字符串上链（消费端 {@link com.scaffold.common.mq.Messages} 兼容解析），
 * 不依赖 value-serializer 配置——此前各服务直发 POJO 在 StringSerializer 默认配置下
 * 是运行期才炸的隐患，本实现显式序列化一并规避。</p>
 *
 * @author ct
 */
public class KafkaMessageProducer implements MessageProducer
{
    private static final Logger log = LoggerFactory.getLogger(KafkaMessageProducer.class);

    private final KafkaOperations<Object, Object> kafkaOperations;

    public KafkaMessageProducer(KafkaOperations<Object, Object> kafkaOperations)
    {
        this.kafkaOperations = kafkaOperations;
    }

    @Override
    public void send(String topic, String type, Object data)
    {
        send(topic, null, type, data);
    }

    @Override
    public void send(String topic, String key, String type, Object data)
    {
        send(MessageEnvelope.of(type, data), topic, key);
    }

    @Override
    public void send(MessageEnvelope<?> envelope, String topic, String key)
    {
        String payload = JSON.toJSONString(envelope);
        java.util.concurrent.CompletableFuture<?> future = kafkaOperations.send(topic, key, payload);
        if (future == null)
        {
            return;
        }
        future.whenComplete((result, ex) -> {
            if (ex != null)
            {
                log.error("[MQ] 消息发送失败（broker 不可达/序列化/超时）: topic={}, key={}, type={}, {}",
                        topic, key, envelope.getType(), ex.toString());
            }
        });
    }
}
