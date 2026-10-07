package com.scaffold.system.service.impl;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.scaffold.system.domain.SysNotice;
import com.scaffold.system.domain.SysNoticeRead;
import com.scaffold.system.repository.SysNoticeReadRepository;
import com.scaffold.system.repository.SysNoticeRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SysNoticeReadServiceImpl Mock 测试（公告已读记录）。
 *
 * <p>覆盖维度：
 * <ul>
 *   <li>标记已读：markRead / markReadBatch</li>
 *   <li>查询：selectUnreadCount / selectNoticeListWithReadStatus / selectReadUsersByNoticeId</li>
 *   <li>删除：deleteByNoticeIds</li>
 * </ul>
 *
 * @author ct
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysNoticeReadServiceImplTest
{
    @Mock
    private SysNoticeReadRepository noticeReadRepository;

    @Mock
    private SysNoticeRepository noticeRepository;

    private SysNoticeReadServiceImpl noticeReadService;

    @BeforeEach
    void setUp()
    {
        noticeReadService = new SysNoticeReadServiceImpl(noticeReadRepository, noticeRepository);
    }

    @Nested
    @DisplayName("markRead / markReadBatch")
    class MarkReadTests
    {
        @Test
        @DisplayName("markRead → 插入已读记录")
        void markRead_insertsRecord()
        {
            when(noticeReadRepository.save(any(SysNoticeRead.class))).thenAnswer(inv -> inv.getArgument(0));
            noticeReadService.markRead(10L, 1L);
            verify(noticeReadRepository).save(any(SysNoticeRead.class));
        }

        @Test
        @DisplayName("markReadBatch → noticeIds 为 null 时提前返回，不调 mapper")
        void markReadBatch_nullIds_returnsEarly()
        {
            noticeReadService.markReadBatch(1L, null);
            // 提前返回，mapper 不被调用
        }

        @Test
        @DisplayName("markReadBatch → noticeIds 为空数组时提前返回，不调 mapper")
        void markReadBatch_emptyIds_returnsEarly()
        {
            noticeReadService.markReadBatch(1L, new Long[]{});
            // 提前返回，mapper 不被调用
        }

        @Test
        @DisplayName("markReadBatch → 正常调用 mapper")
        void markReadBatch_normal()
        {
            
            noticeReadService.markReadBatch(1L, new Long[]{10L, 20L});
            verify(noticeReadRepository).saveAll(any());
        }
    }

    @Nested
    @DisplayName("查询")
    class QueryTests
    {
        @Test
        @DisplayName("selectUnreadCount → 透传 mapper")
        void selectUnreadCount_passesThrough()
        {
            when(noticeReadRepository.countUnread(1L)).thenReturn(5L);
            int count = (int) noticeReadService.selectUnreadCount(1L);
            assertEquals(5, count);
        }

        @Test
        @DisplayName("selectNoticeListWithReadStatus → 透传 mapper")
        void selectNoticeListWithReadStatus_passesThrough()
        {
            List<SysNotice> notices = List.of(new SysNotice());
            when(noticeRepository.findByStatusOrderByNoticeIdDesc(eq("0"), eq(org.springframework.data.domain.PageRequest.of(0, 10)))).thenReturn(notices);
            List<SysNotice> result = noticeReadService.selectNoticeListWithReadStatus(1L, 10);
            assertSame(notices, result);
        }

        @Test
        @DisplayName("selectReadUsersByNoticeId → 透传 mapper")
        void selectReadUsersByNoticeId_passesThrough()
        {
            List<Map<String, Object>> users = List.of(Map.of("userName", "admin"));
            when(noticeReadRepository.findReadUsers(10L, "admin")).thenReturn(users);
            List<Map<String, Object>> result = noticeReadService.selectReadUsersByNoticeId(10L, "admin");
            assertSame(users, result);
        }
    }

    @Nested
    @DisplayName("deleteByNoticeIds")
    class DeleteTests
    {
        @Test
        @DisplayName("deleteByNoticeIds → 透传 mapper")
        void deleteByNoticeIds_passesThrough()
        {
            when(noticeReadRepository.deleteByNoticeIds(java.util.Arrays.asList(10L, 20L))).thenReturn(2);
            noticeReadService.deleteByNoticeIds(new Long[]{10L, 20L});
            verify(noticeReadRepository).deleteByNoticeIds(java.util.Arrays.asList(10L, 20L));
        }
    }
}
