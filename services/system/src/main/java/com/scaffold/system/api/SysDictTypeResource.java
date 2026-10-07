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
import com.scaffold.common.core.web.page.TableDataInfo;
import com.scaffold.system.api.domain.SysDictType;

/**
 * 字典类型信息服务（Triple REST 对外接口）
 *
 * @author scaffold
 */
@RequestMapping("/dict/type")
public interface SysDictTypeResource
{
    /**
     * 获取字典类型列表
     */
    @GetMapping("/list")
    TableDataInfo list(@RequestParam(value = "pageNum", required = false) Integer pageNum,
                       @RequestParam(value = "pageSize", required = false) Integer pageSize,
                       @RequestParam(value = "orderByColumn", required = false) String orderByColumn,
                       @RequestParam(value = "isAsc", required = false) String isAsc,
                       @RequestParam(value = "dictName", required = false) String dictName,
                       @RequestParam(value = "dictType", required = false) String dictType,
                       @RequestParam(value = "status", required = false) String status);

    /**
     * 导出字典类型列表
     */
    @PostMapping("/export")
    @RequestMapping(value = "/export", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    byte[] export(@RequestParam(value = "dictName", required = false) String dictName,
                  @RequestParam(value = "dictType", required = false) String dictType,
                  @RequestParam(value = "status", required = false) String status);

    /**
     * 根据字典编号获取详细信息
     */
    @GetMapping("/{dictId}")
    AjaxResult getInfo(@PathVariable("dictId") Long dictId);

    /**
     * 新增字典类型
     */
    @PostMapping
    AjaxResult add(@RequestBody SysDictType dict);

    /**
     * 修改字典类型
     */
    @PutMapping
    AjaxResult edit(@RequestBody SysDictType dict);

    /**
     * 删除字典类型
     */
    @DeleteMapping("/{dictIds}")
    AjaxResult remove(@PathVariable("dictIds") String dictIds);

    /**
     * 刷新字典缓存
     */
    @DeleteMapping("/refreshCache")
    AjaxResult refreshCache();

    /**
     * 获取字典选择框列表
     */
    @GetMapping("/optionselect")
    AjaxResult optionselect();
}