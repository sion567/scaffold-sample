package com.scaffold.sample.controller;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.scaffold.common.core.utils.poi.ExcelUtil;
import com.scaffold.common.core.web.controller.BaseController;
import com.scaffold.common.core.web.domain.AjaxResult;
import com.scaffold.common.core.web.page.TableDataInfo;
import com.scaffold.common.log.annotation.Log;
import com.scaffold.common.log.enums.BusinessType;
import com.scaffold.common.security.annotation.RequiresPermissions;
import com.scaffold.sample.domain.SampleStock;
import com.scaffold.sample.service.ISampleStockService;

/**
 * 样例库存 Controller（单表 CRUD 案例）
 *
 * @author scaffold
 */
@RestController
@RequestMapping("/sample/stock")
public class SampleStockController extends BaseController
{
    @Autowired
    private ISampleStockService stockService;

    /** 查询库存列表 */
    @RequiresPermissions("sample:stock:list")
    @GetMapping("/list")
    public TableDataInfo list(SampleStock query)
    {
        // 样例演示：整表查询（分页请用 JpaSpecs + repository.page，参考 system 服务）
        return getDataTable(stockService.selectList(query));
    }

    /** 导出库存列表 */
    @RequiresPermissions("sample:stock:export")
    @Log(title = "样例库存", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SampleStock query)
    {
        List<SampleStock> list = stockService.selectList(query);
        ExcelUtil<SampleStock> util = new ExcelUtil<>(SampleStock.class);
        util.exportExcel(response, list, "样例库存");
    }

    /** 获取库存详情 */
    @RequiresPermissions("sample:stock:query")
    @GetMapping(value = "/{stockId}")
    public AjaxResult getInfo(@PathVariable("stockId") Long stockId)
    {
        return success(stockService.selectByStockId(stockId));
    }

    /** 新增库存 */
    @RequiresPermissions("sample:stock:add")
    @Log(title = "样例库存", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SampleStock stock)
    {
        return toAjax(stockService.insert(stock));
    }

    /** 修改库存 */
    @RequiresPermissions("sample:stock:edit")
    @Log(title = "样例库存", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SampleStock stock)
    {
        return toAjax(stockService.update(stock));
    }

    /** 删除库存 */
    @RequiresPermissions("sample:stock:remove")
    @Log(title = "样例库存", businessType = BusinessType.DELETE)
    @DeleteMapping("/{stockIds}")
    public AjaxResult remove(@PathVariable Long[] stockIds)
    {
        return toAjax(stockService.deleteByStockIds(stockIds));
    }
}
