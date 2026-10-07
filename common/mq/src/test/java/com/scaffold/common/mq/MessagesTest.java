package com.scaffold.common.mq;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.alibaba.fastjson2.JSON;

/**
 * 消费端兼容解析单测：信封形态拆包并恢复链路、裸载荷原样解析、幂等键提取。
 *
 * @author ct
 */
class MessagesTest
{
    @Test
    @DisplayName("裸载荷（存量消息）：按 DTO 原样解析，不触发 trace 绑定")
    void unwrap_raw_payload()
    {
        Map<String, Object> data = new HashMap<>();
        data.put("orderId", 9L);
        data.put("source", "manual");

        Map<?, ?> msg = Messages.unwrap(JSON.toJSONString(data), Map.class);

        assertEquals(9L, ((Number) msg.get("orderId")).longValue());
        assertEquals("manual", msg.get("source"));
    }

    @Test
    @DisplayName("信封载荷：拆出 data + traceId 绑定 MDC")
    void unwrap_envelope_payload()
    {
        Map<String, Object> data = new HashMap<>();
        data.put("orderId", 77L);
        MessageEnvelope<Map<String, Object>> envelope = MessageEnvelope.of("order.created", data);
        envelope.setTraceId("trace-abc");
        String payload = JSON.toJSONString(envelope);

        Map<?, ?> msg = Messages.unwrap(payload, Map.class);

        assertEquals(77L, ((Number) msg.get("orderId")).longValue());
        assertEquals("trace-abc", org.slf4j.MDC.get(com.scaffold.common.trace.TraceContext.MDC_KEY));
        com.scaffold.common.trace.TraceContext.clear();
    }

    @Test
    @DisplayName("空载荷返回 null；envelopeId 对裸载荷返回 null")
    void edge_cases()
    {
        assertNull(Messages.unwrap(null, Map.class));
        assertNull(Messages.unwrap("", Map.class));
        assertNull(Messages.envelopeId("{\"orderId\":1}"));
        assertEquals("msg-1", Messages.envelopeId(
                "{\"specVersion\":\"1.0\",\"id\":\"msg-1\",\"type\":\"t\",\"data\":{}}"));
    }
}
