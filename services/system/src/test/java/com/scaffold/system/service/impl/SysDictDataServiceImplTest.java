package com.scaffold.system.service.impl;

import java.util.List;

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

import com.scaffold.common.security.utils.DictUtils;
import com.scaffold.system.api.domain.SysDictData;
import com.scaffold.system.repository.SysDictDataRepository;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SysDictDataServiceImpl Mock 测试（字典数据 CRUD）。
 *
 * <p>覆盖维度：
 * <ul>
 *   <li>基础查询：selectDictDataList / selectDictDataById / selectDictLabel</li>
 *   <li>新增：insertDictData → 更新字典缓存</li>
 *   <li>修改：updateDictData → 更新字典缓存</li>
 *   <li>删除：deleteDictDataByIds → 删除后更新缓存</li>
 * </ul>
 *
 * @author ct
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysDictDataServiceImplTest
{
    @Mock
    private SysDictDataRepository dictDataRepository;

    private SysDictDataServiceImpl dictDataService;

    @BeforeEach
    void setUp()
    {
        dictDataService = new SysDictDataServiceImpl(dictDataRepository);
    }

    @Nested
    @DisplayName("查询")
    class QueryTests
    {
        @Test
        @DisplayName("selectDictDataList → 透传 mapper")
        void selectDictDataList_passesThrough()
        {
            when(dictDataRepository.list(any())).thenReturn(List.of());
            List<SysDictData> result = dictDataService.selectDictDataList(new SysDictData());
            assertNotNull(result);
            verify(dictDataRepository).list(any(), any());
        }

        @Test
        @DisplayName("selectDictDataById → 透传 mapper")
        void selectDictDataById_normal()
        {
            SysDictData data = dictData(1L, "sys_user_status", "0", "正常");
            when(dictDataRepository.findById(1L)).thenReturn(java.util.Optional.of(data));
            SysDictData result = dictDataService.selectDictDataById(1L);
            assertNotNull(result);
            assertEquals("正常", result.getDictLabel());
        }

        @Test
        @DisplayName("selectDictLabel → 透传 mapper")
        void selectDictLabel_normal()
        {
            when(dictDataRepository.findByDictTypeAndDictValue("sys_user_status", "0")).thenReturn(java.util.Optional.of(dictData(9L, "sys_user_status", "0", "正常")));
            String label = dictDataService.selectDictLabel("sys_user_status", "0");
            assertEquals("正常", label);
        }
    }

    @Nested
    @DisplayName("insertDictData")
    class InsertTests
    {
        @Test
        @DisplayName("插入成功 → 更新字典缓存")
        void insert_success_updatesCache()
        {
            SysDictData data = dictData(null, "sys_user_status", "0", "正常");
            when(dictDataRepository.save(any(SysDictData.class))).thenReturn(data);
            when(dictDataRepository.findByDictTypeAndStatusOrderByDictSortAsc("sys_user_status", "0")).thenReturn(List.of(data));

            try (MockedStatic<DictUtils> dictUtils = org.mockito.Mockito.mockStatic(DictUtils.class))
            {
                dictUtils.when(() -> DictUtils.setDictCache(eq("sys_user_status"), any())).thenAnswer(i -> null);
                int rows = dictDataService.insertDictData(data);
                assertEquals(1, rows);
                dictUtils.verify(() -> DictUtils.setDictCache(eq("sys_user_status"), any()));
            }
        }

        @Test
        @DisplayName("插入失败 → 不更新缓存")
        void insert_fail_noCacheUpdate()
        {
            SysDictData data = dictData(null, "sys_user_status", "0", "正常");
            when(dictDataRepository.save(any(SysDictData.class))).thenThrow(new RuntimeException("db-down"));

            try (MockedStatic<DictUtils> dictUtils = org.mockito.Mockito.mockStatic(DictUtils.class))
            {
                assertThrows(RuntimeException.class, () -> dictDataService.insertDictData(data));
                dictUtils.verifyNoInteractions();
            }
        }
    }

    @Nested
    @DisplayName("updateDictData")
    class UpdateTests
    {
        @Test
        @DisplayName("更新成功 → 更新字典缓存")
        void update_success_updatesCache()
        {
            SysDictData data = dictData(1L, "sys_user_status", "0", "启用");
            when(dictDataRepository.save(any(SysDictData.class))).thenReturn(data);
            when(dictDataRepository.findByDictTypeAndStatusOrderByDictSortAsc("sys_user_status", "0")).thenReturn(List.of(data));

            try (MockedStatic<DictUtils> dictUtils = org.mockito.Mockito.mockStatic(DictUtils.class))
            {
                dictUtils.when(() -> DictUtils.setDictCache(eq("sys_user_status"), any())).thenAnswer(i -> null);
                int rows = dictDataService.updateDictData(data);
                assertEquals(1, rows);
                dictUtils.verify(() -> DictUtils.setDictCache(eq("sys_user_status"), any()));
            }
        }
    }

    @Nested
    @DisplayName("deleteDictDataByIds")
    class DeleteTests
    {
        @Test
        @DisplayName("删除 → 更新字典缓存")
        void delete_updatesCache()
        {
            SysDictData data = dictData(1L, "sys_user_status", "0", "正常");
            when(dictDataRepository.findById(1L)).thenReturn(java.util.Optional.of(data));
            
            when(dictDataRepository.findByDictTypeAndStatusOrderByDictSortAsc("sys_user_status", "0")).thenReturn(List.of());

            try (MockedStatic<DictUtils> dictUtils = org.mockito.Mockito.mockStatic(DictUtils.class))
            {
                dictUtils.when(() -> DictUtils.setDictCache(eq("sys_user_status"), any())).thenAnswer(i -> null);
                dictDataService.deleteDictDataByIds(new Long[]{1L});
                dictUtils.verify(() -> DictUtils.setDictCache(eq("sys_user_status"), any()));
            }
        }
    }

    // ─────────────────────────────────────────────
    // 辅助方法
    // ─────────────────────────────────────────────

    private SysDictData dictData(Long dictCode, String dictType, String dictValue, String dictLabel)
    {
        SysDictData d = new SysDictData();
        d.setDictCode(dictCode);
        d.setDictType(dictType);
        d.setDictValue(dictValue);
        d.setDictLabel(dictLabel);
        return d;
    }
}
