package com.scaffold.system.api.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.scaffold.system.domain.SysNotice;
import com.scaffold.system.service.ISysNoticeService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * RemoteNoticeServiceImpl Mock 测试（站内通知内部契约：字段映射/默认值/入参守卫）。
 *
 * @author ct
 */
@ExtendWith(MockitoExtension.class)
class RemoteNoticeServiceImplTest
{
    @Mock
    private ISysNoticeService noticeService;

    @InjectMocks
    private RemoteNoticeServiceImpl service;

    @Test
    @DisplayName("push：字段映射 + 类型缺省 1 + 状态 0 + 操作人记 createBy")
    void push_maps_fields()
    {
        when(noticeService.insertNotice(any())).thenReturn(1);

        service.push(null, "规则超阈提醒", "内容", "system(误报治理)");

        ArgumentCaptor<SysNotice> captor = ArgumentCaptor.forClass(SysNotice.class);
        verify(noticeService).insertNotice(captor.capture());
        assertEquals("1", captor.getValue().getNoticeType());
        assertEquals("规则超阈提醒", captor.getValue().getNoticeTitle());
        assertEquals("内容", captor.getValue().getNoticeContent());
        assertEquals("0", captor.getValue().getStatus());
        assertEquals("system(误报治理)", captor.getValue().getCreateBy());
    }

    @Test
    @DisplayName("push：空标题直接返回 0，不落库")
    void push_blank_title_rejected()
    {
        assertEquals(0, service.push("1", "", "内容", "op"));
        verifyNoInteractions(noticeService);
    }
}
