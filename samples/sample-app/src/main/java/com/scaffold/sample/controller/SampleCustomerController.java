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
import com.scaffold.sample.domain.SampleCustomer;
import com.scaffold.sample.service.ISampleCustomerService;

/**
 * 样例客户 Controller（单表 CRUD 案例，与 gen 生成物同构）
 *
 * @author scaffold
 */
@RestController
@RequestMapping("/sample/customer")
public class SampleCustomerController extends BaseController
{
    @Autowired
    private ISampleCustomerService customerService;

    /** 查询客户列表 */
    @RequiresPermissions("sample:customer:list")
    @GetMapping("/list")
    public TableDataInfo list(SampleCustomer query)
    {
        List<SampleCustomer> list = customerService.selectList(query);
        return getDataTable(list);
    }

    /** 导出客户列表 */
    @RequiresPermissions("sample:customer:export")
    @Log(title = "样例客户", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SampleCustomer query)
    {
        List<SampleCustomer> list = customerService.selectList(query);
        ExcelUtil<SampleCustomer> util = new ExcelUtil<>(SampleCustomer.class);
        util.exportExcel(response, list, "样例客户");
    }

    /** 获取客户详情 */
    @RequiresPermissions("sample:customer:query")
    @GetMapping(value = "/{customerId}")
    public AjaxResult getInfo(@PathVariable("customerId") Long customerId)
    {
        return success(customerService.selectByCustomerId(customerId));
    }

    /** 新增客户 */
    @RequiresPermissions("sample:customer:add")
    @Log(title = "样例客户", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SampleCustomer customer)
    {
        return toAjax(customerService.insert(customer));
    }

    /** 修改客户 */
    @RequiresPermissions("sample:customer:edit")
    @Log(title = "样例客户", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SampleCustomer customer)
    {
        return toAjax(customerService.update(customer));
    }

    /** 删除客户 */
    @RequiresPermissions("sample:customer:remove")
    @Log(title = "样例客户", businessType = BusinessType.DELETE)
    @DeleteMapping("/{customerIds}")
    public AjaxResult remove(@PathVariable Long[] customerIds)
    {
        return toAjax(customerService.deleteByCustomerIds(customerIds));
    }
}
