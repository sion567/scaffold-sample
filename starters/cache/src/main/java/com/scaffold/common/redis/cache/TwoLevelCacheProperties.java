package com.scaffold.common.redis.cache;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 两级缓存参数（P2-2），scaffold-defaults.yml 提供默认值，服务可覆盖。
 *
 * @author ct
 */
@ConfigurationProperties(prefix = "scaffold.cache")
public class TwoLevelCacheProperties
{
    /** L1 本地缓存 TTL（秒）：短，靠 TTL 兜底实例间短暂不一致 */
    private long l1TtlSeconds = 300;

    /** L2 Redis 缓存 TTL（秒）：长 */
    private long l2TtlSeconds = 1800;

    /** L1 最大条目数 */
    private long l1MaxSize = 10000;

    public long getL1TtlSeconds()
    {
        return l1TtlSeconds;
    }

    public void setL1TtlSeconds(long l1TtlSeconds)
    {
        this.l1TtlSeconds = l1TtlSeconds;
    }

    public long getL2TtlSeconds()
    {
        return l2TtlSeconds;
    }

    public void setL2TtlSeconds(long l2TtlSeconds)
    {
        this.l2TtlSeconds = l2TtlSeconds;
    }

    public long getL1MaxSize()
    {
        return l1MaxSize;
    }

    public void setL1MaxSize(long l1MaxSize)
    {
        this.l1MaxSize = l1MaxSize;
    }
}
