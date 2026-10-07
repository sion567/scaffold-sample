package com.scaffold.sample.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.scaffold.common.core.web.controller.BaseController;
import com.scaffold.common.core.web.domain.AjaxResult;
import com.scaffold.common.core.web.page.TableDataInfo;
import com.scaffold.common.log.annotation.Log;
import com.scaffold.common.log.enums.BusinessType;
import com.scaffold.common.security.annotation.RequiresPermissions;
import com.scaffold.common.security.utils.SecurityUtils;
import com.scaffold.sample.domain.SampleOrder;
import com.scaffold.sample.service.ISampleOrderService;

/**
 * 样例订单 Controller（主子表 + 工作流审批案例）
 *
 * @author scaffold
 */
@RestController
@RequestMapping("/sample/order")
public class SampleOrderController extends BaseController
{
    @Autowired
    private ISampleOrderService orderService;

    /** 查询订单列表 */
    @RequiresPermissions("sample:order:list")
    @GetMapping("/list")
    public TableDataInfo list(SampleOrder query)
    {
        // 样例演示：整表查询（分页请用 JpaSpecs + repository.page，参考 system 服务）
        return getDataTable(orderService.selectList(query));
    }

    /** 获取订单详情（含明细） */
    @RequiresPermissions("sample:order:query")
    @GetMapping(value = "/{orderId}")
    public AjaxResult getInfo(@PathVariable("orderId") Long orderId)
    {
        return success(orderService.selectByOrderId(orderId));
    }

    /** 新增订单（含明细，主子表同事务） */
    @RequiresPermissions("sample:order:add")
    @Log(title = "样例订单", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SampleOrder order)
    {
        return toAjax(orderService.insert(order));
    }

    /** 修改订单（明细全删全插） */
    @RequiresPermissions("sample:order:edit")
    @Log(title = "样例订单", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SampleOrder order)
    {
        return toAjax(orderService.update(order));
    }

    /** 删除订单 */
    @RequiresPermissions("sample:order:remove")
    @Log(title = "样例订单", businessType = BusinessType.DELETE)
    @DeleteMapping("/{orderIds}")
    public AjaxResult remove(@PathVariable Long[] orderIds)
    {
        return toAjax(orderService.deleteByOrderIds(orderIds));
    }

    /** 提交订单进入审批流 */
    @RequiresPermissions("sample:order:submit")
    @Log(title = "样例订单-提交审批", businessType = BusinessType.UPDATE)
    @PostMapping("/submit/{orderId}")
    public AjaxResult submit(@PathVariable Long orderId)
    {
        return success(orderService.submit(orderId, SecurityUtils.getUsername()));
    }

    /** 审批订单：pass=true 通过办结，false 相邻退回提交人 */
    @RequiresPermissions("sample:order:audit")
    @Log(title = "样例订单-审批", businessType = BusinessType.UPDATE)
    @PostMapping("/audit/{orderId}")
    public AjaxResult audit(@PathVariable Long orderId,
            @RequestParam(defaultValue = "true") boolean pass,
            @RequestParam(required = false) String remark)
    {
        return success(orderService.audit(orderId, pass, remark, SecurityUtils.getUsername()));
    }

    /** 查询订单审批流转历史 */
    @RequiresPermissions("sample:order:query")
    @GetMapping("/flow/{orderId}")
    public AjaxResult flowHistory(@PathVariable Long orderId)
    {
        return success(orderService.flowHistory(orderId));
    }
}
