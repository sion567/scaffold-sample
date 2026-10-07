package com.scaffold.system.api.impl;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.scaffold.system.api.UserDirectoryItem;
import com.scaffold.system.repository.SysUserRepository;

/**
 * RemoteUserDirectoryServiceImpl 单测：空集合短路 + mapper 透传。
 *
 * @author ct
 */
@ExtendWith(MockitoExtension.class)
class RemoteUserDirectoryServiceImplTest
{
    @Mock
    private SysUserRepository userRepository;

    private RemoteUserDirectoryServiceImpl service;

    @BeforeEach
    void setUp()
    {
        service = new RemoteUserDirectoryServiceImpl(userRepository);
    }

    @Test
    @DisplayName("listByIds：空集合短路返回空列表，不触 mapper")
    void listByIds_emptyShortCircuit()
    {
        assertTrue(service.listByIds(emptyList()).isEmpty());
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("listByRoleKeys：空集合短路返回空列表，不触 mapper")
    void listByRoleKeys_emptyShortCircuit()
    {
        assertTrue(service.listByRoleKeys(emptyList()).isEmpty());
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("listByIds/listByRoleKeys/listAll：透传 mapper 结果")
    void delegatesToMapper()
    {
        List<UserDirectoryItem> items = List.of(new UserDirectoryItem(1L, "admin", "管理员"));
        when(userRepository.selectDirectoryByIds(List.of(1L))).thenReturn(items);
        when(userRepository.selectDirectoryByRoleKeys(List.of("common"))).thenReturn(items);
        when(userRepository.selectDirectoryAll()).thenReturn(items);

        assertSame(items, service.listByIds(List.of(1L)));
        assertSame(items, service.listByRoleKeys(List.of("common")));
        assertSame(items, service.listAll());
        assertEquals("管理员", service.listAll().get(0).getNickName());
    }
}
