package com.scaffold.system.service.impl;

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

import com.scaffold.common.core.constant.UserConstants;
import com.scaffold.system.domain.SysNotice;
import com.scaffold.system.repository.SysNoticeRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SysNoticeServiceImpl Mock 测试（基础 CRUD + 基础查询）。
 *
 * <p>覆盖维度：
 * <ul>
 *   <li>基础查询：selectNoticeById / selectNoticeList</li>
 *   <li>CRUD：insertNotice / updateNotice / deleteNoticeById / deleteNoticeByIds</li>
 * </ul>
 *
 * @author ct
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysNoticeServiceImplTest
{
    @Mock
    private SysNoticeRepository noticeRepository;

    private SysNoticeServiceImpl noticeService;

    @BeforeEach
    void setUp()
    {
        noticeService = new SysNoticeServiceImpl(noticeRepository);
    }

    @Nested
    @DisplayName("基础查询")
    class QueryTests
    {
        @Test
        @DisplayName("selectNoticeById → 透传 mapper")
        void selectNoticeById_normal()
        {
            SysNotice n = notice(1L, "系统公告");
            when(noticeRepository.findById(1L)).thenReturn(java.util.Optional.of(n));

            SysNotice result = noticeService.selectNoticeById(1L);

            assertNotNull(result);
            assertEquals("系统公告", result.getNoticeTitle());
        }

        @Test
        @DisplayName("selectNoticeList → 透传 mapper")
        void selectNoticeList_passesThrough()
        {
            when(noticeRepository.list(any())).thenReturn(List.of());
            noticeService.selectNoticeList(new SysNotice());
            verify(noticeRepository).list(any(), any());
        }
    }

    @Nested
    @DisplayName("CRUD")
    class CrudTests
    {
        @Test
        @DisplayName("insertNotice → 透传 mapper")
        void insertNotice_passesThrough()
        {
            when(noticeRepository.save(any(SysNotice.class))).thenAnswer(inv -> inv.getArgument(0));
            int rows = noticeService.insertNotice(notice(null, "新公告"));
            assertEquals(1, rows);
        }

        @Test
        @DisplayName("updateNotice → 透传 mapper")
        void updateNotice_passesThrough()
        {
            when(noticeRepository.save(any(SysNotice.class))).thenAnswer(inv -> inv.getArgument(0));
            int rows = noticeService.updateNotice(notice(1L, "更新公告"));
            assertEquals(1, rows);
        }

        @Test
        @DisplayName("deleteNoticeById → 透传 mapper")
        void deleteNoticeById_passesThrough()
        {
            
            int rows = noticeService.deleteNoticeById(1L);
            assertEquals(1, rows);
        }

        @Test
        @DisplayName("deleteNoticeByIds → 透传 mapper")
        void deleteNoticeByIds_passesThrough()
        {
            
            int rows = noticeService.deleteNoticeByIds(new Long[]{1L, 2L});
            assertEquals(2, rows);
        }
    }

    // ─────────────────────────────────────────────
    // 辅助方法
    // ─────────────────────────────────────────────

    private SysNotice notice(Long noticeId, String title)
    {
        SysNotice n = new SysNotice();
        n.setNoticeId(noticeId);
        n.setNoticeTitle(title);
        n.setNoticeType("1");
        n.setStatus(UserConstants.NORMAL);
        return n;
    }
}
