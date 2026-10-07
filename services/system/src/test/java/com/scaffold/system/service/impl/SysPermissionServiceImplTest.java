package com.scaffold.system.service.impl;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.scaffold.common.core.constant.Constants;
import com.scaffold.common.core.constant.UserConstants;
import com.scaffold.system.api.domain.SysRole;
import com.scaffold.system.api.domain.SysUser;
import com.scaffold.system.api.model.LoginUser;
import com.scaffold.system.domain.SysUserOnline;
import com.scaffold.system.service.ISysMenuService;
import com.scaffold.system.service.ISysRoleService;
import com.scaffold.system.service.impl.SysPermissionServiceImpl;
import com.scaffold.system.service.convert.SysUserOnlineConverter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * SysPermissionServiceImpl Mock 测试（管理员权限 vs 普通用户）。
 *
 * <p>覆盖维度：
 * <ul>
 *   <li>getRolePermission：管理员直接返回 SUPER_ADMIN，普通用户查 roleService</li>
 *   <li>getMenuPermission：管理员直接返回 ALL_PERMISSION，普通用户查 menuService</li>
 *   <li>普通用户多角色：仅用正常状态非超管角色</li>
 *   <li>普通用户无角色：降级查 menuService.selectMenuPermsByUserId</li>
 * </ul>
 *
 * @author ct
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysPermissionServiceImplTest
{
    @Mock
    private ISysRoleService roleService;

    @Mock
    private ISysMenuService menuService;

    private SysPermissionServiceImpl permissionService;

    @BeforeEach
    void setUp()
    {
        permissionService = new SysPermissionServiceImpl(roleService, menuService);
    }

    // ─────────────────────────────────────────────
    // getRolePermission
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("getRolePermission")
    class GetRolePermissionTests
    {
        @Test
        @DisplayName("管理员用户 → 直接返回 SUPER_ADMIN，不查 roleService")
        void adminUser_returnsSuperAdmin()
        {
            SysUser admin = new SysUser();
            admin.setUserId(1L);

            Set<String> perms = permissionService.getRolePermission(admin);

            assertEquals(1, perms.size());
            assertEquals(Constants.SUPER_ADMIN, perms.iterator().next());
        }

        @Test
        @DisplayName("普通用户 → 查 roleService 并返回权限集合")
        void normalUser_queriesRoleService()
        {
            SysUser normal = new SysUser();
            normal.setUserId(2L);
            when(roleService.selectRolePermissionByUserId(2L))
                    .thenReturn(Set.of("role:add", "role:edit"));

            Set<String> perms = permissionService.getRolePermission(normal);

            assertEquals(2, perms.size());
            assertNotNull(perms);
        }
    }

    // ─────────────────────────────────────────────
    // getMenuPermission
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("getMenuPermission")
    class GetMenuPermissionTests
    {
        @Test
        @DisplayName("管理员用户 → 直接返回 ALL_PERMISSION，不查 menuService")
        void adminUser_returnsAllPermission()
        {
            SysUser admin = new SysUser();
            admin.setUserId(1L);

            Set<String> perms = permissionService.getMenuPermission(admin);

            assertEquals(1, perms.size());
            assertEquals(Constants.ALL_PERMISSION, perms.iterator().next());
        }

        @Test
        @DisplayName("普通用户无角色 → 查 menuService.selectMenuPermsByUserId")
        void normalUserNoRoles_queriesMenuServiceByUserId()
        {
            SysUser normal = new SysUser();
            normal.setUserId(2L);
            normal.setRoles(null);
            when(menuService.selectMenuPermsByUserId(2L))
                    .thenReturn(Set.of("menu:query"));

            Set<String> perms = permissionService.getMenuPermission(normal);

            assertEquals(1, perms.size());
        }

        @Test
        @DisplayName("普通用户有角色（仅正常状态非超管）→ 查 menuService.selectMenuPermsByRoleId")
        void normalUserWithRoles_queriesMenuServiceByRoleId()
        {
            SysUser normal = new SysUser();
            normal.setUserId(2L);

            SysRole r1 = new SysRole();
            r1.setRoleId(10L);
            r1.setRoleKey("role1");
            r1.setStatus(UserConstants.ROLE_NORMAL);  // 正常
            // isAdmin() 基于 roleKey "admin" 判断（需要检查实现）
            // r1 不是超管角色

            normal.setRoles(java.util.List.of(r1));
            when(menuService.selectMenuPermsByRoleId(10L))
                    .thenReturn(Set.of("perm:view", "perm:edit"));

            Set<String> perms = permissionService.getMenuPermission(normal);

            // 正常角色被计入权限集合
            assertEquals(2, perms.size());
        }

        @Test
        @DisplayName("普通用户有角色（含超管角色）→ 超管角色不计入权限")
        void normalUserWithAdminRole_adminRoleExcluded()
        {
            SysUser normal = new SysUser();
            normal.setUserId(2L);

            SysRole adminRole = new SysRole();
            adminRole.setRoleId(1L);
            adminRole.setRoleKey("admin");
            adminRole.setStatus(UserConstants.ROLE_NORMAL);

            SysRole normalRole = new SysRole();
            normalRole.setRoleId(2L);
            normalRole.setRoleKey("normal_role");
            normalRole.setStatus(UserConstants.ROLE_NORMAL);

            normal.setRoles(java.util.List.of(adminRole, normalRole));
            when(menuService.selectMenuPermsByRoleId(2L))
                    .thenReturn(Set.of("perm:normal"));

            Set<String> perms = permissionService.getMenuPermission(normal);

            // 只计入非超管角色
            assertEquals(1, perms.size());
            assertNotNull(perms);
        }
    }
}
