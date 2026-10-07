package com.scaffold.common.job;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 定时任务 starter 配置（scaffold.job.*）
 *
 * @author ct
 */
@ConfigurationProperties(prefix = "scaffold.job")
public class JobProperties {
    /** 总开关（false 时 @ScaffoldJob 注解完全不生效） */
    private boolean enabled = true;

    /** 锁配置（抢不到锁 = 另一实例在执行，本次跳过） */
    private final Lock lock = new Lock();

    /** 执行日志回写任务中心（任务中心不可达时只告警不阻断） */
    private final Report report = new Report();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Lock getLock() {
        return lock;
    }

    public Report getReport() {
        return report;
    }

    public static class Lock {
        /** 防重锁开关（Redis 不可用时自动降级为仅留痕，多实例会重复执行） */
        private boolean enabled = true;

        /** 锁键前缀 */
        private String keyPrefix = "scaffold:job:lock:";

        /** 抢锁等待毫秒数（0=只尝试一次；另一实例在跑即放弃本次） */
        private long waitMillis = 1000L;

        /** 锁租期毫秒数（执行中每 TTL/2 自动续约；实例宕机后最迟 TTL 释放，触发他节点接管） */
        private long leaseMillis = 600_000L;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getKeyPrefix() {
            return keyPrefix;
        }

        public void setKeyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;
        }

        public long getWaitMillis() {
            return waitMillis;
        }

        public void setWaitMillis(long waitMillis) {
            this.waitMillis = waitMillis;
        }

        public long getLeaseMillis() {
            return leaseMillis;
        }

        public void setLeaseMillis(long leaseMillis) {
            this.leaseMillis = leaseMillis;
        }
    }

    public static class Report {
        /** 日志回写开关 */
        private boolean enabled = true;

        /** 回写超时毫秒数（任务线程内同步调用，超时不阻断任务本身） */
        private int timeoutMillis = 3000;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getTimeoutMillis() {
            return timeoutMillis;
        }

        public void setTimeoutMillis(int timeoutMillis) {
            this.timeoutMillis = timeoutMillis;
        }
    }
}
