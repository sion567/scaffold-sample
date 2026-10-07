package com.scaffold.common.mq.kafka;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.scaffold.system.api.RemoteNoticeService;

/**
 * 死信告警器单测：ERROR 留痕 + 按 topic 节流站内通知 + 通知通道异常不外抛。
 *
 * @author ct
 */
class DltNoticeAlerterTest
{
    private final RemoteNoticeService noticeService = mock(RemoteNoticeService.class);

    private final ConsumerRecord<?, ?> record = mock(ConsumerRecord.class);

    private MqConsumerProperties properties;

    private DltNoticeAlerter alerter;

    @BeforeEach
    void setUp()
    {
        properties = new MqConsumerProperties();
        alerter = new DltNoticeAlerter(properties);
        alerter.setNoticeService(noticeService);
        org.mockito.Mockito.when(record.topic()).thenReturn("demo.topic");
        org.mockito.Mockito.when(record.partition()).thenReturn(3);
        org.mockito.Mockito.when(record.offset()).thenReturn(1024L);
    }

    @Test
    @DisplayName("死信：站内通知推送，内容含原 topic 与死信 topic")
    void pushes_notice()
    {
        alerter.onDeadLetter(record, new IllegalStateException("boom"), "demo.topic.dlq");

        verify(noticeService).push(anyString(), contains("demo.topic"),
                contains("demo.topic.dlq"), anyString());
    }

    @Test
    @DisplayName("节流：同 topic 窗口内第二条死信不再刷通知")
    void throttles_per_topic()
    {
        alerter.onDeadLetter(record, new IllegalStateException("boom-1"), "demo.topic.dlq");
        alerter.onDeadLetter(record, new IllegalStateException("boom-2"), "demo.topic.dlq");

        verify(noticeService, org.mockito.Mockito.times(1)).push(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("告警开关关闭：不推送（日志仍留痕）；通知通道异常不外抛")
    void disabled_and_exception_safe()
    {
        properties.getDltAlarm().setEnabled(false);
        alerter.onDeadLetter(record, new IllegalStateException("boom"), "demo.topic.dlq");
        verify(noticeService, never()).push(anyString(), anyString(), any(), anyString());

        properties.getDltAlarm().setEnabled(true);
        doThrow(new RuntimeException("nacos down")).when(noticeService)
                .push(anyString(), anyString(), anyString(), anyString());
        alerter.onDeadLetter(record, new IllegalStateException("boom"), "demo.topic.dlq");
    }
}
