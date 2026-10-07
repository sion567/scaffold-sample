package com.scaffold.message.service.impl;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.scaffold.system.api.RemoteUserDirectoryService;
import com.scaffold.system.api.UserDirectoryItem;

/**
 * UserDirectoryClient 单测：@DubboReference 字段经反射注入 mock，验证三个查询透传。
 *
 * @author ct
 */
@ExtendWith(MockitoExtension.class)
class UserDirectoryClientTest
{
    @Mock
    private RemoteUserDirectoryService userDirectoryService;

    private UserDirectoryClient client;

    @BeforeEach
    void setUp() throws Exception
    {
        client = new UserDirectoryClient();
        var field = UserDirectoryClient.class.getDeclaredField("userDirectoryService");
        field.setAccessible(true);
        field.set(client, userDirectoryService);
    }

    @Test
    @DisplayName("listAll/listByIds/listByRoleKeys：透传契约结果")
    void delegatesToContract()
    {
        List<UserDirectoryItem> items = List.of(new UserDirectoryItem(1L, "admin", "管理员"));
        when(userDirectoryService.listAll()).thenReturn(items);
        when(userDirectoryService.listByIds(List.of(1L))).thenReturn(items);
        when(userDirectoryService.listByRoleKeys(List.of("common"))).thenReturn(items);

        assertSame(items, client.listAll());
        assertSame(items, client.listByIds(List.of(1L)));
        assertSame(items, client.listByRoleKeys(List.of("common")));
    }

    @Test
    @DisplayName("契约返回空列表时透传空列表")
    void passesThroughEmpty()
    {
        when(userDirectoryService.listAll()).thenReturn(emptyList());

        assertEquals(0, client.listAll().size());
    }
}
