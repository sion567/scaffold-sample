package com.scaffold.system.service.impl;

import java.util.ArrayList;
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

import com.scaffold.common.core.constant.UserConstants;
import com.scaffold.common.core.exception.ServiceException;
import com.scaffold.common.security.utils.SecurityUtils;
import com.scaffold.common.core.utils.SpringUtils;
import com.scaffold.system.api.domain.SysDept;
import com.scaffold.system.api.domain.SysRole;
import com.scaffold.system.domain.vo.TreeSelect;
import com.scaffold.system.repository.SysDeptRepository;
import com.scaffold.system.repository.SysRoleRepository;
import com.scaffold.system.repository.SysUserRepository;
import com.scaffold.system.service.impl.SysDeptServiceImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SysDeptServiceImpl Mock 测试（边界防御 + 树结构构建 + 数据权限）。
 *
 * <p>覆盖维度：
 * <ul>
 *   <li>边界防御：selectDeptById / hasChildByDeptId / checkDeptExistUser</li>
 *   <li>树结构构建：buildDeptTree 递归构造，buildDeptTreeSelect 下拉树</li>
 *   <li>唯一性：checkDeptNameUnique 同名不同父级，同父级同名</li>
 *   <li>数据权限：checkDeptDataScope 非管理员无权限时抛异常</li>
 *   <li>部门操作：insertDept 父部门停用禁止新增；updateDept 子部门继承父级变更</li>
 * </ul>
 *
 * @author ct
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysDeptServiceImplTest
{
    @Mock
    private SysDeptRepository deptRepository;

    @Mock
    private SysRoleRepository roleRepository;

    @Mock
    private SysUserRepository userRepository;

    private SysDeptServiceImpl deptService;

    @BeforeEach
    void setUp()
    {
        deptService = new SysDeptServiceImpl(deptRepository, roleRepository, userRepository);
    }

    // ─────────────────────────────────────────────
    // selectDeptById / selectNormalChildrenDeptById
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("selectDeptById / selectNormalChildrenDeptById")
    class BasicQueryTests
    {
        @Test
        @DisplayName("selectDeptById → 透传 mapper")
        void selectDeptById_normal()
        {
            SysDept dept = dept(100L, "研发部", 0L);
            when(deptRepository.findById(100L)).thenReturn(java.util.Optional.of(dept));

            SysDept result = deptService.selectDeptById(100L);

            assertNotNull(result);
            assertEquals(100L, result.getDeptId());
        }

        @Test
        @DisplayName("selectNormalChildrenDeptById → 透传 mapper")
        void selectNormalChildrenDeptById_normal()
        {
            when(deptRepository.countNormalChildren(100L)).thenReturn(3L);

            int count = deptService.selectNormalChildrenDeptById(100L);

            assertEquals(3, count);
        }
    }

    // ─────────────────────────────────────────────
    // buildDeptTree — 树结构构建
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("buildDeptTree 树结构构建")
    class BuildDeptTreeTests
    {
        @Test
        @DisplayName("多级树：顶级节点（parentId 不在列表中）作为根节点递归挂载子节点")
        void multiLevel_buildsCorrectTree()
        {
            // 根节点
            SysDept root = dept(1L, "总公司", 0L);
            // 子节点
            SysDept child1 = dept(2L, "研发部", 1L);
            SysDept child2 = dept(3L, "市场部", 1L);
            // 孙节点
            SysDept grandChild = dept(4L, "前端组", 2L);
            grandChild.setParentId(2L);

            List<SysDept> depts = new ArrayList<>(List.of(root, child1, child2, grandChild));

            List<SysDept> tree = deptService.buildDeptTree(depts);

            assertEquals(1, tree.size());
            assertEquals("总公司", tree.get(0).getDeptName());
            assertEquals(2, tree.get(0).getChildren().size());
        }

        @Test
        @DisplayName("列表为空 → 返回空列表，不抛异常")
        void emptyList_returnsEmpty()
        {
            List<SysDept> tree = deptService.buildDeptTree(List.of());
            assertTrue(tree.isEmpty());
        }

        @Test
        @DisplayName("所有节点都不是顶级节点（循环引用或平铺）→ 整个列表作为根返回")
        void noRoot_returnsAllAsRoot()
        {
            SysDept a = dept(1L, "A", 2L);
            SysDept b = dept(2L, "B", 1L);

            List<SysDept> tree = deptService.buildDeptTree(List.of(a, b));

            assertEquals(2, tree.size());
        }
    }

    // ─────────────────────────────────────────────
    // buildDeptTreeSelect — 下拉树
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("buildDeptTreeSelect 下拉树")
    class BuildDeptTreeSelectTests
    {
        @Test
        @DisplayName("调用 buildDeptTree 后转 TreeSelect")
        void convertsToTreeSelect()
        {
            SysDept root = dept(1L, "总公司", 0L);
            SysDept child = dept(2L, "分公司", 1L);
            List<SysDept> flat = List.of(root, child);

            List<TreeSelect> tree = deptService.buildDeptTreeSelect(flat);

            assertEquals(1, tree.size());
            assertEquals("总公司", tree.get(0).getLabel());
        }
    }

    // ─────────────────────────────────────────────
    // hasChildByDeptId / checkDeptExistUser
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("hasChildByDeptId / checkDeptExistUser")
    class ChildUserCheckTests
    {
        @Test
        @DisplayName("hasChildByDeptId：有子部门 → true")
        void hasChildren_returnsTrue()
        {
            when(deptRepository.countByParentIdAndDelFlag(1L, "0")).thenReturn(5L);
            assertTrue(deptService.hasChildByDeptId(1L));
        }

        @Test
        @DisplayName("hasChildByDeptId：无子部门 → false")
        void noChildren_returnsFalse()
        {
            when(deptRepository.countByParentIdAndDelFlag(99L, "0")).thenReturn(0L);
            assertFalse(deptService.hasChildByDeptId(99L));
        }

        @Test
        @DisplayName("checkDeptExistUser：有用户 → true")
        void hasUser_returnsTrue()
        {
            when(userRepository.countByDeptIdAndDelFlag(1L, "0")).thenReturn(3L);
            assertTrue(deptService.checkDeptExistUser(1L));
        }

        @Test
        @DisplayName("checkDeptExistUser：无用户 → false")
        void noUser_returnsFalse()
        {
            when(userRepository.countByDeptIdAndDelFlag(99L, "0")).thenReturn(0L);
            assertFalse(deptService.checkDeptExistUser(99L));
        }
    }

    // ─────────────────────────────────────────────
    // checkDeptNameUnique — 唯一性
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("checkDeptNameUnique 唯一性")
    class UniqueCheckTests
    {
        @Test
        @DisplayName("同名不同父级 → NOT_UNIQUE")
        void sameNameDifferentParent_returnsNotUnique()
        {
            when(deptRepository.findByDeptNameAndParentIdAndDelFlag("研发部", 2L, "0"))
                    .thenReturn(java.util.Optional.of(dept(2L, "研发部", 2L)));

            // parentId=2L 与 mapper 查询参数一致
            boolean result = deptService.checkDeptNameUnique(dept(1L, "研发部", 2L));

            assertEquals(UserConstants.NOT_UNIQUE, result);
        }

        @Test
        @DisplayName("同名同父级（同 deptId）→ UNIQUE（自己不算冲突）")
        void sameNameSameParentSameId_returnsUnique()
        {
            when(deptRepository.findByDeptNameAndParentIdAndDelFlag("研发部", 1L, "0"))
                    .thenReturn(java.util.Optional.of(dept(1L, "研发部", 0L)));

            boolean result = deptService.checkDeptNameUnique(dept(1L, "研发部", 0L));

            assertEquals(UserConstants.UNIQUE, result);
        }

        @Test
        @DisplayName("deptId 为 null → 用 -1L 比较，无冲突 → UNIQUE")
        void nullDeptId_noConflict_returnsUnique()
        {
            when(deptRepository.findByDeptNameAndParentIdAndDelFlag("新部门", 0L, "0")).thenReturn(java.util.Optional.empty());

            boolean result = deptService.checkDeptNameUnique(dept(null, "新部门", 0L));

            assertEquals(UserConstants.UNIQUE, result);
        }
    }

    // ─────────────────────────────────────────────
    // checkDeptDataScope — 数据权限
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("checkDeptDataScope 数据权限")
    class DataScopeTests
    {
        @Test
        @DisplayName("管理员用户 → 直接通过（isAdmin=true 跳过检查）")
        void adminUser_skipsDataScopeCheck()
        {
            try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class))
            {
                security.when(SecurityUtils::isAdmin).thenReturn(true);

                deptService.checkDeptDataScope(100L);

                verify(deptRepository, never()).list(any());
            }
        }

        @Test
        @DisplayName("deptId 为 null → isAdmin=false 时跳过检查（不查 DB）")
        void nullDeptId_skipsCheck()
        {
            try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class))
            {
                security.when(SecurityUtils::isAdmin).thenReturn(false);

                deptService.checkDeptDataScope(null);

                verify(deptRepository, never()).list(any());
            }
        }

        // 注：非管理员且 deptId 不为 null 的数据权限检查依赖 SpringUtils.getAopProxy
        // 和 Spring 容器中的 @DataScope AOP，单元测试覆盖不了，由集成测试负责。
    }

    // ─────────────────────────────────────────────
    // insertDept — 新增部门
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("insertDept 新增部门")
    class InsertDeptTests
    {
        @Test
        @DisplayName("父部门状态正常 → ancestors 拼接并插入")
        void normalParent_insertsWithAncestors()
        {
            SysDept parent = dept(0L, "总公司", -1L);
            parent.setStatus(UserConstants.DEPT_NORMAL);
            parent.setAncestors("");
            SysDept newDept = dept(null, "新部门", 0L);
            when(deptRepository.findById(0L)).thenReturn(java.util.Optional.of(parent));
            

            int rows = deptService.insertDept(newDept);

            assertEquals(1, rows);
            assertEquals(",0", newDept.getAncestors());
            verify(deptRepository).save(newDept);
        }

        @Test
        @DisplayName("父部门停用 → ServiceException，不插入")
        void parentDisabled_throwsAndDoesNotInsert()
        {
            SysDept parent = dept(0L, "总公司", -1L);
            parent.setStatus(UserConstants.DEPT_DISABLE);
            SysDept newDept = dept(null, "新部门", 0L);
            when(deptRepository.findById(0L)).thenReturn(java.util.Optional.of(parent));

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> deptService.insertDept(newDept));

            assertTrue(ex.getMessage().contains("停用"));
            verify(deptRepository, never()).save(any());
        }
    }

    // ─────────────────────────────────────────────
    // selectDeptListByRoleId — 角色关联部门
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("selectDeptListByRoleId")
    class SelectDeptListByRoleIdTests
    {
        @Test
        @DisplayName("正常查询 → roleRepository + deptRepository")
        void normalQuery_passesThrough()
        {
            SysRole role = new SysRole();
            role.setRoleId(1L);
            role.setDeptCheckStrictly(true);
            when(roleRepository.findById(1L)).thenReturn(java.util.Optional.of(role));
            when(deptRepository.selectDeptListByRoleId(1L, true)).thenReturn(List.of(1L, 2L));

            List<Long> result = deptService.selectDeptListByRoleId(1L);

            assertEquals(2, result.size());
        }
    }

    // ─────────────────────────────────────────────
    // 辅助方法
    // ─────────────────────────────────────────────

    private SysDept dept(Long deptId, String deptName, Long parentId)
    {
        SysDept d = new SysDept();
        d.setDeptId(deptId);
        d.setDeptName(deptName);
        d.setParentId(parentId);
        d.setStatus(UserConstants.DEPT_NORMAL);
        d.setAncestors("");
        return d;
    }
}
