package com.scaffold.system.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import com.scaffold.common.core.jpa.JpaSpecs;
import com.scaffold.common.test.H2JpaTest;
import com.scaffold.system.domain.SysConfig;

/**
 * SysConfig 数据访问切片测试（JPA + H2 Oracle 模式）。
 * 覆盖：IDENTITY 主键、按 key 唯一查询、JpaSpecs 动态条件、@Version 乐观锁、删除。
 *
 * @author scaffold
 */
@H2JpaTest(
        ddl = "sql/system/sys_config_h2.sql",
        entityPackages = "com.scaffold.system.domain")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SysConfigRepositoryTest
{
    @Test

    @Order(1)    @DisplayName("findById：种子数据可查，未命中返回空")
    void findById(SysConfigRepository repository)
    {
        SysConfig config = repository.findById(1L).orElse(null);
        assertNotNull(config);
        assertEquals("sys.index.skinName", config.getConfigKey());
        assertTrue(repository.findById(9999L).isEmpty());
    }

    @Test

    @Order(2)    @DisplayName("新增：IDENTITY 生成主键并回填")
    void insertGeneratesId(SysConfigRepository repository)
    {
        SysConfig config = new SysConfig();
        config.setConfigName("测试配置");
        config.setConfigKey("test.key");
        config.setConfigValue("testValue");
        config.setConfigType("Y");

        assertNull(config.getConfigId());
        repository.save(config);
        assertNotNull(config.getConfigId());
        assertEquals("testValue", repository.findById(config.getConfigId()).orElseThrow().getConfigValue());
    }

    @Test

    @Order(3)    @DisplayName("findByConfigKey：唯一键查询")
    void findByConfigKey(SysConfigRepository repository)
    {
        assertEquals("123456", repository.findByConfigKey("sys.user.initPassword").orElseThrow().getConfigValue());
        assertTrue(repository.findByConfigKey("not.exist.key").isEmpty());
    }

    @Test

    @Order(4)    @DisplayName("JpaSpecs 动态条件：like/eq/全量")
    void dynamicSpec(SysConfigRepository repository)
    {
        List<SysConfig> byName = repository.list(JpaSpecs.likeIf("configName", "用户管理"));
        assertEquals(1, byName.size());

        List<SysConfig> byType = repository.list(JpaSpecs.eqIfNotBlank("configType", "Y"));
        assertTrue(byType.size() >= 2);

        assertEquals(4, repository.list(JpaSpecs.eqIfNotBlank("configType", (String) null)).size(),
                "空条件返回全部种子数据");
    }

    @Test

    @Order(5)    @DisplayName("@Version 乐观锁：旧版本更新被拒，新版本通过")
    void optimisticLock(SysConfigRepository repository)
    {
        SysConfig config = repository.findById(1L).orElseThrow();
        Integer before = config.getVersion();

        config.setConfigValue("skin-red");
        repository.saveAndFlush(config);
        assertEquals(Integer.valueOf(before + 1), repository.findById(1L).orElseThrow().getVersion(),
                "更新后版本号自增");

        // 陈旧版本（基于更新前的快照）→ 冲突
        SysConfig stale = repository.findById(1L).orElseThrow();
        stale.setVersion(before);
        stale.setConfigValue("hacked");
        assertThrows(jakarta.persistence.OptimisticLockException.class, () -> repository.saveAndFlush(stale));
        assertEquals("skin-red", repository.findById(1L).orElseThrow().getConfigValue(), "冲突更新不得落库");
    }

    @Test

    @Order(6)    @DisplayName("删除：物理删除后不可查")
    void delete(SysConfigRepository repository)
    {
        repository.deleteById(1L);
        assertFalse(repository.findById(1L).isPresent());
        repository.deleteAllById(java.util.Arrays.asList(2L, 3L));
        assertEquals(1, repository.count());
    }
}
