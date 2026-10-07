package com.scaffold.system.api;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import com.scaffold.common.core.web.domain.AjaxResult;
import com.scaffold.common.core.web.page.PageDomain;
import com.scaffold.common.core.web.page.TableDataInfo;
import com.scaffold.system.domain.SysPost;

/**
 * 岗位信息服务（Triple REST 对外接口）
 *
 * @author scaffold
 */
@RequestMapping("/post")
public interface SysPostResource
{
    /**
     * 获取岗位列表
     */
    @GetMapping("/list")
    TableDataInfo list(@RequestParam(value = "pageNum", required = false) Integer pageNum,
                       @RequestParam(value = "pageSize", required = false) Integer pageSize,
                       @RequestParam(value = "orderByColumn", required = false) String orderByColumn,
                       @RequestParam(value = "isAsc", required = false) String isAsc,
                       @RequestParam(value = "postCode", required = false) String postCode,
                       @RequestParam(value = "postName", required = false) String postName,
                       @RequestParam(value = "status", required = false) String status);

    /**
     * 导出岗位列表
     */
    @PostMapping("/export")
    @RequestMapping(value = "/export", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    byte[] export(@RequestParam(value = "postCode", required = false) String postCode,
                  @RequestParam(value = "postName", required = false) String postName,
                  @RequestParam(value = "status", required = false) String status);

    /**
     * 根据岗位编号获取详细信息
     */
    @GetMapping("/{postId}")
    AjaxResult getInfo(@PathVariable("postId") Long postId);

    /**
     * 新增岗位
     */
    @PostMapping
    AjaxResult add(@RequestBody SysPost post);

    /**
     * 修改岗位
     */
    @PutMapping
    AjaxResult edit(@RequestBody SysPost post);

    /**
     * 删除岗位
     */
    @DeleteMapping("/{postIds}")
    AjaxResult remove(@PathVariable("postIds") String postIds);

    /**
     * 获取岗位选择框列表
     */
    @GetMapping("/optionselect")
    AjaxResult optionselect();
}

