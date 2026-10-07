package com.scaffold.common.mq.kafka;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.scaffold.system.api.RemoteNoticeService;

/**
 * 死信告警器：重试耗尽进 {@code {topic}.dlq} 时，ERROR 日志必留痕，
 * 并按 topic 节流推送站内通知（默认同 topic 10 分钟最多一条，防毒消息风暴刷屏）。
 *
 * <p>通知通道 best-effort：RemoteNoticeService 不可达/未装配时静默跳过，
 * 只依赖日志留痕——告警链路自身绝不能影响消费线程。</p>
 *
 * @author ct
 */
public class DltNoticeAlerter
{
    private static final Logger log = LoggerFactory.getLogger(DltNoticeAlerter.class);

    private final MqConsumerProperties properties;

    private final Map<String, Long> lastNoticeAt = new ConcurrentHashMap<>();

    /** 站内通知契约；check=false 注册中心不可达不阻断启动 */
    @Autowired(required = false)
    private RemoteNoticeService noticeService;

    public DltNoticeAlerter(MqConsumerProperties properties)
    {
        this.properties = properties;
    }

    /** 供单测注入 mock */
    void setNoticeService(RemoteNoticeService noticeService)
    {
        this.noticeService = noticeService;
    }

    /**
     * 死信落库告警（由消费容错的重试监听器在 recoverer 成功后回调）
     *
     * @param record 原始毒消息（原 topic/partition/offset）
     * @param ex     耗尽重试的最后一个异常
     * @param dlqTopic 死信 topic（{原topic}.dlq）
     */
    public void onDeadLetter(ConsumerRecord<?, ?> record, Exception ex, String dlqTopic)
    {
        log.error("[MQ] 消息重试耗尽已入死信，请排查后人工回放: topic={}, partition={}, offset={}, dlq={}, 原因: {}",
                record.topic(), record.partition(), record.offset(), dlqTopic, ex.toString(), ex);

        if (!properties.getDltAlarm().isEnabled() || noticeService == null)
        {
            return;
        }
        long throttleMillis = properties.getDltAlarm().getThrottleMinutes() * 60_000L;
        long now = System.currentTimeMillis();
        Long last = lastNoticeAt.get(record.topic());
        if (last != null && now - last < throttleMillis)
        {
            return;
        }
        lastNoticeAt.put(record.topic(), now);
        try
        {
            // FIXME:站内通知推给的是广播（noticeType=1），如果后续想只推运维角色，需要配合通知中心的定向能力再调
            noticeService.push("1", "消息死信告警（" + record.topic() + "）",
                    "topic=" + record.topic() + " partition=" + record.partition() + " offset=" + record.offset()
                            + " 已重试耗尽入死信 " + dlqTopic + "，最近异常：" + ex
                            + "。请排查消息内容与消费逻辑，处理后可回放死信。",
                    "system(死信告警)");
        }
        catch (Exception e)
        {
            log.warn("[MQ] 死信站内通知发送失败（不影响告警日志留痕）: {}", e.toString());
        }
    }
}
