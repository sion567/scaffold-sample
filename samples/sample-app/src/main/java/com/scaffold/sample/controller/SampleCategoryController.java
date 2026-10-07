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
import org.springframework.web.bind.annotation.RestController;
import com.scaffold.common.core.web.controller.BaseController;
import com.scaffold.common.core.web.domain.AjaxResult;
import com.scaffold.common.log.annotation.Log;
import com.scaffold.common.log.enums.BusinessType;
import com.scaffold.common.security.annotation.RequiresPermissions;
import com.scaffold.sample.domain.SampleCategory;
import com.scaffold.sample.service.ISampleCategoryService;

/**
 * 样例商品分类 Controller（树表案例，与 gen 生成物同构）
 *
 * @author scaffold
 */
@RestController
@RequestMapping("/sample/category")
public class SampleCategoryController extends BaseController
{
    @Autowired
    private ISampleCategoryService categoryService;

    /** 查询分类列表（平铺） */
    @RequiresPermissions("sample:category:list")
    @GetMapping("/list")
    public AjaxResult list(SampleCategory query)
    {
        return success(categoryService.selectList(query));
    }

    /** 查询分类树 */
    @RequiresPermissions("sample:category:list")
    @GetMapping("/tree")
    public AjaxResult tree(SampleCategory query)
    {
        return success(categoryService.buildTree(categoryService.selectList(query)));
    }

    /** 获取分类详情 */
    @RequiresPermissions("sample:category:query")
    @GetMapping(value = "/{categoryId}")
    public AjaxResult getInfo(@PathVariable("categoryId") Long categoryId)
    {
        return success(categoryService.selectByCategoryId(categoryId));
    }

    /** 新增分类 */
    @RequiresPermissions("sample:category:add")
    @Log(title = "样例分类", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SampleCategory category)
    {
        return toAjax(categoryService.insert(category));
    }

    /** 修改分类 */
    @RequiresPermissions("sample:category:edit")
    @Log(title = "样例分类", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SampleCategory category)
    {
        return toAjax(categoryService.update(category));
    }

    /** 删除分类（有子分类时拒绝） */
    @RequiresPermissions("sample:category:remove")
    @Log(title = "样例分类", businessType = BusinessType.DELETE)
    @DeleteMapping("/{categoryId}")
    public AjaxResult remove(@PathVariable Long categoryId)
    {
        if ("1".equals(categoryService.checkCategoryHasChildren(categoryId)))
        {
            return warn("存在下级分类,不允许删除");
        }
        return toAjax(categoryService.deleteByCategoryId(categoryId));
    }
}
