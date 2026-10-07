package com.scaffold.system.service.impl;

import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.scaffold.common.core.constant.CacheConstants;
import com.scaffold.common.core.constant.UserConstants;
import com.scaffold.common.core.exception.ServiceException;
import com.scaffold.common.core.text.Convert;
import com.scaffold.common.redis.service.RedisService;
import com.scaffold.system.domain.SysConfig;
import com.scaffold.system.repository.SysConfigRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SysConfigServiceImpl Mock 测试（边界防御 + 缓存一致性 + 唯一性）。
 *
 * <p>覆盖维度：
 * <ul>
 *   <li>边界防御：selectConfigById / selectConfigList 基础查询</li>
 *   <li>缓存一致性：selectConfigByKey 命中缓存 vs 回源 DB；insertConfig/updateConfig 写缓存；deleteConfigByIds 删缓存</li>
 *   <li>内置参数保护：deleteConfigByIds 内置参数不可删除</li>
 *   <li>唯一性：checkConfigKeyUnique 同 key 不同 id → NOT_UNIQUE，同 id → UNIQUE</li>
 *   <li>缓存操作：loadingConfigCache / clearConfigCache / resetConfigCache</li>
 * </ul>
 *
 * @author ct
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysConfigServiceImplTest
{
    @Mock
    private SysConfigRepository configRepository;

    @Mock
    private RedisService redisService;

    private SysConfigServiceImpl configService;

    @BeforeEach
    void setUp()
    {
        // 必须在构造函数之前 stub：@PostConstruct init() 会在构造时触发 loadingConfigCache
        // 用 any() 匹配任意 SysConfig 实例，确保 init() 不抛异常
        when(configRepository.findAll()).thenReturn(java.util.List.of());
        configService = new SysConfigServiceImpl(configRepository, redisService);
        // 重置 mock 以清除 @PostConstruct 期间的交互记录
        clearInvocations(configRepository, redisService);
    }

    // ─────────────────────────────────────────────
    // selectConfigById / selectConfigList
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("selectConfigById / selectConfigList 基础查询")
    class BasicQueryTests
    {
        @Test
        @DisplayName("selectConfigById：正常查询 → 返回 SysConfig")
        void selectConfigById_normal()
        {
            SysConfig cfg = config(1L, "sys.index.schoolName", "测试学校");
            when(configRepository.findById(1L)).thenReturn(java.util.Optional.of(cfg));

            SysConfig result = configService.selectConfigById(1L);

            assertNotNull(result);
            assertEquals(1L, result.getConfigId());
        }

        @Test
        @DisplayName("selectConfigList：透传 mapper")
        void selectConfigList_passesThrough()
        {
            when(configRepository.list(any())).thenReturn(java.util.List.of());
            configService.selectConfigList(new SysConfig());
            verify(configRepository).list(any());
        }
    }

    // ─────────────────────────────────────────────
    // selectConfigByKey — 缓存一致性
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("selectConfigByKey 缓存与回源")
    class SelectConfigByKeyTests
    {
        private static final String CACHE_KEY = CacheConstants.SYS_CONFIG_KEY + "sys.index.schoolName";

        @Test
        @DisplayName("缓存命中 → 直接返回，不查 DB")
        void cacheHit_returnsWithoutDb()
        {
            when(redisService.getCacheObject(CACHE_KEY)).thenReturn("缓存值");

            String value = configService.selectConfigByKey("sys.index.schoolName");

            assertEquals("缓存值", value);
            verify(configRepository, never()).findByConfigKey(any());
        }

        @Test
        @DisplayName("缓存未命中 → 回源 DB 并回填缓存")
        void cacheMiss_queriesDbAndFillsCache()
        {
            SysConfig cfg = config(1L, "sys.index.schoolName", "DB值");
            when(redisService.getCacheObject(CACHE_KEY)).thenReturn(null);
            when(configRepository.findByConfigKey(anyString())).thenReturn(java.util.Optional.of(cfg));

            String value = configService.selectConfigByKey("sys.index.schoolName");

            assertEquals("DB值", value);
            verify(redisService).setCacheObject(CACHE_KEY, "DB值");
        }

        @Test
        @DisplayName("缓存未命中 + DB 也没有 → 返回空字符串，不写缓存")
        void cacheMiss_dbNotFound_returnsEmpty()
        {
            when(redisService.getCacheObject(CACHE_KEY)).thenReturn(null);
            when(configRepository.findByConfigKey(anyString())).thenReturn(java.util.Optional.empty());

            String value = configService.selectConfigByKey("not.exist.key");

            assertEquals("", value);
            verify(redisService, never()).setCacheObject(anyString(), anyString());
        }
    }

    // ─────────────────────────────────────────────
    // insertConfig / updateConfig — 缓存一致性
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("insertConfig / updateConfig 缓存写策略")
    class InsertUpdateCacheTests
    {
        @Test
        @DisplayName("insertConfig 成功 → 写缓存")
        void insertConfig_success_writesCache()
        {
            SysConfig cfg = config(null, "sys.index.schoolName", "新值");
            when(configRepository.save(cfg)).thenReturn(cfg);

            int rows = configService.insertConfig(cfg);

            assertEquals(1, rows);
            verify(redisService).setCacheObject(
                    CacheConstants.SYS_CONFIG_KEY + "sys.index.schoolName", "新值");
        }

        @Test
        @DisplayName("insertConfig 失败（0 行）→ 不写缓存")
        void insertConfig_fail_noCache()
        {
            SysConfig cfg = config(null, "sys.index.schoolName", "新值");
            when(configRepository.save(cfg)).thenThrow(new RuntimeException("db"));

            assertThrows(RuntimeException.class, () -> configService.insertConfig(cfg));

            verify(redisService, never()).setCacheObject(anyString(), anyString());
        }

        @Test
        @DisplayName("updateConfig key 不变 → 只写新值到缓存")
        void updateConfig_keyUnchanged_writesNewValue()
        {
            SysConfig oldCfg = config(1L, "sys.index.schoolName", "旧值");
            SysConfig newCfg = config(1L, "sys.index.schoolName", "新值");
            when(configRepository.findById(1L)).thenReturn(java.util.Optional.of(oldCfg));
            when(configRepository.save(newCfg)).thenReturn(newCfg);

            int rows = configService.updateConfig(newCfg);

            assertEquals(1, rows);
            // key 不变，不删旧缓存；只写新值
            verify(redisService, never()).deleteObject(anyString());
            verify(redisService).setCacheObject(
                    CacheConstants.SYS_CONFIG_KEY + "sys.index.schoolName", "新值");
        }

        @Test
        @DisplayName("updateConfig key 变更 → 删旧缓存 + 写新缓存")
        void updateConfig_keyChanged_deletesOldAndWritesNew()
        {
            SysConfig oldCfg = config(1L, "old.key", "旧值");
            SysConfig newCfg = config(1L, "new.key", "新值");
            when(configRepository.findById(1L)).thenReturn(java.util.Optional.of(oldCfg));
            when(configRepository.save(newCfg)).thenReturn(newCfg);

            configService.updateConfig(newCfg);

            verify(redisService).deleteObject(CacheConstants.SYS_CONFIG_KEY + "old.key");
            verify(redisService).setCacheObject(CacheConstants.SYS_CONFIG_KEY + "new.key", "新值");
        }

        @Test
        @DisplayName("updateConfig 失败（0 行）→ 不写缓存")
        void updateConfig_fail_noCache()
        {
            SysConfig oldCfg = config(1L, "sys.index.schoolName", "旧值");
            SysConfig newCfg = config(1L, "sys.index.schoolName", "新值");
            when(configRepository.findById(1L)).thenReturn(java.util.Optional.of(oldCfg));
            when(configRepository.save(newCfg)).thenThrow(new RuntimeException("db"));

            assertThrows(RuntimeException.class, () -> configService.updateConfig(newCfg));

            verify(redisService, never()).setCacheObject(anyString(), anyString());
        }
    }

    // ─────────────────────────────────────────────
    // deleteConfigByIds — 内置参数保护
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("deleteConfigByIds 内置参数保护")
    class DeleteConfigTests
    {
        @Test
        @DisplayName("内置参数（configType=Y）→ ServiceException，不删除")
        void buildInConfig_throwsAndDoesNotDelete()
        {
            SysConfig cfg = config(1L, "sys.user.initPassword", "内置值");
            cfg.setConfigType(UserConstants.YES);
            when(configRepository.findById(1L)).thenReturn(java.util.Optional.of(cfg));

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> configService.deleteConfigByIds(new Long[]{1L}));

            assertTrue(ex.getMessage().contains("内置参数"));
            verify(configRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("非内置参数 → 删除并清缓存")
        void normalConfig_deletesAndClearsCache()
        {
            SysConfig cfg = config(2L, "custom.key", "自定义值");
            when(configRepository.findById(2L)).thenReturn(java.util.Optional.of(cfg));
            

            configService.deleteConfigByIds(new Long[]{2L});

            verify(configRepository).deleteById(2L);
            verify(redisService).deleteObject(CacheConstants.SYS_CONFIG_KEY + "custom.key");
        }

        @Test
        @DisplayName("批量删除：含内置参数 → 第一个内置参数抛异常，后面的未处理")
        void batchWithBuildIn_throwsFirst()
        {
            SysConfig cfg1 = config(1L, "buildin.key", "内置");
            cfg1.setConfigType(UserConstants.YES);
            when(configRepository.findById(1L)).thenReturn(java.util.Optional.of(cfg1));

            assertThrows(ServiceException.class,
                    () -> configService.deleteConfigByIds(new Long[]{1L, 2L}));

            verify(configRepository, never()).deleteById(any());
        }
    }

    // ─────────────────────────────────────────────
    // checkConfigKeyUnique — 唯一性
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("checkConfigKeyUnique 唯一性")
    class UniqueCheckTests
    {
        @Test
        @DisplayName("key 无冲突 → UNIQUE")
        void noConflict_returnsUnique()
        {
            when(configRepository.findByConfigKey("unique.key")).thenReturn(java.util.Optional.empty());

            boolean result = configService.checkConfigKeyUnique(config(null, "unique.key", "v"));

            assertEquals(UserConstants.UNIQUE, result);
        }

        @Test
        @DisplayName("key 冲突（不同 configId）→ NOT_UNIQUE")
        void conflictDifferentId_returnsNotUnique()
        {
            when(configRepository.findByConfigKey("same.key"))
                    .thenReturn(java.util.Optional.of(config(2L, "same.key", "v")));

            boolean result = configService.checkConfigKeyUnique(config(1L, "same.key", "v"));

            assertEquals(UserConstants.NOT_UNIQUE, result);
        }

        @Test
        @DisplayName("key 冲突（同 configId）→ UNIQUE（自己不算冲突）")
        void conflictSameId_returnsUnique()
        {
            when(configRepository.findByConfigKey("same.key"))
                    .thenReturn(java.util.Optional.of(config(1L, "same.key", "v")));

            boolean result = configService.checkConfigKeyUnique(config(1L, "same.key", "v"));

            assertEquals(UserConstants.UNIQUE, result);
        }

        @Test
        @DisplayName("configId 为 null → 比较时用 -1L，无冲突 → UNIQUE")
        void nullConfigId_noConflict_returnsUnique()
        {
            when(configRepository.findByConfigKey("new.key")).thenReturn(java.util.Optional.empty());

            boolean result = configService.checkConfigKeyUnique(config(null, "new.key", "v"));

            assertEquals(UserConstants.UNIQUE, result);
        }
    }

    // ─────────────────────────────────────────────
    // 缓存操作
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("缓存操作")
    class CacheOperationTests
    {
        @Test
        @DisplayName("loadingConfigCache：从 DB 加载所有配置并写入缓存")
        void loadingConfigCache_loadsAll()
        {
            List<SysConfig> configs = List.of(
                    config(1L, "key1", "v1"),
                    config(2L, "key2", "v2"));
            when(configRepository.findAll()).thenReturn(configs);

            configService.loadingConfigCache();

            verify(redisService).setCacheObject(CacheConstants.SYS_CONFIG_KEY + "key1", "v1");
            verify(redisService).setCacheObject(CacheConstants.SYS_CONFIG_KEY + "key2", "v2");
        }

        @Test
        @DisplayName("clearConfigCache：删除所有 config 相关 key")
        void clearConfigCache_deletesAll()
        {
            when(redisService.keys(CacheConstants.SYS_CONFIG_KEY + "*"))
                    .thenReturn(List.of("config:key1", "config:key2"));

            configService.clearConfigCache();

            verify(redisService).deleteObject(anyCollection());
        }

        @Test
        @DisplayName("resetConfigCache = clear + reload")
        void resetConfigCache_clearsAndReloads()
        {
            when(configRepository.findAll()).thenReturn(java.util.List.of());
            when(redisService.keys(CacheConstants.SYS_CONFIG_KEY + "*")).thenReturn(List.of());

            configService.resetConfigCache();

            // 顺序：resetConfigCache → clearConfigCache → loadingConfigCache
            verify(redisService).deleteObject(anyCollection());
            verify(configRepository, org.mockito.Mockito.atLeastOnce()).findAll();
        }
    }

    // ─────────────────────────────────────────────
    // 辅助方法
    // ─────────────────────────────────────────────

    private SysConfig config(Long configId, String configKey, String configValue)
    {
        SysConfig c = new SysConfig();
        c.setConfigId(configId);
        c.setConfigKey(configKey);
        c.setConfigValue(configValue);
        c.setConfigType("N");
        return c;
    }
}
