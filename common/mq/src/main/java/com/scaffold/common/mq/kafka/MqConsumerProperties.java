package com.scaffold.common.mq.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Kafka 消费容错参数（P1-3），在 scaffold-defaults.yml 统一默认值，服务可覆盖。
 *
 * @author ct
 */
@ConfigurationProperties(prefix = "scaffold.mq.consumer")
public class MqConsumerProperties
{
    /** 死信 topic 后缀：{原topic}.dlq */
    private String dlqSuffix = ".dlq";

    private final Retry retry = new Retry();

    /** 死信告警（重试耗进入死信时日志 + 节流站内通知） */
    private final DltAlarm dltAlarm = new DltAlarm();

    public String getDlqSuffix()
    {
        return dlqSuffix;
    }

    public void setDlqSuffix(String dlqSuffix)
    {
        this.dlqSuffix = dlqSuffix;
    }

    public Retry getRetry()
    {
        return retry;
    }

    public DltAlarm getDltAlarm()
    {
        return dltAlarm;
    }

    public static class DltAlarm
    {
        /** 死信告警开关（关闭后仅剩 DefaultErrorHandler 自身的投递失败日志） */
        private boolean enabled = true;

        /** 同 topic 站内通知节流窗口（分钟），防毒消息风暴刷屏 */
        private int throttleMinutes = 10;

        public boolean isEnabled()
        {
            return enabled;
        }

        public void setEnabled(boolean enabled)
        {
            this.enabled = enabled;
        }

        public int getThrottleMinutes()
        {
            return throttleMinutes;
        }

        public void setThrottleMinutes(int throttleMinutes)
        {
            this.throttleMinutes = throttleMinutes;
        }
    }


    public static class Retry
    {
        /** 首次重试等待（毫秒） */
        private long initialIntervalMillis = 200;

        /** 退避倍数 */
        private double multiplier = 2.0;

        /** 单次重试等待上限（毫秒） */
        private long maxIntervalMillis = 5000;

        public long getInitialIntervalMillis()
        {
            return initialIntervalMillis;
        }

        public void setInitialIntervalMillis(long initialIntervalMillis)
        {
            this.initialIntervalMillis = initialIntervalMillis;
        }

        public double getMultiplier()
        {
            return multiplier;
        }

        public void setMultiplier(double multiplier)
        {
            this.multiplier = multiplier;
        }

        public long getMaxIntervalMillis()
        {
            return maxIntervalMillis;
        }

        public void setMaxIntervalMillis(long maxIntervalMillis)
        {
            this.maxIntervalMillis = maxIntervalMillis;
        }
    }
}
