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

import com.scaffold.common.security.utils.SecurityUtils;
import com.scaffold.system.domain.SysUserPasswordHistory;
import com.scaffold.system.repository.SysUserPasswordHistoryRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SysUserPasswordHistoryServiceImpl Mock 测试（密码历史记录）。
 *
 * <p>覆盖维度：
 * <ul>
 *   <li>新增：insertPasswordHistory</li>
 *   <li>查询：selectRecentPasswords（边界：limit <= 0 → 空列表）</li>
 *   <li>匹配：matchesRecentHistory（边界条件 + SM3 匹配 + 异常密文跳过）</li>
 *   <li>清理：cleanupHistory（负数 limit 钳制为 0）</li>
 * </ul>
 *
 * @author ct
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysUserPasswordHistoryServiceImplTest
{
    @Mock
    private SysUserPasswordHistoryRepository passwordHistoryRepository;

    private SysUserPasswordHistoryServiceImpl passwordHistoryService;

    @BeforeEach
    void setUp()
    {
        passwordHistoryService = new SysUserPasswordHistoryServiceImpl(passwordHistoryRepository);
    }

    @Nested
    @DisplayName("insertPasswordHistory")
    class InsertTests
    {
        @Test
        @DisplayName("insertPasswordHistory → 透传 mapper")
        void insertPasswordHistory_passesThrough()
        {
            when(passwordHistoryRepository.save(any(SysUserPasswordHistory.class))).thenAnswer(inv -> inv.getArgument(0));
            int rows = passwordHistoryService.insertPasswordHistory(1L, "$2a$10$hashedpassword");
            assertEquals(1, rows);
            verify(passwordHistoryRepository).save(any(SysUserPasswordHistory.class));
        }
    }

    @Nested
    @DisplayName("selectRecentPasswords")
    class SelectTests
    {
        @Test
        @DisplayName("limit > 0 → 透传 mapper")
        void limitPositive_passesThrough()
        {
            when(passwordHistoryRepository.findRecentPasswords(eq(1L), eq(org.springframework.data.domain.PageRequest.of(0, 3)))).thenReturn(List.of("pwd1", "pwd2"));
            List<String> result = passwordHistoryService.selectRecentPasswords(1L, 3);
            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("limit = 0 → 直接返回空列表，不查 mapper")
        void limitZero_returnsEmpty()
        {
            List<String> result = passwordHistoryService.selectRecentPasswords(1L, 0);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("limit < 0 → 直接返回空列表，不查 mapper")
        void limitNegative_returnsEmpty()
        {
            List<String> result = passwordHistoryService.selectRecentPasswords(1L, -1);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("matchesRecentHistory")
    class MatchTests
    {
        @Test
        @DisplayName("historyCount <= 0 → 直接返回 false")
        void historyCountZero_returnsFalse()
        {
            assertFalse(passwordHistoryService.matchesRecentHistory("rawPassword", 1L, 0));
        }

        @Test
        @DisplayName("rawPassword 为 null → 直接返回 false")
        void nullPassword_returnsFalse()
        {
            assertFalse(passwordHistoryService.matchesRecentHistory(null, 1L, 3));
        }

        @Test
        @DisplayName("rawPassword 为空字符串 → 直接返回 false")
        void emptyPassword_returnsFalse()
        {
            assertFalse(passwordHistoryService.matchesRecentHistory("", 1L, 3));
        }

        @Test
        @DisplayName("历史记录中有匹配的 SM3 密文 → 返回 true")
        void matchingPassword_returnsTrue()
        {
            when(passwordHistoryRepository.findRecentPasswords(eq(1L), eq(org.springframework.data.domain.PageRequest.of(0, 3))))
                    .thenReturn(List.of("$2a$10$hashedpassword"));

            try (MockedStatic<SecurityUtils> security = org.mockito.Mockito.mockStatic(SecurityUtils.class))
            {
                security.when(() -> SecurityUtils.matchesPassword("rawPassword", "$2a$10$hashedpassword"))
                        .thenReturn(true);
                boolean result = passwordHistoryService.matchesRecentHistory("rawPassword", 1L, 3);
                assertTrue(result);
            }
        }

        @Test
        @DisplayName("历史记录全部不匹配 → 返回 false")
        void noMatch_returnsFalse()
        {
            when(passwordHistoryRepository.findRecentPasswords(eq(1L), eq(org.springframework.data.domain.PageRequest.of(0, 3))))
                    .thenReturn(List.of("$2a$10$oldhash1", "$2a$10$oldhash2"));

            try (MockedStatic<SecurityUtils> security = org.mockito.Mockito.mockStatic(SecurityUtils.class))
            {
                security.when(() -> SecurityUtils.matchesPassword(eq("rawPassword"), any()))
                        .thenReturn(false);
                boolean result = passwordHistoryService.matchesRecentHistory("rawPassword", 1L, 3);
                assertFalse(result);
            }
        }

        @Test
        @DisplayName("遇到异常密文（抛 IllegalArgumentException）→ 跳过继续匹配")
        void nonBcryptPassword_skipsAndContinues()
        {
            when(passwordHistoryRepository.findRecentPasswords(eq(1L), eq(org.springframework.data.domain.PageRequest.of(0, 3))))
                    .thenReturn(List.of("plaintext_old", "$2a$10$validhash"));

            try (MockedStatic<SecurityUtils> security = org.mockito.Mockito.mockStatic(SecurityUtils.class))
            {
                security.when(() -> SecurityUtils.matchesPassword("rawPassword", "plaintext_old"))
                        .thenThrow(new IllegalArgumentException("Illegal argument"));
                security.when(() -> SecurityUtils.matchesPassword("rawPassword", "$2a$10$validhash"))
                        .thenReturn(true);
                boolean result = passwordHistoryService.matchesRecentHistory("rawPassword", 1L, 3);
                assertTrue(result);
            }
        }
    }

    @Nested
    @DisplayName("cleanupHistory")
    class CleanupTests
    {
        @Test
        @DisplayName("keepCount > 0 → 透传 mapper")
        void keepCountPositive_passesThrough()
        {
            when(passwordHistoryRepository.findRecentIds(eq(1L), any())).thenReturn(java.util.List.of(101L, 102L));
            when(passwordHistoryRepository.deleteByUserIdAndIdNotIn(eq(1L), any())).thenReturn(2);
            int deleted = passwordHistoryService.cleanupHistory(1L, 5);
            assertEquals(2, deleted);
        }

        @Test
        @DisplayName("keepCount < 0 → 钳制为 0 后调用 mapper")
        void keepCountNegative_clampsToZero()
        {
            int deleted = passwordHistoryService.cleanupHistory(1L, -3);
            assertEquals(0, deleted);
            verify(passwordHistoryRepository, never()).deleteByUserIdAndIdNotIn(any(), any());
        }
    }
}
