package com.scaffold.common.mq;

/**
 * 消息发送门面：业务侧唯一出口，底层 Kafka 实现可整体替换（换 MQ 只动本模块实现）。
 *
 * <p>统一职责：自动包 {@link MessageEnvelope} 信封（消息ID/traceId/事件类型/时间）、
 * fastjson2 序列化为字符串载荷、发送失败异步回调记日志（不阻塞业务线程）。
 * 业务代码不再直接持有 KafkaTemplate。</p>
 *
 * <p>事件类型约定 {@code {domain}.{event}}（如 "order.created"），进信封 type 字段，
 * 供消费端路由与排查；载荷若已是组装好的信封，走 {@link #send(MessageEnvelope, String, String)} 直发。</p>
 *
 * @author scaffold
 */
public interface MessageProducer
{
    /**
     * 发送（无路由键，轮询分区）
     *
     * @param topic 目标 topic（建议各服务以自有 topic 常量类收口，禁魔法字符串）
     * @param type  事件类型（{domain}.{event}）
     * @param data  业务载荷（POJO/Map，自动包信封并序列化）
     */
    void send(String topic, String type, Object data);

    /**
     * 发送（带路由键：同键消息进同分区保序，如 orderId/recordId）
     *
     * @param topic 目标 topic
     * @param key   分区路由键
     * @param type  事件类型
     * @param data  业务载荷
     */
    void send(String topic, String key, String type, Object data);

    /**
     * 直发已组装的信封（需要自定义 subject/extensions 等信封字段时用）
     *
     * @param envelope 组装好的信封（id/time/traceId 已就绪，可用 MessageEnvelope.of 起步）
     * @param topic    目标 topic
     * @param key      分区路由键（可空）
     */
    void send(MessageEnvelope<?> envelope, String topic, String key);
}
