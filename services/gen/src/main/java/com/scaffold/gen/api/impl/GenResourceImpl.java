package com.scaffold.gen.api.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.apache.dubbo.config.annotation.DubboService;
import com.scaffold.common.core.text.Convert;
import com.scaffold.common.core.web.controller.BaseController;
import com.scaffold.common.core.web.domain.AjaxResult;
import com.scaffold.common.core.web.page.PageDomain;
import com.scaffold.common.core.web.page.TableDataInfo;
import com.scaffold.common.log.annotation.Log;
import com.scaffold.common.log.enums.BusinessType;
import com.scaffold.common.security.annotation.RequiresPermissions;
import com.scaffold.gen.api.GenResource;
import com.scaffold.gen.config.GenConfig;
import com.scaffold.gen.domain.GenTable;
import com.scaffold.gen.domain.GenTableColumn;
import com.scaffold.gen.service.IGenTableColumnService;
import com.scaffold.gen.service.IGenTableService;

/**
 * 代码生成信息服务实现（Triple REST）
 *
 * @author ct
 */
@DubboService
public class GenResourceImpl extends BaseController implements GenResource
{
    private final IGenTableService genTableService;
    private final IGenTableColumnService genTableColumnService;

    public GenResourceImpl(IGenTableService genTableService, IGenTableColumnService genTableColumnService)
    {
        this.genTableService = genTableService;
        this.genTableColumnService = genTableColumnService;
    }

    @RequiresPermissions("tool:gen:list")
    @Override
    public TableDataInfo genList(Integer pageNum, Integer pageSize, String orderByColumn, String isAsc, GenTable genTable)
    {
        PageDomain page = new PageDomain();
        page.setPageNum(pageNum);
        page.setPageSize(pageSize);
        page.setOrderByColumn(orderByColumn);
        page.setIsAsc(isAsc);
        return genTableService.selectGenTablePage(genTable, page);
    }

    @RequiresPermissions("tool:gen:query")
    @Override
    public AjaxResult getInfo(Long tableId)
    {
        GenTable table = genTableService.selectGenTableById(tableId);
        List<GenTable> tables = genTableService.selectGenTableAll();
        List<GenTableColumn> list = genTableColumnService.selectGenTableColumnListByTableId(tableId);
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("info", table);
        map.put("rows", list);
        map.put("tables", tables);
        return success(map);
    }

    @RequiresPermissions("tool:gen:list")
    @Override
    public TableDataInfo dataList(Integer pageNum, Integer pageSize, String orderByColumn, String isAsc, GenTable genTable)
    {
        PageDomain page = new PageDomain();
        page.setPageNum(pageNum);
        page.setPageSize(pageSize);
        page.setOrderByColumn(orderByColumn);
        page.setIsAsc(isAsc);
        List<GenTable> list = genTableService.selectDbTableList(genTable);
        return pageOf(list, page);
    }

    @Override
    public TableDataInfo columnList(Long tableId)
    {
        TableDataInfo dataInfo = new TableDataInfo();
        List<GenTableColumn> list = genTableColumnService.selectGenTableColumnListByTableId(tableId);
        dataInfo.setRows(list);
        dataInfo.setTotal(list.size());
        return dataInfo;
    }

    @RequiresPermissions("tool:gen:import")
    @Log(title = "代码生成", businessType = BusinessType.IMPORT)
    @Override
    public AjaxResult importTableSave(String tables, String tplWebType)
    {
        String[] tableNames = Convert.toStrArray(tables);
        List<GenTable> tableList = genTableService.selectDbTableListByNames(tableNames);
        genTableService.importGenTable(tableList, tplWebType);
        return success();
    }

    @RequiresPermissions("tool:gen:edit")
    @Log(title = "代码生成", businessType = BusinessType.UPDATE)
    @Override
    public AjaxResult editSave(GenTable genTable)
    {
        genTableService.validateEdit(genTable);
        genTableService.updateGenTable(genTable);
        return success();
    }

    @RequiresPermissions("tool:gen:remove")
    @Log(title = "代码生成", businessType = BusinessType.DELETE)
    @Override
    public AjaxResult remove(Long[] tableIds)
    {
        genTableService.deleteGenTableByIds(tableIds);
        return success();
    }

    @RequiresPermissions("tool:gen:preview")
    @Override
    public AjaxResult preview(Long tableId)
    {
        Map<String, String> dataMap = genTableService.previewCode(tableId);
        return success(dataMap);
    }

    @RequiresPermissions("tool:gen:code")
    @Log(title = "代码生成", businessType = BusinessType.GENCODE)
    @Override
    public byte[] download(String tableName)
    {
        return genTableService.downloadCode(tableName);
    }

    @RequiresPermissions("tool:gen:code")
    @Log(title = "代码生成", businessType = BusinessType.GENCODE)
    @Override
    public AjaxResult genCode(String tableName)
    {
        if (!GenConfig.isAllowOverwrite())
        {
            return AjaxResult.error("【系统预设】不允许生成文件覆盖到本地");
        }
        genTableService.generatorCode(tableName);
        return success();
    }

    @RequiresPermissions("tool:gen:edit")
    @Log(title = "代码生成", businessType = BusinessType.UPDATE)
    @Override
    public AjaxResult synchDb(String tableName)
    {
        genTableService.synchDb(tableName);
        return success();
    }

    @RequiresPermissions("tool:gen:code")
    @Log(title = "代码生成", businessType = BusinessType.GENCODE)
    @Override
    public byte[] batchGenCode(String tables)
    {
        String[] tableNames = Convert.toStrArray(tables);
        return genTableService.downloadCode(tableNames);
    }

    /**
     * 目录查询结果内存分页（逆向元数据列表量级小；排序沿用 SQL 内建顺序，
     * 请求带 orderByColumn 时按属性名排序）。
     */
    private TableDataInfo pageOf(List<GenTable> list, PageDomain page)
    {
        if (page != null && page.getOrderByColumn() != null && !page.getOrderByColumn().isEmpty())
        {
            String prop = page.getOrderByColumn();
            boolean asc = !"desc".equalsIgnoreCase(page.getIsAsc());
            java.util.Comparator<GenTable> cmp = java.util.Comparator.comparing(
                    t -> {
                        Object v = valueOf(t, prop);
                        return (Comparable) (v == null ? "" : v);
                    });
            list.sort(asc ? cmp : cmp.reversed());
        }
        int pageNum = page == null || page.getPageNum() == null ? 1 : page.getPageNum();
        int pageSize = page == null || page.getPageSize() == null ? 10 : page.getPageSize();
        int from = Math.max((pageNum - 1) * pageSize, 0);
        int to = Math.min(from + pageSize, list.size());
        TableDataInfo dataInfo = new TableDataInfo();
        dataInfo.setTotal(list.size());
        dataInfo.setRows(from >= to ? java.util.Collections.emptyList() : new java.util.ArrayList<>(list.subList(from, to)));
        return dataInfo;
    }

    private static Object valueOf(GenTable t, String prop)
    {
        try
        {
            java.beans.PropertyDescriptor pd = new java.beans.PropertyDescriptor(prop, GenTable.class);
            return pd.getReadMethod().invoke(t);
        }
        catch (Exception e)
        {
            return null;
        }
    }
}