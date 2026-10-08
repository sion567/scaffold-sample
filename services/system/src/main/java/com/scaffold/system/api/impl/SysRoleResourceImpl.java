package com.scaffold.system.api.impl;

import java.util.List;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.apache.dubbo.config.annotation.DubboService;
import com.scaffold.common.core.utils.PageUtils;
import com.scaffold.common.core.web.controller.BaseController;
import com.scaffold.common.core.web.domain.AjaxResult;
import com.scaffold.common.core.web.page.PageDomain;
import com.scaffold.common.core.web.page.TableDataInfo;
import com.scaffold.common.log.annotation.Log;
import com.scaffold.common.log.enums.BusinessType;
import com.scaffold.common.security.annotation.RequiresPermissions;
import com.scaffold.common.security.utils.SecurityUtils;
import com.scaffold.system.api.SysRoleResource;
import com.scaffold.system.api.domain.SysDept;
import com.scaffold.system.api.domain.SysRole;
import com.scaffold.system.api.domain.SysUser;
import com.scaffold.system.domain.SysUserRole;
import com.scaffold.system.service.ISysDeptService;
import com.scaffold.system.service.ISysRoleService;
import com.scaffold.system.service.ISysUserService;

/**
 * 角色信息服务实现（Triple REST）
 *
 * @author scaffold
 */
@DubboService
public class SysRoleResourceImpl extends BaseController implements SysRoleResource
{
    private final ISysRoleService roleService;
    private final ISysUserService userService;
    private final ISysDeptService deptService;

    public SysRoleResourceImpl(ISysRoleService roleService, ISysUserService userService, ISysDeptService deptService)
    {
        this.roleService = roleService;
        this.userService = userService;
        this.deptService = deptService;
    }

    @RequiresPermissions("system:role:list")
    @Override
    public TableDataInfo list(Integer pageNum, Integer pageSize, String orderByColumn, String isAsc,
                              String roleName, String roleKey, String status, String beginTime, String endTime)
    {
        PageDomain page = new PageDomain();
        page.setPageNum(pageNum);
        page.setPageSize(pageSize);
        page.setOrderByColumn(orderByColumn);
        page.setIsAsc(isAsc);
        SysRole role = new SysRole();
        role.setRoleName(roleName);
        role.setRoleKey(roleKey);
        role.setStatus(status);
        role.getParams().put("beginTime", beginTime);
        role.getParams().put("endTime", endTime);
        return roleService.selectRolePage(role, page);
    }

    @Log(title = "角色管理", businessType = BusinessType.EXPORT)
    @RequiresPermissions("system:role:export")
    @Override
    public byte[] export(SysRole role)
    {
        List<SysRole> list = roleService.selectRoleList(role);
        com.scaffold.common.core.utils.poi.ExcelUtil<SysRole> util = new com.scaffold.common.core.utils.poi.ExcelUtil<SysRole>(SysRole.class);
        return util.exportExcel(list, "角色数据");
    }

    @RequiresPermissions("system:role:query")
    @Override
    public AjaxResult getInfo(Long roleId)
    {
        roleService.checkRoleDataScope(roleId);
        return success(roleService.selectRoleById(roleId));
    }

    @RequiresPermissions("system:role:add")
    @Log(title = "角色管理", businessType = BusinessType.INSERT)
    @Override
    public AjaxResult add(SysRole role)
    {
        if (!roleService.checkRoleNameUnique(role))
        {
            return error("新增角色'" + role.getRoleName() + "'失败，角色名称已存在");
        }
        else if (!roleService.checkRoleKeyUnique(role))
        {
            return error("新增角色'" + role.getRoleName() + "'失败，角色权限已存在");
        }
        role.setCreateBy(SecurityUtils.getUsername());
        return toAjax(roleService.insertRole(role));
    }

    @RequiresPermissions("system:role:edit")
    @Log(title = "角色管理", businessType = BusinessType.UPDATE)
    @Override
    public AjaxResult edit(SysRole role)
    {
        roleService.checkRoleAllowed(role);
        roleService.checkRoleDataScope(role.getRoleId());
        if (!roleService.checkRoleNameUnique(role))
        {
            return error("修改角色'" + role.getRoleName() + "'失败，角色名称已存在");
        }
        else if (!roleService.checkRoleKeyUnique(role))
        {
            return error("修改角色'" + role.getRoleName() + "'失败，角色权限已存在");
        }
        role.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(roleService.updateRole(role));
    }

