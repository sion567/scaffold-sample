package com.scaffold.system.service.impl;

import java.util.List;

import jakarta.validation.Validator;
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
import com.scaffold.common.core.exception.ServiceException;
import com.scaffold.system.api.domain.SysRole;
import com.scaffold.system.api.domain.SysUser;
import com.scaffold.system.domain.SysPost;
import com.scaffold.system.repository.SysPostRepository;
import com.scaffold.system.repository.SysRoleRepository;
import com.scaffold.system.repository.SysUserRepository;
import com.scaffold.system.repository.SysUserPostRepository;
import com.scaffold.system.repository.SysUserRoleRepository;
import com.scaffold.system.service.ISysConfigService;
import com.scaffold.system.service.ISysDeptService;
import com.scaffold.system.service.ISysUserPasswordHistoryService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SysUserServiceImpl Mock 测试（边界防御 + 状态校验 + 唯一性检查）。
 *
 * <p>覆盖维度：
 * <ul>
 *   <li>边界防御：checkUserAllowed 拦截 admin、importUser 空列表校验</li>
 *   <li>状态与缓存：checkUserAllowed 超级管理员禁止操作</li>
 *   <li>唯一性：checkUserNameUnique/PhoneUnique/EmailUnique 与 DB 冲突时返回 NOT_UNIQUE</li>
 *   <li>微服务防腐：resetUserPwdWithHistory 密码历史记录链路</li>
 * </ul>
 *
 * @author ct
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysUserServiceImplTest
{
    @Mock
    private SysUserRepository userRepository;

    @Mock
    private SysRoleRepository roleRepository;

    @Mock
    private SysPostRepository postRepository;

    @Mock
    private SysUserRoleRepository userRoleRepository;

    @Mock
    private SysUserPostRepository userPostRepository;

    @Mock
    private ISysConfigService configService;

    @Mock
    private ISysDeptService deptService;

    @Mock
    private ISysUserPasswordHistoryService passwordHistoryService;

    @Mock
    private Validator validator;

    private SysUserServiceImpl userService;

    @BeforeEach
    void setUp()
    {
        userService = new SysUserServiceImpl(
                userRepository, roleRepository, postRepository,
                userRoleRepository, userPostRepository,
                configService, deptService, passwordHistoryService, validator);
    }

    // ─────────────────────────────────────────────
    // checkUserAllowed — 状态与缓存
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("checkUserAllowed 超级管理员禁止操作")
    class CheckUserAllowedTests
    {
        @Test
        @DisplayName("超级管理员 → ServiceException（isAdmin 基于 userId=1）")
        void adminUser_throws()
        {
            SysUser admin = user(1L, "admin");

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> userService.checkUserAllowed(admin));
            assertTrue(ex.getMessage().contains("不允许操作超级管理员"));
        }

        @Test
        @DisplayName("普通用户 → 不抛异常")
        void normalUser_ok()
        {
            SysUser normal = user(2L, "zhangsan");
            // 不抛异常即为通过
            userService.checkUserAllowed(normal);
        }

        @Test
        @DisplayName("userId 为 null → 不抛异常（新建用户不涉及权限校验）")
        void nullUserId_ok()
        {
            SysUser newUser = user(null, "newuser");
            userService.checkUserAllowed(newUser);
        }
    }

    // ─────────────────────────────────────────────
    // 唯一性检查 — 边界防御
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("唯一性检查")
    class UniqueCheckTests
    {
        @Test
        @DisplayName("用户名唯一：数据库无冲突 → UNIQUE")
        void userNameUnique_noConflict_returnsUnique()
        {
            when(userRepository.findByUserNameAndDelFlag("zhangsan", "0")).thenReturn(java.util.Optional.empty());

            boolean result = userService.checkUserNameUnique(user(null, "zhangsan"));

            assertEquals(UserConstants.UNIQUE, result);
        }

        @Test
        @DisplayName("用户名重复（不同 userId）→ NOT_UNIQUE")
        void userNameUnique_conflict_returnsNotUnique()
        {
            when(userRepository.findByUserNameAndDelFlag("zhangsan", "0"))
                    .thenReturn(java.util.Optional.of(user(2L, "zhangsan")));

            // 当前用户 userId=1L，与数据库 2L 不同 → 冲突
            boolean result = userService.checkUserNameUnique(user(1L, "zhangsan"));

            assertEquals(UserConstants.NOT_UNIQUE, result);
        }

        @Test
        @DisplayName("用户名重复（同 userId）→ UNIQUE（自己不算冲突）")
        void userNameUnique_sameUserId_returnsUnique()
        {
            when(userRepository.findByUserNameAndDelFlag("zhangsan", "0"))
                    .thenReturn(java.util.Optional.of(user(2L, "zhangsan")));

            // 当前用户 userId=2L，与数据库一致 → 不是冲突
            boolean result = userService.checkUserNameUnique(user(2L, "zhangsan"));

            assertEquals(UserConstants.UNIQUE, result);
        }

        @Test
        @DisplayName("手机号唯一：同 userId → UNIQUE")
        void phoneUnique_sameUserId()
        {
            SysUser u = user(2L, "zhangsan");
            u.setPhonenumber("13800138000");
            when(userRepository.findByPhonenumberAndDelFlag("13800138000", "0")).thenReturn(java.util.Optional.of(u));

            boolean result = userService.checkPhoneUnique(u);
            assertEquals(UserConstants.UNIQUE, result);
        }

        @Test
        @DisplayName("手机号重复（不同 userId）→ NOT_UNIQUE")
        void phoneUnique_conflict_returnsNotUnique()
        {
            SysUser conflict = user(3L, "lisi");
            conflict.setPhonenumber("13800138000");
            SysUser current = user(2L, "zhangsan");
            current.setPhonenumber("13800138000");

            when(userRepository.findByPhonenumberAndDelFlag("13800138000", "0")).thenReturn(java.util.Optional.of(conflict));

            boolean result = userService.checkPhoneUnique(current);
            assertEquals(UserConstants.NOT_UNIQUE, result);
        }

        @Test
        @DisplayName("邮箱唯一：同 userId → UNIQUE")
        void emailUnique_sameUserId()
        {
            SysUser u = user(2L, "zhangsan");
            u.setEmail("a@b.com");
            when(userRepository.findByEmailAndDelFlag("a@b.com", "0")).thenReturn(java.util.Optional.of(u));

            boolean result = userService.checkEmailUnique(u);
            assertEquals(UserConstants.UNIQUE, result);
        }

        @Test
        @DisplayName("邮箱重复（不同 userId）→ NOT_UNIQUE")
        void emailUnique_conflict_returnsNotUnique()
        {
            SysUser conflict = user(3L, "lisi");
            conflict.setEmail("a@b.com");
            SysUser current = user(2L, "zhangsan");
            current.setEmail("a@b.com");

            when(userRepository.findByEmailAndDelFlag("a@b.com", "0")).thenReturn(java.util.Optional.of(conflict));

            boolean result = userService.checkEmailUnique(current);
            assertEquals(UserConstants.NOT_UNIQUE, result);
        }
    }

    // ─────────────────────────────────────────────
    // importUser — 边界防御 + 批量逻辑
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("importUser 入参校验与批量导入")
    class ImportUserTests
    {
        @Test
        @DisplayName("userList 为 null → ServiceException")
        void importUser_nullList_throws()
        {
            ServiceException ex = assertThrows(ServiceException.class,
                    () -> userService.importUser(null, false, "admin"));
            assertTrue(ex.getMessage().contains("导入用户数据不能为空"));
        }

        @Test
        @DisplayName("userList 为空列表 → ServiceException")
        void importUser_emptyList_throws()
        {
            ServiceException ex = assertThrows(ServiceException.class,
                    () -> userService.importUser(List.of(), false, "admin"));
            assertTrue(ex.getMessage().contains("导入用户数据不能为空"));
        }

        @Test
        @DisplayName("新用户：查不到 → 插入成功，返回成功信息")
        void importUser_newUser_inserts()
        {
            SysUser newUser = user(null, "newone");
            newUser.setDeptId(100L);

            when(userRepository.findByUserNameAndDelFlag("newone", "0")).thenReturn(java.util.Optional.empty());
            when(configService.selectConfigByKey("sys.user.initPassword")).thenReturn("Init@123");
            

            String result = userService.importUser(List.of(newUser), false, "admin");

            assertTrue(result.contains("导入成功"));
            verify(userRepository).save(any(SysUser.class));
        }

        @Test
        @DisplayName("已存在用户 + isUpdateSupport=false → 记录失败，最终抛 ServiceException")
        void importUser_exists_noUpdateSupport_throws()
        {
            SysUser existing = user(5L, "existing");
            existing.setDeptId(100L);
            when(userRepository.findByUserNameAndDelFlag("existing", "0")).thenReturn(java.util.Optional.of(existing));

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> userService.importUser(List.of(existing), false, "admin"));

            assertTrue(ex.getMessage().contains("已存在"));
            verify(userRepository, never()).save(any(SysUser.class));
            verify(userRepository, never()).save(any(SysUser.class));
        }

        @Test
        @DisplayName("已存在用户 + isUpdateSupport=true + 超管用户 → ServiceException（isAdmin 基于 userId=1）")
        void importUser_exists_updateSupport_adminUser_throws()
        {
            // userId=1L → isAdmin()=true → checkUserAllowed 抛异常（不触发 checkUserDataScope AOP 问题）
            SysUser existing = user(1L, "adminuser");
            existing.setDeptId(100L);
            SysUser updateInput = user(1L, "adminuser");
            updateInput.setDeptId(100L);
            updateInput.setNickName("新昵称");

            when(userRepository.findByUserNameAndDelFlag("adminuser", "0")).thenReturn(java.util.Optional.of(existing));

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> userService.importUser(List.of(updateInput), true, "admin"));

            assertTrue(ex.getMessage().contains("不允许操作超级管理员"));
            verify(userRepository, never()).save(any(SysUser.class));
        }

        @Test
        @DisplayName("部分成功：第 1 条入库、第 2 条失败 → 整体抛 ServiceException，明细含失败账号")
        void importUser_partialFailure_throwsWithDetail()
        {
            // 第 1 条：新用户，插入成功
            SysUser successUser = user(null, "success");
            successUser.setDeptId(100L);
            // 第 2 条：查询时报异常
            SysUser failUser = user(null, "fail");
            failUser.setDeptId(100L);

            when(userRepository.findByUserNameAndDelFlag("success", "0")).thenReturn(java.util.Optional.empty());
            when(userRepository.findByUserNameAndDelFlag("fail", "0")).thenThrow(new RuntimeException("DB error"));
            when(configService.selectConfigByKey("sys.user.initPassword")).thenReturn("Init@123");

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> userService.importUser(List.of(successUser, failUser), false, "admin"));

            // 异常明细只含失败行；第 1 条成功信息体现在已执行入库
            assertTrue(ex.getMessage().contains("导入失败"));
            assertTrue(ex.getMessage().contains("fail"));
            assertTrue(ex.getMessage().contains("DB error"));
            verify(userRepository).save(successUser);
        }
    }

    // ─────────────────────────────────────────────
    // resetUserPwdWithHistory — 密码历史链路
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("resetUserPwdWithHistory 密码历史记录")
    class ResetPwdWithHistoryTests
    {
        @Test
        @DisplayName("重置成功 → 写密码历史 + 清理超限历史")
        void resetUserPwdWithHistory_success_callsHistory()
        {
            when(userRepository.resetPassword(2L, "Sm3Hash")).thenReturn(1);
            when(passwordHistoryService.insertPasswordHistory(eq(2L), eq("Sm3Hash"))).thenReturn(1);
            when(passwordHistoryService.cleanupHistory(eq(2L), eq(5))).thenReturn(1);

            int rows = userService.resetUserPwdWithHistory(2L, "Sm3Hash", 5);

            assertEquals(1, rows);
            verify(passwordHistoryService).insertPasswordHistory(2L, "Sm3Hash");
            verify(passwordHistoryService).cleanupHistory(2L, 5);
        }

        @Test
        @DisplayName("重置失败（0 行）→ 不写历史")
        void resetUserPwdWithHistory_fail_noHistory()
        {
            when(userRepository.resetPassword(2L, "Sm3Hash")).thenReturn(0);

            int rows = userService.resetUserPwdWithHistory(2L, "Sm3Hash", 5);

            assertEquals(0, rows);
            verify(passwordHistoryService, never()).insertPasswordHistory(anyLong(), anyString());
        }

        @Test
        @DisplayName("historyCount ≤ 0 → 跳过清理")
        void resetUserPwdWithHistory_skipCleanup()
        {
            when(userRepository.resetPassword(2L, "Sm3Hash")).thenReturn(1);
            when(passwordHistoryService.insertPasswordHistory(eq(2L), eq("Sm3Hash"))).thenReturn(1);

            userService.resetUserPwdWithHistory(2L, "Sm3Hash", 0);

            verify(passwordHistoryService).insertPasswordHistory(2L, "Sm3Hash");
            verify(passwordHistoryService, never()).cleanupHistory(anyLong(), anyInt());
        }
    }

    // ─────────────────────────────────────────────
    // selectUserRoleGroup / selectUserPostGroup
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("selectUserRoleGroup / selectUserPostGroup")
    class GroupQueryTests
    {
        @Test
        @DisplayName("角色组：有角色 → 逗号分隔")
        void roleGroup_withRoles_joinsWithComma()
        {
            SysRole r1 = new SysRole();
            r1.setRoleName("管理员");
            SysRole r2 = new SysRole();
            r2.setRoleName("普通用户");
            when(userRepository.selectRolesByUserName("zhangsan"))
                    .thenReturn(List.of(r1, r2));

            String group = userService.selectUserRoleGroup("zhangsan");

            assertEquals("管理员,普通用户", group);
        }

        @Test
        @DisplayName("角色组：无角色 → 空字符串")
        void roleGroup_noRoles_returnsEmpty()
        {
            when(userRepository.selectRolesByUserName("zhangsan")).thenReturn(List.of());

            String group = userService.selectUserRoleGroup("zhangsan");

            assertEquals("", group);
        }

        @Test
        @DisplayName("岗位组：有岗位 → 逗号分隔")
        void postGroup_withPosts_joinsWithComma()
        {
            SysPost p1 = new SysPost();
            p1.setPostName("产品经理");
            SysPost p2 = new SysPost();
            p2.setPostName("设计师");
            when(postRepository.selectPostsByUserName("zhangsan"))
                    .thenReturn(List.of(p1, p2));

            String group = userService.selectUserPostGroup("zhangsan");

            assertEquals("产品经理,设计师", group);
        }

        @Test
        @DisplayName("岗位组：无岗位 → 空字符串")
        void postGroup_noPosts_returnsEmpty()
        {
            when(postRepository.selectPostsByUserName("zhangsan")).thenReturn(List.of());

            String group = userService.selectUserPostGroup("zhangsan");

            assertEquals("", group);
        }
    }

    // ─────────────────────────────────────────────
    // updateLoginInfo — 登录留痕
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("updateLoginInfo")
    class UpdateLoginInfoTests
    {
        @Test
        @DisplayName("登录更新：走专用 update 语句，不经实体乐观锁")
        void updateLoginInfo_nullVersion_notInMapperCall()
        {
            when(userRepository.updateLoginInfo(any(), any(), any())).thenReturn(1);

            SysUser u = user(1L, "admin");
            u.setVersion(99); // 专用 update 语句不含 version 列，实体版本保持不变
            boolean updated = userService.updateLoginInfo(u);

            assertTrue(updated);
            verify(userRepository).updateLoginInfo(eq(1L), any(), any());
        }
    }

    // ─────────────────────────────────────────────
    // 辅助方法
    // ─────────────────────────────────────────────

    private SysUser user(Long userId, String userName)
    {
        SysUser u = new SysUser();
        u.setUserId(userId);
        u.setUserName(userName);
        u.setNickName(userName);
        u.setDeptId(100L);
        u.setStatus("0");
        u.setDelFlag("0");
        return u;
    }
}
