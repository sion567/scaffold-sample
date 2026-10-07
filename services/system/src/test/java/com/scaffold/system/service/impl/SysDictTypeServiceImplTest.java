package com.scaffold.system.service.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.scaffold.common.core.constant.UserConstants;
import com.scaffold.common.core.exception.ServiceException;
import com.scaffold.common.security.utils.DictUtils;
import com.scaffold.system.api.domain.SysDictData;
import com.scaffold.system.api.domain.SysDictType;
import com.scaffold.system.repository.SysDictDataRepository;
import com.scaffold.system.repository.SysDictTypeRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SysDictTypeServiceImpl Mock 测试（字典类型 CRUD）。
 *
 * <p>覆盖维度：
 * <ul>
 *   <li>基础查询：selectDictTypeList / selectDictTypeAll / selectDictTypeById / selectDictTypeByType</li>
 *   <li>按类型查字典数据：selectDictDataByType（缓存优先）</li>
 *   <li>缓存操作：loadingDictCache / clearDictCache / resetDictCache</li>
 *   <li>新增/修改/删除：insertDictType / updateDictType / deleteDictTypeByIds</li>
 *   <li>唯一性校验：checkDictTypeUnique</li>
 * </ul>
 *
 * @author ct
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysDictTypeServiceImplTest
{
    @Mock
    private SysDictTypeRepository dictTypeRepository;

    @Mock
    private SysDictDataRepository dictDataRepository;

    private SysDictTypeServiceImpl dictTypeService;

    @BeforeEach
    void setUp()
    {
        // Stub mapper before construction to prevent @PostConstruct from making real calls
        when(dictDataRepository.findByStatusOrderByDictSortAsc(anyString())).thenReturn(List.of());
        dictTypeService = new SysDictTypeServiceImpl(dictTypeRepository, dictDataRepository);
    }

    @Nested
    @DisplayName("查询")
    class QueryTests
    {
        @Test
        @DisplayName("selectDictTypeList → 透传 mapper")
        void selectDictTypeList_passesThrough()
        {
            when(dictTypeRepository.list(any())).thenReturn(List.of());
            List<SysDictType> result = dictTypeService.selectDictTypeList(new SysDictType());
            assertNotNull(result);
        }

        @Test
        @DisplayName("selectDictTypeAll → 透传 mapper")
        void selectDictTypeAll_passesThrough()
        {
            when(dictTypeRepository.findAll()).thenReturn(List.of());
            List<SysDictType> result = dictTypeService.selectDictTypeAll();
            assertNotNull(result);
        }

        @Test
        @DisplayName("selectDictTypeById → 透传 mapper")
        void selectDictTypeById_normal()
        {
            SysDictType dict = dictType(1L, "sys_user_status", "用户状态");
            when(dictTypeRepository.findById(1L)).thenReturn(java.util.Optional.of(dict));
            SysDictType result = dictTypeService.selectDictTypeById(1L);
            assertNotNull(result);
            assertEquals("sys_user_status", result.getDictType());
        }

        @Test
        @DisplayName("selectDictTypeByType → 透传 mapper")
        void selectDictTypeByType_normal()
        {
            SysDictType dict = dictType(1L, "sys_user_status", "用户状态");
            when(dictTypeRepository.findByDictType("sys_user_status")).thenReturn(java.util.Optional.of(dict));
            SysDictType result = dictTypeService.selectDictTypeByType("sys_user_status");
            assertNotNull(result);
        }

        @Test
        @DisplayName("selectDictDataByType → 缓存命中则直接返回")
        void selectDictDataByType_cacheHit()
        {
            List<SysDictData> cached = List.of(new SysDictData());
            try (MockedStatic<DictUtils> dictUtils = org.mockito.Mockito.mockStatic(DictUtils.class))
            {
                dictUtils.when(() -> DictUtils.getDictCache("sys_user_status")).thenReturn(cached);
                List<SysDictData> result = dictTypeService.selectDictDataByType("sys_user_status");
                assertSame(cached, result);
            }
        }

        @Test
        @DisplayName("selectDictDataByType → 缓存未命中查 mapper 并回填缓存")
        void selectDictDataByType_cacheMiss_queriesMapper()
        {
            List<SysDictData> mapperResult = List.of(new SysDictData());
            try (MockedStatic<DictUtils> dictUtils = org.mockito.Mockito.mockStatic(DictUtils.class))
            {
                dictUtils.when(() -> DictUtils.getDictCache("sys_user_status")).thenReturn(null);
                when(dictDataRepository.findByDictTypeAndStatusOrderByDictSortAsc("sys_user_status", "0")).thenReturn(mapperResult);
                dictUtils.when(() -> DictUtils.setDictCache(eq("sys_user_status"), any())).thenAnswer(i -> null);

                List<SysDictData> result = dictTypeService.selectDictDataByType("sys_user_status");

                assertSame(mapperResult, result);
                dictUtils.verify(() -> DictUtils.setDictCache(eq("sys_user_status"), any()));
            }
        }
    }

    @Nested
    @DisplayName("缓存操作")
    class CacheTests
    {
        @Test
        @DisplayName("loadingDictCache → 分组写入 DictUtils 缓存")
        void loadingDictCache_writesToDictUtils()
        {
            SysDictData data = new SysDictData();
            data.setDictType("sys_user_status");
            when(dictDataRepository.findByStatusOrderByDictSortAsc(anyString())).thenReturn(List.of(data));

            try (MockedStatic<DictUtils> dictUtils = org.mockito.Mockito.mockStatic(DictUtils.class))
            {
                dictUtils.when(() -> DictUtils.setDictCache(eq("sys_user_status"), any())).thenAnswer(i -> null);
                dictTypeService.loadingDictCache();
                dictUtils.verify(() -> DictUtils.setDictCache(eq("sys_user_status"), any()));
            }
        }

        @Test
        @DisplayName("clearDictCache → 调用 DictUtils.clearDictCache")
        void clearDictCache_callsDictUtils()
        {
            try (MockedStatic<DictUtils> dictUtils = org.mockito.Mockito.mockStatic(DictUtils.class))
            {
                dictUtils.when(DictUtils::clearDictCache).thenAnswer(i -> null);
                dictTypeService.clearDictCache();
                dictUtils.verify(DictUtils::clearDictCache);
            }
        }

        @Test
        @DisplayName("resetDictCache → 清空后重新加载")
        void resetDictCache_clearsAndReloads()
        {
            when(dictDataRepository.findByStatusOrderByDictSortAsc(anyString())).thenReturn(List.of());
            try (MockedStatic<DictUtils> dictUtils = org.mockito.Mockito.mockStatic(DictUtils.class))
            {
                dictUtils.when(DictUtils::clearDictCache).thenAnswer(i -> null);
                dictUtils.when(() -> DictUtils.setDictCache(any(), any())).thenAnswer(i -> null);
                dictTypeService.resetDictCache();
                dictUtils.verify(DictUtils::clearDictCache);
            }
        }
    }

    @Nested
    @DisplayName("insertDictType / updateDictType / deleteDictTypeByIds")
    class MutationTests
    {
        @Test
        @DisplayName("insertDictType → 插入后清空该类型缓存")
        void insertDictType_clearsCache()
        {
            SysDictType dict = dictType(null, "sys_user_status", "用户状态");
            when(dictTypeRepository.save(any(SysDictType.class))).thenReturn(null);

            try (MockedStatic<DictUtils> dictUtils = org.mockito.Mockito.mockStatic(DictUtils.class))
            {
                dictUtils.when(() -> DictUtils.setDictCache(any(), any())).thenAnswer(i -> null);
                int rows = dictTypeService.insertDictType(dict);
                assertEquals(1, rows);
                dictUtils.verify(() -> DictUtils.setDictCache(eq("sys_user_status"), eq(null)));
            }
        }

        @Test
        @DisplayName("updateDictType → 更新后回填缓存")
        void updateDictType_updatesCache()
        {
            SysDictType oldDict = dictType(1L, "sys_user_status", "用户状态");
            SysDictType newDict = dictType(1L, "sys_user_status_new", "用户状态(新)");
            when(dictTypeRepository.findById(1L)).thenReturn(java.util.Optional.of(oldDict));
            when(dictTypeRepository.save(any(SysDictType.class))).thenReturn(null);
            when(dictDataRepository.updateDictDataType("sys_user_status", "sys_user_status_new")).thenReturn(1);
            when(dictDataRepository.findByDictTypeAndStatusOrderByDictSortAsc("sys_user_status_new", "0")).thenReturn(List.of());

            try (MockedStatic<DictUtils> dictUtils = org.mockito.Mockito.mockStatic(DictUtils.class))
            {
                dictUtils.when(() -> DictUtils.setDictCache(any(), any())).thenAnswer(i -> null);
                int rows = dictTypeService.updateDictType(newDict);
                assertEquals(1, rows);
                verify(dictDataRepository).updateDictDataType("sys_user_status", "sys_user_status_new");
                dictUtils.verify(() -> DictUtils.setDictCache(eq("sys_user_status_new"), any()));
            }
        }

        @Test
        @DisplayName("deleteDictTypeByIds → 有字典数据则抛异常")
        void deleteDictTypeByIds_withData_throwsException()
        {
            SysDictType dict = dictType(1L, "sys_user_status", "用户状态");
            when(dictTypeRepository.findById(1L)).thenReturn(java.util.Optional.of(dict));
            when(dictDataRepository.countByDictType("sys_user_status")).thenReturn(1L);

            assertThrows(ServiceException.class, () -> dictTypeService.deleteDictTypeByIds(new Long[]{1L}));
        }

        @Test
        @DisplayName("deleteDictTypeByIds → 无字典数据则删除并清缓存")
        void deleteDictTypeByIds_noData_deletesAndClearsCache()
        {
            SysDictType dict = dictType(1L, "sys_user_status", "用户状态");
            when(dictTypeRepository.findById(1L)).thenReturn(java.util.Optional.of(dict));
            when(dictDataRepository.countByDictType("sys_user_status")).thenReturn(0L);
            

            try (MockedStatic<DictUtils> dictUtils = org.mockito.Mockito.mockStatic(DictUtils.class))
            {
                dictUtils.when(() -> DictUtils.removeDictCache("sys_user_status")).thenAnswer(i -> null);
                dictTypeService.deleteDictTypeByIds(new Long[]{1L});
                verify(dictTypeRepository).deleteById(1L);
                dictUtils.verify(() -> DictUtils.removeDictCache("sys_user_status"));
            }
        }
    }

    @Nested
    @DisplayName("checkDictTypeUnique")
    class UniqueTests
    {
        @Test
        @DisplayName("新字典，类型名不冲突 → UNIQUE")
        void newDict_unique_returnsUnique()
        {
            when(dictTypeRepository.findByDictType("sys_user_status")).thenReturn(java.util.Optional.empty());
            boolean result = dictTypeService.checkDictTypeUnique(dictType(null, "sys_user_status", "用户状态"));
            assertTrue(result);  // UserConstants.UNIQUE is truthy (non-zero)
        }

        @Test
        @DisplayName("新字典，类型名已存在 → NOT_UNIQUE")
        void newDict_duplicate_returnsNotUnique()
        {
            when(dictTypeRepository.findByDictType("sys_user_status"))
                    .thenReturn(java.util.Optional.of(dictType(1L, "sys_user_status", "用户状态")));
            boolean result = dictTypeService.checkDictTypeUnique(dictType(null, "sys_user_status", "用户状态"));
            assertFalse(result);  // UserConstants.NOT_UNIQUE is truthy but we return it as-is; mapper found existing → different id → NOT_UNIQUE
        }

        @Test
        @DisplayName("修改同一条记录 → UNIQUE（自己不算冲突）")
        void updateSameRecord_returnsUnique()
        {
            when(dictTypeRepository.findByDictType("sys_user_status"))
                    .thenReturn(java.util.Optional.of(dictType(1L, "sys_user_status", "用户状态")));
            boolean result = dictTypeService.checkDictTypeUnique(dictType(1L, "sys_user_status", "用户状态"));
            assertTrue(result);  // same id → UNIQUE
        }
    }

    // ─────────────────────────────────────────────
    // 辅助方法
    // ─────────────────────────────────────────────

    private SysDictType dictType(Long dictId, String dictType, String dictName)
    {
        SysDictType d = new SysDictType();
        d.setDictId(dictId);
        d.setDictType(dictType);
        d.setDictName(dictName);
        d.setStatus("0");
        return d;
    }
}
