package com.scaffold.system.api.impl;

import java.util.ArrayList;
import java.util.List;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import com.scaffold.common.core.text.Convert;
import org.apache.dubbo.config.annotation.DubboService;
import com.scaffold.common.core.utils.PageUtils;
import com.scaffold.common.core.utils.StringUtils;
import com.scaffold.common.core.utils.poi.ExcelUtil;
import com.scaffold.common.core.web.controller.BaseController;
import com.scaffold.common.core.web.domain.AjaxResult;
import com.scaffold.common.core.web.page.PageDomain;
import com.scaffold.common.core.web.page.TableDataInfo;
import com.scaffold.common.log.annotation.Log;
import com.scaffold.common.log.enums.BusinessType;
import com.scaffold.common.security.annotation.RequiresPermissions;
import com.scaffold.common.security.utils.SecurityUtils;
import com.scaffold.system.api.SysDictDataResource;
import com.scaffold.system.api.domain.SysDictData;
import com.scaffold.system.service.ISysDictDataService;
import com.scaffold.system.service.ISysDictTypeService;

/**
 * 字典数据信息服务实现（Triple REST）
 *
 * @author scaffold
 */
@DubboService
public class SysDictDataResourceImpl extends BaseController implements SysDictDataResource
{
    private final ISysDictDataService dictDataService;
    private final ISysDictTypeService dictTypeService;

    public SysDictDataResourceImpl(ISysDictDataService dictDataService, ISysDictTypeService dictTypeService)
    {
        this.dictDataService = dictDataService;
        this.dictTypeService = dictTypeService;
    }

    @RequiresPermissions("system:dict:list")
    @Override
    public TableDataInfo list(Integer pageNum, Integer pageSize, String orderByColumn, String isAsc, String dictType, String dictLabel, String status)
    {
        PageDomain page = new PageDomain();
        page.setPageNum(pageNum);
        page.setPageSize(pageSize);
        page.setOrderByColumn(orderByColumn);
        page.setIsAsc(isAsc);
        SysDictData dictData = new SysDictData();
        dictData.setDictType(dictType);
        dictData.setDictLabel(dictLabel);
        dictData.setStatus(status);
        return dictDataService.selectDictDataPage(dictData, page);
    }

    @Log(title = "字典数据", businessType = BusinessType.EXPORT)
    @RequiresPermissions("system:dict:export")
    @Override
    public byte[] export(String dictType, String dictLabel, String status)
    {
        SysDictData dictData = new SysDictData();
        dictData.setDictType(dictType);
        dictData.setDictLabel(dictLabel);
        dictData.setStatus(status);
        List<SysDictData> list = dictDataService.selectDictDataList(dictData);
        ExcelUtil<SysDictData> util = new ExcelUtil<SysDictData>(SysDictData.class);
        return util.exportExcel(list, "字典数据");
    }

    @RequiresPermissions("system:dict:query")
    @Override
    public AjaxResult getInfo(@PathVariable Long dictCode)
    {
        return success(dictDataService.selectDictDataById(dictCode));
    }

    @Override
    public AjaxResult dictType(@PathVariable String dictType)
    {
        List<SysDictData> data = dictTypeService.selectDictDataByType(dictType);
        if (StringUtils.isNull(data))
        {
            data = new ArrayList<SysDictData>();
        }
        return success(data);
    }

    @RequiresPermissions("system:dict:add")
    @Log(title = "字典数据", businessType = BusinessType.INSERT)
    @Override
    public AjaxResult add(SysDictData dict)
    {
        dict.setCreateBy(SecurityUtils.getUsername());
        return toAjax(dictDataService.insertDictData(dict));
    }

    @RequiresPermissions("system:dict:edit")
    @Log(title = "字典数据", businessType = BusinessType.UPDATE)
    @Override
    public AjaxResult edit(SysDictData dict)
    {
        dict.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(dictDataService.updateDictData(dict));
    }

    @RequiresPermissions("system:dict:remove")
    @Log(title = "字典类型", businessType = BusinessType.DELETE)
    @Override
    public AjaxResult remove(@PathVariable String dictCodes)
    {
        dictDataService.deleteDictDataByIds(Convert.toLongArray(dictCodes));
        return success();
    }
}