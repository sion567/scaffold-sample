package com.scaffold.system.api.impl;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.Map;
import org.apache.dubbo.config.annotation.DubboService;
import com.scaffold.common.core.constant.UserConstants;
import com.scaffold.common.core.utils.StringUtils;
import com.scaffold.common.core.web.controller.BaseController;
import com.scaffold.common.core.web.domain.AjaxResult;
import com.scaffold.common.log.annotation.Log;
import com.scaffold.common.log.enums.BusinessType;
import com.scaffold.common.security.annotation.RequiresPermissions;
import com.scaffold.common.security.utils.SecurityUtils;
import com.scaffold.system.api.SysDeptResource;
import com.scaffold.system.api.domain.SysDept;
import com.scaffold.system.service.ISysDeptService;

@DubboService
public class SysDeptResourceImpl extends BaseController implements SysDeptResource
{
    private final ISysDeptService deptService;

    public SysDeptResourceImpl(ISysDeptService deptService) {
        this.deptService = deptService;
    }

    @RequiresPermissions("system:dept:list")
    @Override
    public AjaxResult list(SysDept dept)
    {
        return success(deptService.selectDeptList(dept));
    }

    @RequiresPermissions("system:dept:list")
    @Override
    public AjaxResult excludeChild(@PathVariable Long deptId)
    {
        return success(deptService.selectDeptExcludeChild(deptId));
    }

    @RequiresPermissions("system:dept:query")
    @Override
    public AjaxResult getInfo(Long deptId)
    {
        deptService.checkDeptDataScope(deptId);
        return success(deptService.selectDeptById(deptId));
    }

    @RequiresPermissions("system:dept:add")
    @Log(title = "部门管理", businessType = BusinessType.INSERT)
    @Override
    public AjaxResult add(@RequestBody SysDept dept)
    {
        if (!deptService.checkDeptNameUnique(dept))
        {
            return error("新增部门'" + dept.getDeptName() + "'失败，部门名称已存在");
        }
        dept.setCreateBy(SecurityUtils.getUsername());
        return toAjax(deptService.insertDept(dept));
    }

    @RequiresPermissions("system:dept:edit")
    @Log(title = "部门管理", businessType = BusinessType.UPDATE)
    @Override
    public AjaxResult edit(@RequestBody SysDept dept)
    {
        Long deptId = dept.getDeptId();
        deptService.checkDeptDataScope(deptId);
        if (!deptService.checkDeptNameUnique(dept))
        {
            return error("修改部门'" + dept.getDeptName() + "'失败，部门名称已存在");
        }
        else if (dept.getParentId().equals(deptId))
        {
            return error("修改部门'" + dept.getDeptName() + "'失败，上级部门不能是自己");
        }
        else if (StringUtils.equals(UserConstants.DEPT_DISABLE, dept.getStatus())
                && deptService.selectNormalChildrenDeptById(deptId) > 0)
        {
            return error("该部门包含未停用的子部门！");
        }
        dept.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(deptService.updateDept(dept));
    }

    @RequiresPermissions("system:dept:edit")
    @Log(title = "保存部门排序", businessType = BusinessType.UPDATE)
    @Override
    public AjaxResult updateSort(Map<String, String> params)
    {
        String[] deptIds = params.get("deptIds").split(",");
        String[] orderNums = params.get("orderNums").split(",");
        deptService.updateDeptSort(deptIds, orderNums);
        return success();
    }

    @RequiresPermissions("system:dept:remove")
    @Log(title = "部门管理", businessType = BusinessType.DELETE)
    @Override
    public AjaxResult remove(@PathVariable Long deptId)
    {
        if (deptService.hasChildByDeptId(deptId))
        {
            return warn("存在下级部门,不允许删除");
        }
        if (deptService.checkDeptExistUser(deptId))
        {
            return warn("部门存在用户,不允许删除");
        }
        deptService.checkDeptDataScope(deptId);
        return toAjax(deptService.deleteDeptById(deptId));
    }
}