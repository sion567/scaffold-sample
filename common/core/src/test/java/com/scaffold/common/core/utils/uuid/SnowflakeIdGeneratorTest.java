package com.scaffold.common.core.utils.uuid;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SnowflakeIdGenerator 单元测试：唯一性、趋势递增、参数与回拨校验。
 */
class SnowflakeIdGeneratorTest
{
    @Test
    @DisplayName("连续生成 10000 个 ID：全局唯一且趋势递增")
    void nextId_uniqueAndIncreasing()
    {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1, 1);

        Set<Long> ids = new HashSet<>();
        long prev = -1L;
        for (int i = 0; i < 10_000; i++)
        {
            long id = generator.nextId();
            assertTrue(ids.add(id), "ID 重复: " + id);
            assertTrue(id > prev, "ID 未递增: " + id + " <= " + prev);
            prev = id;
        }
        assertEquals(10_000, ids.size());
    }

    @Test
    @DisplayName("不同 worker/数据中心组合 ID 互不相同（多实例场景）")
    void nextId_distinctAcrossInstances()
    {
        SnowflakeIdGenerator g1 = new SnowflakeIdGenerator(1, 1);
        SnowflakeIdGenerator g2 = new SnowflakeIdGenerator(2, 1);
        SnowflakeIdGenerator g3 = new SnowflakeIdGenerator(1, 2);

        Set<Long> ids = new HashSet<>();
        for (int i = 0; i < 1_000; i++)
        {
            assertTrue(ids.add(g1.nextId()));
            assertTrue(ids.add(g2.nextId()));
            assertTrue(ids.add(g3.nextId()));
        }
    }

    @Test
    @DisplayName("workerId/dataCenterId 越界拒绝构造")
    void constructor_validatesRange()
    {
        assertThrows(IllegalArgumentException.class, () -> new SnowflakeIdGenerator(32, 1));
        assertThrows(IllegalArgumentException.class, () -> new SnowflakeIdGenerator(1, 32));
        assertThrows(IllegalArgumentException.class, () -> new SnowflakeIdGenerator(-1, 1));
    }
}
