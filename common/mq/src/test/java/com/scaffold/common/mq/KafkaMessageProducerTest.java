package com.scaffold.common.mq;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaOperations;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.scaffold.common.mq.kafka.KafkaMessageProducer;

/**
 * 发送门面实现单测：信封自动包装、路由键透传、发送失败不外抛。
 *
 * @author ct
 */
class KafkaMessageProducerTest
{
    @SuppressWarnings("unchecked")
    private final KafkaOperations<Object, Object> operations = mock(KafkaOperations.class);

    private KafkaMessageProducer producer;

    @BeforeEach
    void setUp()
    {
        producer = new KafkaMessageProducer(operations);
    }

    @Test
    @DisplayName("send：自动包信封（id/type/time 就绪）+ 路由键透传 + data 结构保真")
    void wraps_envelope_and_routes_key()
    {
        Map<String, Object> data = new HashMap<>();
        data.put("orderId", 77L);
        data.put("source", "demo-source");

        producer.send("demo.events", "77", "order.created", data);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(operations).send(eq("demo.events"), eq("77"), payload.capture());
        JSONObject json = JSON.parseObject((String) payload.getValue());
        Assertions.assertEquals("order.created", json.getString("type"));
        Assertions.assertEquals("1.0", json.getString("specVersion"));
        Assertions.assertFalse(json.getString("id").isEmpty());
        Assertions.assertFalse(json.getString("time").isEmpty());
        JSONObject inner = json.getJSONObject("data");
        Assertions.assertEquals(77L, inner.getLongValue("orderId"));
        Assertions.assertEquals("demo-source", inner.getString("source"));
    }

    @Test
    @DisplayName("直发信封：不再二次包装，自定义字段保留")
    void sends_prebuilt_envelope_untouched()
    {
        Map<String, Object> empty = new HashMap<>();
        MessageEnvelope<Map<String, Object>> envelope = MessageEnvelope.of("order.created", empty)
                .source("order-service").subject("order-1");

        producer.send(envelope, "demo.order.events", "order-1");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(operations).send(eq("demo.order.events"), eq("order-1"), payload.capture());
        JSONObject json = JSON.parseObject((String) payload.getValue());
        Assertions.assertEquals("order.created", json.getString("type"));
        Assertions.assertEquals("order-service", json.getString("source"));
        Assertions.assertEquals("order-1", json.getString("subject"));
    }

    @Test
    @DisplayName("发送失败：异步回调记日志，不向业务线程外抛")
    void swallows_send_failure()
    {
        CompletableFuture<Object> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("broker down"));
        doReturn(failed).when(operations).send(any(), any(), any());

        producer.send("any.topic", "type.x", new HashMap<>());
    }
}
