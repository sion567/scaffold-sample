package com.scaffold.common.mq;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

import com.scaffold.common.trace.TraceContext;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 消息信封（P2-12，CloudEvents 风格）：统一 Kafka 消息外层结构，便于跨服务演进与排查。
 * <ul>
 * <li>id：消息唯一 ID（消费端幂等键的自然来源，配合 IdempotentChecker）</li>
 * <li>type：事件类型，如 "order.created"、"demo.event"</li>
 * <li>traceId：自动取当前 MDC（网关透传链路），消费端用 TraceContext.bindOrNew(envelope.traceId) 恢复</li>
 * <li>time：ISO-8601 字符串（跨 JSON 序列化器安全，不用 Instant 避免注册模块差异）</li>
 * <li>version：业务 schema 版本（演进时的兼容判断依据）</li>
 * </ul>
 * 用法（发送侧经 MessageProducer 门面自动包信封；自定义信封字段时直发）：
 * <pre>
 * messageProducer.send(topic, key, "order.created", dto);                // 自动包信封
 * messageProducer.send(MessageEnvelope.of("order.created", dto)
 *         .source("order-service").subject(orderId), topic, key);        // 直发信封
 * </pre>
 * 消费端用 {@link Messages#unwrap(String, Class)} 兼容解析（信封/裸载荷两形态）。
 *
 * @author scaffold
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageEnvelope<T> implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 信封结构版本（CloudEvents specVersion 语义） */
    private String specVersion = "1.0";

    /** 消息唯一 ID */
    private String id;

    /** 事件类型，约定 {domain}.{event} */
    private String type;

    /** 来源服务名（建议 spring.application.name） */
    private String source;

    /** 业务主键（如 eventHash、orderId），可选 */
    private String subject;

    /** 事件时间（ISO-8601） */
    private String time;

    /** 全链路 traceId（of() 自动取当前 MDC） */
    private String traceId;

    /** 租户 ID（预留） */
    private String tenantId;

    /** 业务 schema 版本 */
    private String version = "v1";

    /** 业务载荷 */
    private T data;

    /** 扩展字段 */
    private Map<String, String> extensions;

    public static <T> MessageEnvelope<T> of(String type, T data)
    {
        MessageEnvelope<T> envelope = new MessageEnvelope<>();
        envelope.id = UUID.randomUUID().toString().replace("-", "");
        envelope.type = type;
        envelope.time = java.time.Instant.now().toString();
        envelope.traceId = TraceContext.current();
        envelope.data = data;
        return envelope;
    }

    public MessageEnvelope<T> source(String source)
    {
        this.source = source;
        return this;
    }

    public MessageEnvelope<T> subject(String subject)
    {
        this.subject = subject;
        return this;
    }

    public MessageEnvelope<T> tenantId(String tenantId)
    {
        this.tenantId = tenantId;
        return this;
    }

    public MessageEnvelope<T> version(String version)
    {
        this.version = version;
        return this;
    }

    public MessageEnvelope<T> extensions(Map<String, String> extensions)
    {
        this.extensions = extensions;
        return this;
    }

    public String getSpecVersion()
    {
        return specVersion;
    }

    public void setSpecVersion(String specVersion)
    {
        this.specVersion = specVersion;
    }

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public String getSource()
    {
        return source;
    }

    public void setSource(String source)
    {
        this.source = source;
    }

    public String getSubject()
    {
        return subject;
    }

    public void setSubject(String subject)
    {
        this.subject = subject;
    }

    public String getTime()
    {
        return time;
    }

    public void setTime(String time)
    {
        this.time = time;
    }

    public String getTraceId()
    {
        return traceId;
    }

    public void setTraceId(String traceId)
    {
        this.traceId = traceId;
    }

    public String getTenantId()
    {
        return tenantId;
    }

    public void setTenantId(String tenantId)
    {
        this.tenantId = tenantId;
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion(String version)
    {
        this.version = version;
    }

    public T getData()
    {
        return data;
    }

    public void setData(T data)
    {
        this.data = data;
    }

    public Map<String, String> getExtensions()
    {
        return extensions;
    }

    public void setExtensions(Map<String, String> extensions)
    {
        this.extensions = extensions;
    }
}
