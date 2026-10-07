package com.scaffold.system.api.impl;

import java.util.List;
import org.springframework.web.bind.annotation.RequestParam;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
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
import com.scaffold.system.api.SysPostResource;
import com.scaffold.system.domain.SysPost;
import com.scaffold.system.service.ISysPostService;

/**
 * 岗位信息服务实现（Triple REST）
 *
 * @author scaffold
 */
@DubboService
public class SysPostResourceImpl extends BaseController implements SysPostResource
{
    private final ISysPostService postService;

    public SysPostResourceImpl(ISysPostService postService) {
        this.postService = postService;
    }

    @RequiresPermissions("system:post:list")
    @Override
    public TableDataInfo list(Integer pageNum, Integer pageSize, String orderByColumn, String isAsc, String postCode, String postName, String status)
    {
        PageDomain page = new PageDomain();
        page.setPageNum(pageNum);
        page.setPageSize(pageSize);
        page.setOrderByColumn(orderByColumn);
        page.setIsAsc(isAsc);
        SysPost post = new SysPost();
        post.setPostCode(postCode);
        post.setPostName(postName);
        post.setStatus(status);
        return postService.selectPostPage(post, page);
    }

    @Log(title = "岗位管理", businessType = BusinessType.EXPORT)
    @RequiresPermissions("system:post:export")
    @Override
    public byte[] export(String postCode, String postName, String status)
    {
        SysPost post = new SysPost();
        post.setPostCode(postCode);
        post.setPostName(postName);
        post.setStatus(status);
        List<SysPost> list = postService.selectPostList(post);
        ExcelUtil<SysPost> util = new ExcelUtil<SysPost>(SysPost.class);
        return util.exportExcel(list, "岗位数据");
    }

    @RequiresPermissions("system:post:query")
    @Override
    public AjaxResult getInfo(Long postId)
    {
        return success(postService.selectPostById(postId));
    }

    @RequiresPermissions("system:post:add")
    @Log(title = "岗位管理", businessType = BusinessType.INSERT)
    @Override
    public AjaxResult add(SysPost post)
    {
        if (!postService.checkPostNameUnique(post))
        {
            return error("新增岗位'" + post.getPostName() + "'失败，岗位名称已存在");
        }
        else if (!postService.checkPostCodeUnique(post))
        {
            return error("新增岗位'" + post.getPostName() + "'失败，岗位编码已存在");
        }
        post.setCreateBy(SecurityUtils.getUsername());
        return toAjax(postService.insertPost(post));
    }

    @RequiresPermissions("system:post:edit")
    @Log(title = "岗位管理", businessType = BusinessType.UPDATE)
    @Override
    public AjaxResult edit(SysPost post)
    {
        if (!postService.checkPostNameUnique(post))
        {
            return error("修改岗位'" + post.getPostName() + "'失败，岗位名称已存在");
        }
        else if (!postService.checkPostCodeUnique(post))
        {
            return error("修改岗位'" + post.getPostName() + "'失败，岗位编码已存在");
        }
        post.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(postService.updatePost(post));
    }

    @RequiresPermissions("system:post:remove")
    @Log(title = "岗位管理", businessType = BusinessType.DELETE)
    @Override
    public AjaxResult remove(String postIds)
    {
        return toAjax(postService.deletePostByIds(Convert.toLongArray(postIds)));
    }

    @Override
    public AjaxResult optionselect()
    {
        return success(postService.selectPostAll());
    }
}

