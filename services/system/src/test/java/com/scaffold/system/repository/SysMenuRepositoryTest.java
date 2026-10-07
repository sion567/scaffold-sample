package com.scaffold.system.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.scaffold.common.test.H2JpaTest;
import com.scaffold.system.domain.SysMenu;

/**
 * SysMenu 数据访问切片测试（JPA + H2 Oracle 模式）。
 * 重点守住全量菜单树派生查询的方法名与语义：OrderBy 多字段必须逐个带
 * Asc/Desc 分隔，缺分隔会被解析成属性穿越（parentId→orderNum），启动即抛
 * PropertyReferenceException（No property 'orderNum' found for type 'Long'）。
 *
 * @author scaffold
 */
@H2JpaTest(
        ddl = "sql/system/sys_menu_h2.sql",
        entityPackages = {"com.scaffold.system.domain", "com.scaffold.system.api.domain"})
class SysMenuRepositoryTest
{
    private static final List<String> TREE_MENU_TYPES = Arrays.asList("M", "C");

    @Test
    @DisplayName("全量菜单树：M/C + 正常状态过滤，parentId asc 排序")
    void selectMenuTreeAllFilterAndOrder(SysMenuRepository repository)
    {
        List<Long> menuIds = repository.findByMenuTypeInAndStatusOrderByParentIdAscOrderNumAsc(
                TREE_MENU_TYPES, "0").stream().map(SysMenu::getMenuId).collect(Collectors.toList());

        // 种子数据中 102 已停用（status=1）、200 为按钮型（F），都应被过滤
        assertEquals(Arrays.asList(1L, 100L, 101L), menuIds);
    }

    @Test
    @DisplayName("同 parentId 下按 orderNum 次序排序")
    void orderNumBreaksTieWithinSameParent(SysMenuRepository repository)
    {
        SysMenu first = new SysMenu();
        first.setMenuName("置顶目录");
        first.setParentId(0L);
        first.setOrderNum(0);
        first.setPath("top");
        first.setMenuType("M");
        first.setVisible("0");
        first.setStatus("0");
        repository.save(first);

        List<Long> menuIds = repository.findByMenuTypeInAndStatusOrderByParentIdAscOrderNumAsc(
                TREE_MENU_TYPES, "0").stream().map(SysMenu::getMenuId).collect(Collectors.toList());

        // 同为 parentId=0：orderNum=0 的新菜单排在 orderNum=1 的“系统管理”之前
        assertEquals(Arrays.asList(first.getMenuId(), 1L, 100L, 101L), menuIds);

        // @H2JpaTest 类级共享数据源，手动清理避免污染本类其他测试
        repository.delete(first);
    }
}
