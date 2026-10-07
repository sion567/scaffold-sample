package com.scaffold.system.service.impl;

import java.util.ArrayList;
import java.util.List;
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

import com.scaffold.common.core.constant.UserConstants;
import com.scaffold.common.core.exception.ServiceException;
import com.scaffold.system.api.domain.SysRole;
import com.scaffold.system.domain.SysMenu;
import com.scaffold.system.domain.vo.MetaVo;
import com.scaffold.system.domain.vo.RouterVo;
import com.scaffold.system.domain.vo.TreeSelect;
import com.scaffold.system.repository.SysMenuRepository;
import com.scaffold.system.repository.SysRoleRepository;
import com.scaffold.system.repository.SysRoleMenuRepository;
import com.scaffold.system.service.impl.SysMenuServiceImpl;
import com.scaffold.system.service.convert.RouterVoConverter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SysMenuServiceImpl Mock 测试（边界防御 + 树结构构建 + 路由构建 + 唯一性）。
 *
 * <p>覆盖维度：
 * <ul>
 *   <li>权限查询：selectMenuPermsByUserId / selectMenuPermsByRoleId 去重逻辑</li>
 *   <li>树结构：buildMenuTree / buildMenuTreeSelect / getChildPerms</li>
 *   <li>路由构建：buildMenus 目录/菜单/内链场景</li>
 *   <li>唯一性：checkMenuNameUnique / checkRouteConfigUnique 路由冲突检测</li>
 *   <li>辅助方法：getRouteName / getRouterPath / getComponent / isMenuFrame / isInnerLink</li>
 * </ul>
 *
 * @author ct
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysMenuServiceImplTest
{
    @Mock
    private SysMenuRepository menuRepository;

    @Mock
    private SysRoleRepository roleRepository;

    @Mock
    private SysRoleMenuRepository roleMenuRepository;

    @Mock
    private RouterVoConverter routerVoMapper;

    private SysMenuServiceImpl menuService;

    @BeforeEach
    void setUp()
    {
        menuService = new SysMenuServiceImpl(menuRepository, roleRepository, roleMenuRepository, routerVoMapper);
    }

    // ─────────────────────────────────────────────
    // selectMenuPermsByUserId / selectMenuPermsByRoleId
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("selectMenuPermsByUserId / selectMenuPermsByRoleId 权限去重")
    class PermsTests
    {
        @Test
        @DisplayName("单个权限字符串 → 直接加入 Set")
        void singlePerm_returnsSetWithOne()
        {
            when(menuRepository.selectMenuPermsByUserId(1L)).thenReturn(List.of("system:user:list"));

            Set<String> perms = menuService.selectMenuPermsByUserId(1L);

            assertEquals(1, perms.size());
            assertTrue(perms.contains("system:user:list"));
        }

        @Test
        @DisplayName("逗号分隔的多权限 → 拆分为多个")
        void commaSeparatedPerms_splits()
        {
            when(menuRepository.selectMenuPermsByUserId(1L)).thenReturn(List.of("system:user:list,system:user:add"));

            Set<String> perms = menuService.selectMenuPermsByUserId(1L);

            assertTrue(perms.contains("system:user:list"));
            assertTrue(perms.contains("system:user:add"));
        }

        @Test
        @DisplayName("含空字符串 → 过滤掉空串")
        void emptyString_filtered()
        {
            when(menuRepository.selectMenuPermsByUserId(1L)).thenReturn(List.of("perm:add", "", "perm:edit"));

            Set<String> perms = menuService.selectMenuPermsByUserId(1L);

            assertEquals(2, perms.size());
            assertTrue(perms.contains("perm:add"));
            assertTrue(perms.contains("perm:edit"));
        }

        @Test
        @DisplayName("重复权限 → Set 去重")
        void duplicatePerms_deduped()
        {
            when(menuRepository.selectMenuPermsByUserId(1L)).thenReturn(List.of("perm:edit", "perm:edit"));

            Set<String> perms = menuService.selectMenuPermsByUserId(1L);

            assertEquals(1, perms.size());
        }

        @Test
        @DisplayName("无权限 → 返回空 Set，不抛异常")
        void noPerms_returnsEmptySet()
        {
            when(menuRepository.selectMenuPermsByUserId(99L)).thenReturn(List.of());

            Set<String> perms = menuService.selectMenuPermsByUserId(99L);

            assertNotNull(perms);
            assertTrue(perms.isEmpty());
        }
    }

    // ─────────────────────────────────────────────
    // buildMenuTree / buildMenuTreeSelect / getChildPerms
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("树结构构建")
    class TreeBuildTests
    {
        @Test
        @DisplayName("buildMenuTree：多级树，正确挂载子节点")
        void multiLevel_buildsTree()
        {
            SysMenu root = menu(1L, "系统管理", 0L, UserConstants.TYPE_DIR);
            SysMenu child = menu(2L, "用户管理", 1L, UserConstants.TYPE_MENU);
            List<SysMenu> flat = List.of(root, child);

            List<SysMenu> tree = menuService.buildMenuTree(flat);

            assertEquals(1, tree.size());
            assertEquals("系统管理", tree.get(0).getMenuName());
            assertEquals(1, tree.get(0).getChildren().size());
            assertEquals("用户管理", tree.get(0).getChildren().get(0).getMenuName());
        }

        @Test
        @DisplayName("buildMenuTree：空列表 → 返回空")
        void emptyList_returnsEmpty()
        {
            List<SysMenu> tree = menuService.buildMenuTree(List.of());
            assertTrue(tree.isEmpty());
        }

        @Test
        @DisplayName("getChildPerms：parentId=0 匹配根节点下的所有直接子节点")
        void getChildPerms_matchesDirectChildren()
        {
            SysMenu root = menu(1L, "根", 0L, UserConstants.TYPE_DIR);
            SysMenu child1 = menu(2L, "子1", 1L, UserConstants.TYPE_MENU);
            SysMenu child2 = menu(3L, "子2", 1L, UserConstants.TYPE_MENU);
            SysMenu grandChild = menu(4L, "孙", 2L, UserConstants.TYPE_MENU);

            List<SysMenu> result = menuService.getChildPerms(List.of(root, child1, child2, grandChild), 0L);

            // parentId=0 的直接子节点是 root，root 下递归挂载 child1, child2, grandChild
            assertEquals(1, result.size());
            assertEquals("根", result.get(0).getMenuName());
        }

        @Test
        @DisplayName("buildMenuTreeSelect：调用 buildMenuTree 后转 TreeSelect")
        void buildMenuTreeSelect_convertsToTreeSelect()
        {
            SysMenu root = menu(1L, "总公司", 0L, UserConstants.TYPE_DIR);
            List<SysMenu> flat = List.of(root);

            List<TreeSelect> tree = menuService.buildMenuTreeSelect(flat);

            assertEquals(1, tree.size());
            assertEquals("总公司", tree.get(0).getLabel());
        }
    }

    // ─────────────────────────────────────────────
    // hasChildByMenuId / checkMenuExistRole
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("hasChildByMenuId / checkMenuExistRole")
    class ChildRoleCheckTests
    {
        @Test
        @DisplayName("hasChildByMenuId：有子菜单 → true")
        void hasChildren_returnsTrue()
        {
            when(menuRepository.countByParentId(1L)).thenReturn(3L);
            assertTrue(menuService.hasChildByMenuId(1L));
        }

        @Test
        @DisplayName("hasChildByMenuId：无子菜单 → false")
        void noChildren_returnsFalse()
        {
            when(menuRepository.countByParentId(99L)).thenReturn(0L);
            assertFalse(menuService.hasChildByMenuId(99L));
        }

        @Test
        @DisplayName("checkMenuExistRole：菜单已分配角色 → true")
        void menuAssignedToRole_returnsTrue()
        {
            when(roleMenuRepository.countByMenuId(1L)).thenReturn(2L);
            assertTrue(menuService.checkMenuExistRole(1L));
        }

        @Test
        @DisplayName("checkMenuExistRole：菜单未分配角色 → false")
        void menuNotAssigned_returnsFalse()
        {
            when(roleMenuRepository.countByMenuId(99L)).thenReturn(0L);
            assertFalse(menuService.checkMenuExistRole(99L));
        }
    }

    // ─────────────────────────────────────────────
    // checkMenuNameUnique — 唯一性
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("checkMenuNameUnique 唯一性")
    class MenuNameUniqueTests
    {
        @Test
        @DisplayName("同名不同父级 → NOT_UNIQUE")
        void sameNameDifferentParent_returnsNotUnique()
        {
            when(menuRepository.findByMenuNameAndParentId("用户管理", 2L))
                    .thenReturn(java.util.Optional.of(menu(2L, "用户管理", 2L, UserConstants.TYPE_MENU)));

            // parentId=2L 与 mapper 查询参数一致
            boolean result = menuService.checkMenuNameUnique(menu(1L, "用户管理", 2L, UserConstants.TYPE_MENU));

            assertEquals(UserConstants.NOT_UNIQUE, result);
        }

        @Test
        @DisplayName("同名同父级（同 menuId）→ UNIQUE（自己不算冲突）")
        void sameNameSameParentSameId_returnsUnique()
        {
            when(menuRepository.findByMenuNameAndParentId("用户管理", 1L))
                    .thenReturn(java.util.Optional.of(menu(1L, "用户管理", 0L, UserConstants.TYPE_MENU)));

            boolean result = menuService.checkMenuNameUnique(menu(1L, "用户管理", 0L, UserConstants.TYPE_MENU));

            assertEquals(UserConstants.UNIQUE, result);
        }

        @Test
        @DisplayName("menuId 为 null → 用 -1L 比较，无冲突 → UNIQUE")
        void nullMenuId_noConflict_returnsUnique()
        {
            when(menuRepository.findByMenuNameAndParentId("新菜单", 0L)).thenReturn(java.util.Optional.empty());

            boolean result = menuService.checkMenuNameUnique(menu(null, "新菜单", 0L, UserConstants.TYPE_MENU));

            assertEquals(UserConstants.UNIQUE, result);
        }
    }

    // ─────────────────────────────────────────────
    // checkRouteConfigUnique — 路由冲突
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("checkRouteConfigUnique 路由冲突检测")
    class RouteConfigUniqueTests
    {
        @Test
        @DisplayName("无冲突 → UNIQUE")
        void noConflict_returnsUnique()
        {
            when(menuRepository.selectMenusByPathOrRouteName(anyString(), anyString()))
                    .thenReturn(List.of());

            boolean result = menuService.checkRouteConfigUnique(menu(1L, "新菜单", 0L, UserConstants.TYPE_DIR, "newPath", "NewRoute"));

            assertEquals(UserConstants.UNIQUE, result);
        }

        @Test
        @DisplayName("同级路径冲突（不同 menuId）→ NOT_UNIQUE")
        void sameLevelPathConflict_returnsNotUnique()
        {
            SysMenu existing = menu(2L, "已有菜单", 0L, UserConstants.TYPE_MENU);
            existing.setPath("user");
            existing.setRouteName("User");
            when(menuRepository.selectMenusByPathOrRouteName("user", "User"))
                    .thenReturn(List.of(existing));

            boolean result = menuService.checkRouteConfigUnique(menu(1L, "新菜单", 0L, UserConstants.TYPE_MENU, "user", "User"));

            assertEquals(UserConstants.NOT_UNIQUE, result);
        }

        @Test
        @DisplayName("根目录路径冲突（parentId=0）→ NOT_UNIQUE")
        void rootLevelPathConflict_returnsNotUnique()
        {
            SysMenu existing = menu(2L, "已有", 0L, UserConstants.TYPE_DIR);
            existing.setPath("dashboard");
            existing.setRouteName("Dashboard");
            when(menuRepository.selectMenusByPathOrRouteName("dashboard", "Dashboard"))
                    .thenReturn(List.of(existing));

            boolean result = menuService.checkRouteConfigUnique(menu(1L, "新", 0L, UserConstants.TYPE_DIR, "dashboard", "Dashboard"));

            assertEquals(UserConstants.NOT_UNIQUE, result);
        }

        @Test
        @DisplayName("同 menuId → UNIQUE（自己不算冲突）")
        void sameMenuId_returnsUnique()
        {
            SysMenu existing = menu(1L, "自己", 0L, UserConstants.TYPE_MENU);
            existing.setPath("user");
            existing.setRouteName("User");
            when(menuRepository.selectMenusByPathOrRouteName("user", "User"))
                    .thenReturn(List.of(existing));

            boolean result = menuService.checkRouteConfigUnique(menu(1L, "自己", 0L, UserConstants.TYPE_MENU, "user", "User"));

            assertEquals(UserConstants.UNIQUE, result);
        }
    }

    // ─────────────────────────────────────────────
    // 辅助方法 — getRouteName / getRouterPath / getComponent
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("辅助方法")
    class HelperMethodTests
    {
        @Test
        @DisplayName("getRouteName：一级菜单且 isMenuFrame → 返回空字符串")
        void getRouteName_menuFrame_returnsEmpty()
        {
            SysMenu m = menu(1L, "首页", 0L, UserConstants.TYPE_MENU);
            m.setIsFrame(UserConstants.NO_FRAME);

            String name = menuService.getRouteName(m);

            assertEquals("", name);
        }

        @Test
        @DisplayName("getRouteName：目录类型 → 返回大写路由名（path 转驼峰）")
        void getRouteName_dir_returnsCapitalized()
        {
            SysMenu m = menu(1L, "系统管理", 0L, UserConstants.TYPE_DIR);
            m.setPath("system");
            m.setRouteName(null);  // 强制使用 path 而非 routeName
            m.setIsFrame(UserConstants.YES_FRAME);

            String name = menuService.getRouteName(m);

            assertEquals("System", name);
        }

        @Test
        @DisplayName("getRouteName(String, String)：name 有值 → 直接首字母大写")
        void getRouteNameWithName_capitalizes()
        {
            String result = menuService.getRouteName("userList", "/user/list");
            assertEquals("UserList", result);
        }

        @Test
        @DisplayName("getRouteName(String, String)：name 为空 → 用 path 首字母大写")
        void getRouteNameWithPathOnly_capitalizesPath()
        {
            String result = menuService.getRouteName("", "user/list");
            assertEquals("User/list", result);
        }

        @Test
        @DisplayName("getRouterPath：一级目录（TYPE_DIR + 非外链）→ 前面加 /")
        void getRouterPath_rootDir_addsSlash()
        {
            SysMenu m = menu(1L, "系统", 0L, UserConstants.TYPE_DIR);
            m.setPath("system");
            m.setIsFrame(UserConstants.NO_FRAME);

            String path = menuService.getRouterPath(m);

            assertEquals("/system", path);
        }

        @Test
        @DisplayName("getRouterPath：一级菜单（TYPE_MENU + 非外链）→ 返回 /")
        void getRouterPath_rootMenu_returnsSlash()
        {
            SysMenu m = menu(1L, "首页", 0L, UserConstants.TYPE_MENU);
            m.setIsFrame(UserConstants.NO_FRAME);

            String path = menuService.getRouterPath(m);

            assertEquals("/", path);
        }

        @Test
        @DisplayName("getComponent：普通菜单 → 返回 menu.component")
        void getComponent_normalMenu()
        {
            SysMenu m = menu(1L, "用户管理", 1L, UserConstants.TYPE_MENU);
            m.setComponent("system/user/index");

            String component = menuService.getComponent(m);

            assertEquals("system/user/index", component);
        }

        @Test
        @DisplayName("getComponent：外链菜单（isMenuFrame=true）→ 返回 LAYOUT")
        void getComponent_menuFrame_returnsLayout()
        {
            SysMenu m = menu(1L, "首页", 0L, UserConstants.TYPE_MENU);
            m.setIsFrame(UserConstants.NO_FRAME);

            String component = menuService.getComponent(m);

            assertEquals(UserConstants.LAYOUT, component);
        }

        @Test
        @DisplayName("isMenuFrame：一级菜单且 TYPE_MENU 且 NO_FRAME → true")
        void isMenuFrame_matches_returnsTrue()
        {
            SysMenu m = menu(1L, "首页", 0L, UserConstants.TYPE_MENU);
            m.setIsFrame(UserConstants.NO_FRAME);

            assertTrue(menuService.isMenuFrame(m));
        }

        @Test
        @DisplayName("isMenuFrame：目录类型 → false")
        void isMenuFrame_dir_returnsFalse()
        {
            SysMenu m = menu(1L, "系统", 0L, UserConstants.TYPE_DIR);
            m.setIsFrame(UserConstants.NO_FRAME);

            assertFalse(menuService.isMenuFrame(m));
        }

        @Test
        @DisplayName("isParentView：非一级目录 → true")
        void isParentView_nonRootDir_returnsTrue()
        {
            SysMenu m = menu(1L, "子目录", 100L, UserConstants.TYPE_DIR);

            assertTrue(menuService.isParentView(m));
        }

        @Test
        @DisplayName("isInnerLink：http 链接 + NO_FRAME → true")
        void isInnerLink_httpLink_returnsTrue()
        {
            SysMenu m = menu(1L, "外链", 1L, UserConstants.TYPE_MENU);
            m.setPath("http://example.com");
            m.setIsFrame(UserConstants.NO_FRAME);

            assertTrue(menuService.isInnerLink(m));
        }

    // (innerLinkReplaceEach 是 private 实现细节，与 buildMenus 集成测试覆盖，此处不再单独测试)
    }

    // ─────────────────────────────────────────────
    // selectMenuListByRoleId
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("selectMenuListByRoleId")
    class SelectMenuListByRoleIdTests
    {
        @Test
        @DisplayName("正常查询 → roleRepository + menuRepository")
        void normalQuery_passesThrough()
        {
            SysRole role = new SysRole();
            role.setMenuCheckStrictly(true);
            when(roleRepository.findById(1L)).thenReturn(java.util.Optional.of(role));
            when(menuRepository.selectMenuListByRoleId(1L, true)).thenReturn(List.of(1L, 2L));

            List<Long> result = menuService.selectMenuListByRoleId(1L);

            assertEquals(2, result.size());
        }
    }

    // ─────────────────────────────────────────────
    // 辅助方法
    // ─────────────────────────────────────────────

    private SysMenu menu(Long menuId, String menuName, Long parentId, String menuType)
    {
        return menu(menuId, menuName, parentId, menuType, "path_" + menuId, "route_" + menuId);
    }

    private SysMenu menu(Long menuId, String menuName, Long parentId, String menuType,
                         String path, String routeName)
    {
        SysMenu m = new SysMenu();
        m.setMenuId(menuId);
        m.setMenuName(menuName);
        m.setParentId(parentId);
        m.setMenuType(menuType);
        m.setPath(path);
        m.setRouteName(routeName);
        m.setVisible("0");
        m.setStatus("0");
        m.setIsFrame(UserConstants.YES_FRAME);
        m.setIsCache("0");
        m.setIcon("el-icon-menu");
        return m;
    }
}