    @RequiresPermissions("system:role:edit")
    @Log(title = "角色管理", businessType = BusinessType.UPDATE)
    @Override
    public AjaxResult dataScope(SysRole role)
    {
        roleService.checkRoleAllowed(role);
        roleService.checkRoleDataScope(role.getRoleId());
        return toAjax(roleService.authDataScope(role));
    }

    @RequiresPermissions("system:role:edit")
    @Log(title = "角色管理", businessType = BusinessType.UPDATE)
    @Override
    public AjaxResult changeStatus(SysRole role)
    {
        roleService.checkRoleAllowed(role);
        roleService.checkRoleDataScope(role.getRoleId());
        role.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(roleService.updateRoleStatus(role));
    }

    @RequiresPermissions("system:role:remove")
    @Log(title = "角色管理", businessType = BusinessType.DELETE)
    @Override
    public AjaxResult remove(Long[] roleIds)
    {
        return toAjax(roleService.deleteRoleByIds(roleIds));
    }

    @RequiresPermissions("system:role:query")
    @Override
    public AjaxResult optionselect()
    {
        return success(roleService.selectRoleAll());
    }

    @RequiresPermissions("system:role:list")
    @Override
    public TableDataInfo allocatedList(Integer pageNum, Integer pageSize, String orderByColumn, String isAsc,
                                       Long roleId, String userName, String phonenumber)
    {
        PageDomain page = new PageDomain();
        page.setPageNum(pageNum);
        page.setPageSize(pageSize);
        page.setOrderByColumn(orderByColumn);
        page.setIsAsc(isAsc);
        SysUser user = buildAuthUserQuery(roleId, userName, phonenumber);
        return userService.selectAllocatedPage(user, page);
    }

    @RequiresPermissions("system:role:list")
    @Override
    public TableDataInfo unallocatedList(Integer pageNum, Integer pageSize, String orderByColumn, String isAsc,
                                         Long roleId, String userName, String phonenumber)
    {
        PageDomain page = new PageDomain();
        page.setPageNum(pageNum);
        page.setPageSize(pageSize);
        page.setOrderByColumn(orderByColumn);
        page.setIsAsc(isAsc);
        SysUser user = buildAuthUserQuery(roleId, userName, phonenumber);
        return userService.selectUnallocatedPage(user, page);
    }

    /** 已分配/未分配用户查询条件（roleId 走 params，selectAllocatedList 从中取） */
    private SysUser buildAuthUserQuery(Long roleId, String userName, String phonenumber)
    {
        SysUser user = new SysUser();
        user.setUserName(userName);
        user.setPhonenumber(phonenumber);
        user.getParams().put("roleId", roleId);
        return user;
    }

    @RequiresPermissions("system:role:edit")
    @Log(title = "角色管理", businessType = BusinessType.GRANT)
    @Override
    public AjaxResult cancelAuthUser(SysUserRole userRole)
    {
        return toAjax(roleService.deleteAuthUser(userRole));
    }

    @RequiresPermissions("system:role:edit")
    @Log(title = "角色管理", businessType = BusinessType.GRANT)
    @Override
    public AjaxResult cancelAuthUserAll(Long roleId, Long[] userIds)
    {
        return toAjax(roleService.deleteAuthUsers(roleId, userIds));
    }

    @RequiresPermissions("system:role:edit")
    @Log(title = "角色管理", businessType = BusinessType.GRANT)
    @Override
    public AjaxResult selectAuthUserAll(Long roleId, Long[] userIds)
    {
        roleService.checkRoleDataScope(roleId);
        return toAjax(roleService.insertAuthUsers(roleId, userIds));
    }

    @RequiresPermissions("system:role:query")
    @Override
    public AjaxResult deptTree(@PathVariable Long roleId)
    {
        AjaxResult ajax = success();
        ajax.put("checkedKeys", deptService.selectDeptListByRoleId(roleId));
        ajax.put("depts", deptService.selectDeptTreeList(new SysDept()));
        return ajax;
    }
}
