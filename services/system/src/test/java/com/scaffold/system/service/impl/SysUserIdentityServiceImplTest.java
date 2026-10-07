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

import com.scaffold.system.domain.SysUserIdentity;
import com.scaffold.system.repository.SysUserIdentityRepository;
import com.scaffold.system.service.impl.SysUserIdentityServiceImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SysUserIdentityServiceImpl Mock 测试（第三方账号绑定）。
 *
 * <p>覆盖维度：
 * <ul>
 *   <li>bindIdentity：透传 mapper</li>
 *   <li>selectByExternalId：透传 mapper</li>
 *   <li>findUserIdByExternalId：命中返回 userId，未命中返回 null</li>
 *   <li>selectListByUserId：透传 mapper</li>
 *   <li>unbindIdentity / unbindAllByUserId：透传 mapper</li>
 * </ul>
 *
 * @author ct
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysUserIdentityServiceImplTest
{
    @Mock
    private SysUserIdentityRepository identityRepository;

    private SysUserIdentityServiceImpl identityService;

    @BeforeEach
    void setUp()
    {
        identityService = new SysUserIdentityServiceImpl(identityRepository);
    }

    @Nested
    @DisplayName("bindIdentity")
    class BindTests
    {
        @Test
        @DisplayName("bindIdentity → 透传 mapper")
        void bindIdentity_passesThrough()
        {
            when(identityRepository.save(any(SysUserIdentity.class))).thenAnswer(inv -> inv.getArgument(0));
            int rows = identityService.bindIdentity(identity(null, 1L, "wechat", "wx_openid_123"));
            assertEquals(1, rows);
        }
    }

    @Nested
    @DisplayName("selectByExternalId / findUserIdByExternalId")
    class SelectByExternalIdTests
    {
        @Test
        @DisplayName("selectByExternalId → 命中返回 SysUserIdentity")
        void found_returnsIdentity()
        {
            SysUserIdentity id = identity(1L, 2L, "wechat", "wx_openid_123");
            when(identityRepository.findByIdpTypeAndIdpUid("wechat", "wx_openid_123")).thenReturn(java.util.Optional.of(id));

            SysUserIdentity result = identityService.selectByExternalId("wechat", "wx_openid_123");

            assertNotNull(result);
            assertEquals(1L, result.getId());
        }

        @Test
        @DisplayName("selectByExternalId → 未命中返回 null")
        void notFound_returnsNull()
        {
            when(identityRepository.findByIdpTypeAndIdpUid("wechat", "unknown")).thenReturn(java.util.Optional.empty());
            assertNull(identityService.selectByExternalId("wechat", "unknown"));
        }

        @Test
        @DisplayName("findUserIdByExternalId → 命中返回 userId")
        void found_returnsUserId()
        {
            SysUserIdentity id = identity(1L, 2L, "wechat", "wx_openid_123");
            when(identityRepository.findByIdpTypeAndIdpUid("wechat", "wx_openid_123")).thenReturn(java.util.Optional.of(id));

            Long userId = identityService.findUserIdByExternalId("wechat", "wx_openid_123");

            assertEquals(2L, userId);
        }

        @Test
        @DisplayName("findUserIdByExternalId → 未命中返回 null")
        void findUserId_notFound_returnsNull()
        {
            when(identityRepository.findByIdpTypeAndIdpUid("wechat", "unknown")).thenReturn(java.util.Optional.empty());
            assertNull(identityService.findUserIdByExternalId("wechat", "unknown"));
        }
    }

    @Nested
    @DisplayName("selectListByUserId")
    class SelectListByUserIdTests
    {
        @Test
        @DisplayName("selectListByUserId → 透传 mapper")
        void selectListByUserId_passesThrough()
        {
            when(identityRepository.findByUserId(1L)).thenReturn(List.of());
            List<SysUserIdentity> result = identityService.selectListByUserId(1L);
            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("unbindIdentity / unbindAllByUserId")
    class UnbindTests
    {
        @Test
        @DisplayName("unbindIdentity → 透传 mapper")
        void unbindIdentity_passesThrough()
        {
            when(identityRepository.deleteByIdpTypeAndIdpUid("wechat", "wx_openid_123")).thenReturn(1L);
            int rows = identityService.unbindIdentity("wechat", "wx_openid_123");
            assertEquals(1, rows);
        }

        @Test
        @DisplayName("unbindAllByUserId → 透传 mapper")
        void unbindAllByUserId_passesThrough()
        {
            when(identityRepository.deleteByUserId(1L)).thenReturn(3L);
            int rows = identityService.unbindAllByUserId(1L);
            assertEquals(3, rows);
        }
    }

    // ─────────────────────────────────────────────
    // 辅助方法
    // ─────────────────────────────────────────────

    private SysUserIdentity identity(Long id, Long userId, String idpType, String idpUid)
    {
        SysUserIdentity i = new SysUserIdentity();
        i.setId(id);
        i.setUserId(userId);
        i.setIdpType(idpType);
        i.setIdpUid(idpUid);
        return i;
    }
}
