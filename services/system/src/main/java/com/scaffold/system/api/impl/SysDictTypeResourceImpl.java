package com.scaffold.system.api.impl;

import java.util.List;
import org.springframework.web.bind.annotation.RequestParam;
import org.apache.dubbo.config.annotation.DubboService;
import com.scaffold.common.core.text.Convert;
import com.scaffold.common.core.utils.PageUtils;
import com.scaffold.common.core.utils.poi.ExcelUtil;
import com.scaffold.common.core.web.controller.BaseController;
import com.scaffold.common.core.web.domain.AjaxResult;
import com.scaffold.common.core.web.page.PageDomain;
import com.scaffold.common.core.web.page.TableDataInfo;
import com.scaffold.common.log.annotation.Log;
import com.scaffold.common.log.enums.BusinessType;
import com.scaffold.common.security.annotation.RequiresPermissions;
import com.scaffold.common.security.utils.SecurityUtils;
import com.scaffold.system.api.SysDictTypeResource;
import com.scaffold.system.api.domain.SysDictType;
import com.scaffold.system.service.ISysDictTypeService;

/**
 * 字典类型信息服务实现（Triple REST）
 *
 * @author scaffold
 */
@DubboService
public class SysDictTypeResourceImpl extends BaseController implements SysDictTypeResource
{
    private final ISysDictTypeService dictTypeService;

    public SysDictTypeResourceImpl(ISysDictTypeService dictTypeService) {
        this.dictTypeService = dictTypeService;
    }

    @RequiresPermissions("system:dict:list")
    @Override
    public TableDataInfo list(Integer pageNum, Integer pageSize, String orderByColumn, String isAsc, String dictName, String dictType, String status)
    {
        PageDomain page = new PageDomain();
        page.setPageNum(pageNum);
        page.setPageSize(pageSize);
        page.setOrderByColumn(orderByColumn);
        page.setIsAsc(isAsc);
        SysDictType dict = new SysDictType();
        dict.setDictName(dictName);
        dict.setDictType(dictType);
        dict.setStatus(status);
        return dictTypeService.selectDictTypePage(dict, page);
    }

    @Log(title = "字典类型", businessType = BusinessType.EXPORT)
    @RequiresPermissions("system:dict:export")
    @Override
    public byte[] export(String dictName, String dictType, String status)
    {
        SysDictType dict = new SysDictType();
        dict.setDictName(dictName);
        dict.setDictType(dictType);
        dict.setStatus(status);
        List<SysDictType> list = dictTypeService.selectDictTypeList(dict);
        ExcelUtil<SysDictType> util = new ExcelUtil<SysDictType>(SysDictType.class);
        return util.exportExcel(list, "字典类型");
    }

    @RequiresPermissions("system:dict:query")
    @Override
    public AjaxResult getInfo(Long dictId)
    {
        return success(dictTypeService.selectDictTypeById(dictId));
    }

    @RequiresPermissions("system:dict:add")
    @Log(title = "字典类型", businessType = BusinessType.INSERT)
    @Override
    public AjaxResult add(SysDictType dict)
    {
        if (!dictTypeService.checkDictTypeUnique(dict))
        {
            return error("新增字典'" + dict.getDictName() + "'失败，字典类型已存在");
        }
        dict.setCreateBy(SecurityUtils.getUsername());
        return toAjax(dictTypeService.insertDictType(dict));
    }

    @RequiresPermissions("system:dict:edit")
    @Log(title = "字典类型", businessType = BusinessType.UPDATE)
    @Override
    public AjaxResult edit(SysDictType dict)
    {
        if (!dictTypeService.checkDictTypeUnique(dict))
        {
            return error("修改字典'" + dict.getDictName() + "'失败，字典类型已存在");
        }
        dict.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(dictTypeService.updateDictType(dict));
    }

    @RequiresPermissions("system:dict:remove")
    @Log(title = "字典类型", businessType = BusinessType.DELETE)
    @Override
    public AjaxResult remove(String dictIds)
    {
        dictTypeService.deleteDictTypeByIds(Convert.toLongArray(dictIds));
        return success();
    }

    @RequiresPermissions("system:dict:remove")
    @Log(title = "字典类型", businessType = BusinessType.CLEAN)
    @Override
    public AjaxResult refreshCache()
    {
        dictTypeService.resetDictCache();
        return success();
    }

    @Override
    public AjaxResult optionselect()
    {
        List<SysDictType> dictTypes = dictTypeService.selectDictTypeAll();
        return success(dictTypes);
    }
}