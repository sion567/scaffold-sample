package com.scaffold.system.api.impl;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.apache.dubbo.config.annotation.DubboService;
import com.scaffold.common.core.constant.UserConstants;
import com.scaffold.common.core.utils.StringUtils;
import com.scaffold.common.core.web.controller.BaseController;
import com.scaffold.common.core.web.domain.AjaxResult;
import com.scaffold.common.log.annotation.Log;
import com.scaffold.common.log.enums.BusinessType;
import com.scaffold.common.security.annotation.RequiresPermissions;
import com.scaffold.common.security.utils.SecurityUtils;
import com.scaffold.system.api.SysMenuResource;
import com.scaffold.system.domain.SysMenu;
import com.scaffold.system.service.ISysMenuService;

@DubboService
public class SysMenuResourceImpl extends BaseController implements SysMenuResource
{
    private final ISysMenuService menuService;

    public SysMenuResourceImpl(ISysMenuService menuService)
    {
        this.menuService = menuService;
    }

    @RequiresPermissions("system:menu:list")
    @Override
    public AjaxResult list(String menuName, String status)
    {
        Long userId = SecurityUtils.getUserId();
        List<SysMenu> menus = menuService.selectMenuList(buildMenuQuery(menuName, status), userId);
        return success(menus);
    }

    @RequiresPermissions("system:menu:query")
    @Override
    public AjaxResult getInfo(@PathVariable("menuId") Long menuId)
    {
        return success(menuService.selectMenuById(menuId));
    }

    @Override
    public AjaxResult treeselect(String menuName, String status)
    {
        Long userId = SecurityUtils.getUserId();
        List<SysMenu> menus = menuService.selectMenuList(buildMenuQuery(menuName, status), userId);
        return success(menuService.buildMenuTreeSelect(menus));
    }

    /** 菜单查询条件（接口层过滤字段摊平后的装配） */
    private SysMenu buildMenuQuery(String menuName, String status)
    {
        SysMenu menu = new SysMenu();
        menu.setMenuName(menuName);
        menu.setStatus(status);
        return menu;
    }

    @Override
    public AjaxResult roleMenuTreeselect(@PathVariable("roleId") Long roleId)
    {
        Long userId = SecurityUtils.getUserId();
        List<SysMenu> menus = menuService.selectMenuList(userId);
        AjaxResult ajax = AjaxResult.success();
        ajax.put("checkedKeys", menuService.selectMenuListByRoleId(roleId));
        ajax.put("menus", menuService.buildMenuTreeSelect(menus));
        return ajax;
    }

    @RequiresPermissions("system:menu:add")
    @Log(title = "菜单管理", businessType = BusinessType.INSERT)
    @Override
    public AjaxResult add(@RequestBody SysMenu menu)
    {
        if (!menuService.checkMenuNameUnique(menu))
        {
            return error("新增菜单'" + menu.getMenuName() + "'失败，菜单名称已存在");
        }
        else if (UserConstants.YES_FRAME.equals(menu.getIsFrame()) && !StringUtils.ishttp(menu.getPath()))
        {
            return error("新增菜单'" + menu.getMenuName() + "'失败，地址必须以http(s)://开头");
        }
        else if (!menuService.checkRouteConfigUnique(menu))
        {
            return error("新增菜单'" + menu.getMenuName() + "'失败，路由名称或地址已存在");
        }
        menu.setCreateBy(SecurityUtils.getUsername());
        return toAjax(menuService.insertMenu(menu));
    }

    @RequiresPermissions("system:menu:edit")
    @Log(title = "菜单管理", businessType = BusinessType.UPDATE)
    @Override
    public AjaxResult edit(@RequestBody SysMenu menu)
    {
        if (!menuService.checkMenuNameUnique(menu))
        {
            return error("修改菜单'" + menu.getMenuName() + "'失败，菜单名称已存在");
        }
        else if (UserConstants.YES_FRAME.equals(menu.getIsFrame()) && !StringUtils.ishttp(menu.getPath()))
        {
            return error("修改菜单'" + menu.getMenuName() + "'失败，地址必须以http(s)://开头");
        }
        else if (menu.getMenuId().equals(menu.getParentId()))
        {
            return error("修改菜单'" + menu.getMenuName() + "'失败，上级菜单不能选择自己");
        }
        else if (!menuService.checkRouteConfigUnique(menu))
        {
            return error("修改菜单'" + menu.getMenuName() + "'失败，路由名称或地址已存在");
        }
        menu.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(menuService.updateMenu(menu));
    }

    @RequiresPermissions("system:menu:edit")
    @Log(title = "保存菜单排序", businessType = BusinessType.UPDATE)
    @Override
    public AjaxResult updateSort(Map<String, String> params)
    {
        String[] menuIds = params.get("menuIds").split(",");
        String[] orderNums = params.get("orderNums").split(",");
        menuService.updateMenuSort(menuIds, orderNums);
        return success();
    }

    @RequiresPermissions("system:menu:remove")
    @Log(title = "菜单管理", businessType = BusinessType.DELETE)
    @Override
    public AjaxResult remove(@PathVariable("menuId") Long menuId)
    {
        if (menuService.hasChildByMenuId(menuId))
        {
            return warn("存在子菜单,不允许删除");
        }
        if (menuService.checkMenuExistRole(menuId))
        {
            return warn("菜单已分配,不允许删除");
        }
        return toAjax(menuService.deleteMenuById(menuId));
    }

    @Override
    public AjaxResult getRouters()
    {
        Long userId = SecurityUtils.getUserId();
        List<SysMenu> menus = menuService.selectMenuTreeByUserId(userId);
        return success(menuService.buildMenus(menus));
    }
}