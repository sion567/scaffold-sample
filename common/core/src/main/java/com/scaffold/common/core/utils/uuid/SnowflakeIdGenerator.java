package com.scaffold.common.core.utils.uuid;

/**
 * 雪花 ID 生成器（64 位：1 符号位 + 41 毫秒时间戳 + 5 数据中心 + 5 机器 + 12 序列）
 *
 * 主键由应用层生成：不依赖 DB 自增/序列，跨数据库行为一致，
 * 且天然规避 sequence 在高频写入下的争用。
 * 多实例部署时 workerId/dataCenterId 必须各自不同；
 * Spring 环境经 SnowflakeIdGeneratorConfiguration 装配（scaffold.snowflake.* 可配）。
 *
 * @author ct
 */
public class SnowflakeIdGenerator
{
    /** 起始时间戳：2026-01-01 00:00:00 CST */
    private static final long EPOCH = 1767196800000L;

    private static final long WORKER_ID_BITS = 5L;
    private static final long DATA_CENTER_ID_BITS = 5L;
    private static final long SEQUENCE_BITS = 12L;

    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    private static final long MAX_DATA_CENTER_ID = ~(-1L << DATA_CENTER_ID_BITS);

    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long DATA_CENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATA_CENTER_ID_BITS;
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    /** 时钟回拨最大容忍毫秒数，超过直接抛错避免生成重复 ID */
    private static final long MAX_BACKWARD_MS = 5L;

    private final long workerId;
    private final long dataCenterId;

    private long sequence;
    private long lastTimestamp = -1L;

    public SnowflakeIdGenerator(long workerId, long dataCenterId)
    {
        if (workerId < 0 || workerId > MAX_WORKER_ID)
        {
            throw new IllegalArgumentException("workerId 须在 0~" + MAX_WORKER_ID);
        }
        if (dataCenterId < 0 || dataCenterId > MAX_DATA_CENTER_ID)
        {
            throw new IllegalArgumentException("dataCenterId 须在 0~" + MAX_DATA_CENTER_ID);
        }
        this.workerId = workerId;
        this.dataCenterId = dataCenterId;
    }

    /**
     * 生成全局唯一、趋势递增的雪花 ID
     */
    public synchronized long nextId()
    {
        long timestamp = System.currentTimeMillis();
        if (timestamp < lastTimestamp)
        {
            long offset = lastTimestamp - timestamp;
            if (offset > MAX_BACKWARD_MS)
            {
                throw new IllegalStateException("时钟回拨 " + offset + "ms，拒绝生成 ID");
            }
            // 小幅回拨：等待追平
            timestamp = lastTimestamp;
        }
        if (timestamp == lastTimestamp)
        {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0)
            {
                // 当前毫秒序列耗尽，自旋到下一毫秒
                while ((timestamp = System.currentTimeMillis()) <= lastTimestamp)
                {
                }
            }
        }
        else
        {
            sequence = 0L;
        }
        lastTimestamp = timestamp;
        return ((timestamp - EPOCH) << TIMESTAMP_SHIFT)
                | (dataCenterId << DATA_CENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }
}
