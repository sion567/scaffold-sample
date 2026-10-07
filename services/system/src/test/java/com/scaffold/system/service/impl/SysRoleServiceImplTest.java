package com.scaffold.system.service.impl;

import java.util.List;
import java.util.Set;

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
import com.scaffold.common.security.utils.SecurityUtils;
import com.scaffold.system.api.domain.SysRole;
import com.scaffold.system.repository.SysRoleDeptRepository;
import com.scaffold.system.repository.SysRoleRepository;
import com.scaffold.system.repository.SysRoleMenuRepository;
import com.scaffold.system.repository.SysUserRoleRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SysRoleServiceImpl Mock 测试（边界防御 + 角色权限校验 + 删除前检查）。
 *
 * <p>覆盖维度：
 * <ul>
 *   <li>边界防御：checkRoleAllowed 超级管理员禁止操作</li>
 *   <li>状态与缓存：checkRoleDataScope 非管理员权限校验</li>
 *   <li>删除保护：deleteRoleByIds 角色下有用户时禁止删除</li>
 *   <li>唯一性：checkRoleNameUnique / checkRoleKeyUnique</li>
 * </ul>
 *
 * @author ct
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysRoleServiceImplTest
{
    @Mock
    private SysRoleRepository roleRepository;

    @Mock
    private SysRoleMenuRepository roleMenuRepository;

    @Mock
    private SysUserRoleRepository userRoleRepository;

    @Mock
    private SysRoleDeptRepository roleDeptRepository;

    private SysRoleServiceImpl roleService;

    @BeforeEach
    void setUp()
    {
        roleService = new SysRoleServiceImpl(roleRepository, roleMenuRepository, userRoleRepository, roleDeptRepository);
    }

    // ─────────────────────────────────────────────
    // checkRoleAllowed — 状态与缓存
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("checkRoleAllowed 超级管理员角色禁止操作")
    class CheckRoleAllowedTests
    {
        @Test
        @DisplayName("超级管理员角色 → ServiceException（roleId=1L 即为管理员）")
        void adminRole_throws()
        {
            SysRole adminRole = role(1L, "超级管理员", "admin");

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> roleService.checkRoleAllowed(adminRole));
            assertTrue(ex.getMessage().contains("不允许操作超级管理员"));
        }

        @Test
        @DisplayName("普通角色 → 不抛异常")
        void normalRole_ok()
        {
            SysRole normalRole = role(2L, "普通角色", "common");
            roleService.checkRoleAllowed(normalRole);
        }

        @Test
        @DisplayName("roleId 为 null → 不抛异常（新建角色）")
        void nullRoleId_ok()
        {
            SysRole newRole = role(null, "新角色", "new");
            roleService.checkRoleAllowed(newRole);
        }
    }

    // ─────────────────────────────────────────────
    // checkRoleDataScope — 状态与缓存
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("checkRoleDataScope 权限校验")
    class CheckDataScopeTests
    {
        @Test
        @DisplayName("管理员用户 → 直接通过，不查数据范围（不触发 AOP proxy）")
        void adminUser_skipsDataScopeCheck()
        {
            try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class))
            {
                security.when(SecurityUtils::isAdmin).thenReturn(true);

                roleService.checkRoleDataScope(1L);

                verify(roleRepository, never()).list(any());
            }
        }

        // 注：非管理员数据权限校验（checkRoleDataScope 调用 SpringUtils.getAopProxy）依赖 Spring 容器，
        //     单元测试覆盖不了，由集成测试负责。
        //     此处仅验证：管理员用户跳过数据权限检查（adminUser_skipsDataScopeCheck）。
    }

    // ─────────────────────────────────────────────
    // 唯一性检查
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("唯一性检查")
    class UniqueCheckTests
    {
        @Test
        @DisplayName("角色名唯一：无冲突 → UNIQUE")
        void roleNameUnique_noConflict()
        {
            when(roleRepository.findByRoleName("测试角色")).thenReturn(java.util.Optional.empty());

            boolean result = roleService.checkRoleNameUnique(role(null, "测试角色", "test"));

            assertEquals(UserConstants.UNIQUE, result);
        }

        @Test
        @DisplayName("角色名冲突（同 roleId 除外）→ NOT_UNIQUE")
        void roleNameUnique_conflict()
        {
            when(roleRepository.findByRoleName("测试角色"))
                    .thenReturn(java.util.Optional.of(role(2L, "测试角色", "test")));

            // 当前 roleId=1L，与 DB 2L 不同 → 冲突
            boolean result = roleService.checkRoleNameUnique(role(1L, "测试角色", "test"));

            assertEquals(UserConstants.NOT_UNIQUE, result);
        }

        @Test
        @DisplayName("角色名冲突（roleId 相同）→ UNIQUE（自己不算冲突）")
        void roleNameUnique_sameRoleId()
        {
            when(roleRepository.findByRoleName("测试角色"))
                    .thenReturn(java.util.Optional.of(role(2L, "测试角色", "test")));

            boolean result = roleService.checkRoleNameUnique(role(2L, "测试角色", "test"));

            assertEquals(UserConstants.UNIQUE, result);
        }

        @Test
        @DisplayName("角色标识唯一：无冲突 → UNIQUE")
        void roleKeyUnique_noConflict()
        {
            when(roleRepository.findByRoleKey("test_role")).thenReturn(java.util.Optional.empty());

            boolean result = roleService.checkRoleKeyUnique(role(null, "测试角色", "test_role"));

            assertEquals(UserConstants.UNIQUE, result);
        }

        @Test
        @DisplayName("角色标识冲突（不同 roleId）→ NOT_UNIQUE")
        void roleKeyUnique_conflict()
        {
            when(roleRepository.findByRoleKey("test_role"))
                    .thenReturn(java.util.Optional.of(role(2L, "测试角色", "test_role")));

            boolean result = roleService.checkRoleKeyUnique(role(1L, "测试角色", "test_role"));

            assertEquals(UserConstants.NOT_UNIQUE, result);
        }
    }

    // ─────────────────────────────────────────────
    // countUserRoleByRoleId + deleteRoleByIds
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("countUserRoleByRoleId / deleteRoleByIds 删除保护")
    class DeleteRoleTests
    {
        @Test
        @DisplayName("countUserRoleByRoleId：角色下有用户 → 返回 > 0")
        void countUserRoleByRoleId_hasUsers()
        {
            when(userRoleRepository.countByRoleId(1L)).thenReturn(5L);

            int count = roleService.countUserRoleByRoleId(1L);

            assertEquals(5, count);
        }

        @Test
        @DisplayName("deleteRoleByIds：角色下有用户 → ServiceException（roleId=2L 非管理员，命中 countUserRoleByRoleId > 0）")
        void deleteRoleByIds_withUsers_throws()
        {
            try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class))
            {
                security.when(SecurityUtils::isAdmin).thenReturn(true);
                // roleId=2L → role.isAdmin()=false → checkRoleAllowed 通过
                // SecurityUtils.isAdmin()=true → checkRoleDataScope 跳过
                // → 命中 countByRoleId(2L)=3 → 抛异常
                when(roleRepository.findById(2L)).thenReturn(java.util.Optional.of(role(2L, "有用户角色", "has_users")));
                when(userRoleRepository.countByRoleId(2L)).thenReturn(3L);

                ServiceException ex = assertThrows(ServiceException.class,
                        () -> roleService.deleteRoleByIds(new Long[] { 2L }));

                assertTrue(ex.getMessage().contains("已分配"));
                verify(roleRepository, never()).softDeleteByIds(any());
            }
        }

        @Test
        @DisplayName("deleteRoleByIds：角色下无用户 → 正常删除（走 deleteRoleByIds，非 deleteRoleById）")
        void deleteRoleByIds_noUsers_deletes()
        {
            try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class))
            {
                security.when(SecurityUtils::isAdmin).thenReturn(true);
                when(roleRepository.findById(2L)).thenReturn(java.util.Optional.of(role(2L, "无用户角色", "no_users")));
                when(userRoleRepository.countByRoleId(2L)).thenReturn(0L);
                

                int rows = roleService.deleteRoleByIds(new Long[]{2L});

                assertEquals(1, rows);
                verify(roleMenuRepository).deleteByRoleIds(java.util.Arrays.asList(2L));
                verify(roleDeptRepository).deleteByRoleIds(java.util.Arrays.asList(2L));
                verify(roleRepository).softDeleteByIds(java.util.Arrays.asList(2L));
            }
        }

        @Test
        @DisplayName("deleteRoleByIds：超管角色 → 第一条即抛异常（checkRoleAllowed 先检测 roleId=1L）")
        void deleteRoleByIds_adminRole_throwsFirst()
        {
            try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class))
            {
                security.when(SecurityUtils::isAdmin).thenReturn(true);
                // checkRoleAllowed(new SysRole(1L)) → isAdmin()=true → 抛异常在 countUserRoleByRoleId 之前
                assertThrows(ServiceException.class,
                        () -> roleService.deleteRoleByIds(new Long[] { 1L, 2L }));

                // 2L 未被检查（第一条 checkRoleAllowed 已抛异常）
                verify(userRoleRepository, never()).countByRoleId(2L);
            }
        }
    }

    // ─────────────────────────────────────────────
    // selectRolePermissionByUserId — 权限解析
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("selectRolePermissionByUserId 权限解析")
    class PermissionTests
    {
        @Test
        @DisplayName("用户有角色 → 返回去重后的权限集合")
        void withRoles_returnsDedupedSet()
        {
            SysRole r1 = role(1L, "角色1", "role1");
            r1.setRoleKey("perm:add,perm:edit");
            SysRole r2 = role(2L, "角色2", "role2");
            r2.setRoleKey("perm:edit,perm:delete");

            when(roleRepository.selectRolePermissionByUserId(1L))
                    .thenReturn(List.of(r1, r2));

            Set<String> perms = roleService.selectRolePermissionByUserId(1L);

            assertTrue(perms.contains("perm:add"));
            assertTrue(perms.contains("perm:edit"));
            assertTrue(perms.contains("perm:delete"));
            // edit 重复但集合去重
            assertEquals(3, perms.size());
        }

        @Test
        @DisplayName("用户无角色 → 返回空集合，不抛异常")
        void noRoles_returnsEmptySet()
        {
            when(roleRepository.selectRolePermissionByUserId(1L)).thenReturn(List.of());

            Set<String> perms = roleService.selectRolePermissionByUserId(1L);

            assertNotNull(perms);
            assertTrue(perms.isEmpty());
        }
    }

    // ─────────────────────────────────────────────
    // 辅助方法
    // ─────────────────────────────────────────────

    private SysRole role(Long roleId, String roleName, String roleKey)
    {
        SysRole r = new SysRole();
        r.setRoleId(roleId);
        r.setRoleName(roleName);
        r.setRoleKey(roleKey);
        r.setRoleSort(1);
        r.setStatus("0");
        return r;
    }
}
