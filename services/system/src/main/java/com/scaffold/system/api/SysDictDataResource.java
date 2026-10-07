package com.scaffold.system.api;

import java.util.List;
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
import com.scaffold.system.api.domain.SysDictData;

/**
 * 字典数据信息服务（Triple REST 对外接口）
 *
 * @author scaffold
 */
@RequestMapping("/dict/data")
public interface SysDictDataResource
{
    /**
     * 获取字典数据列表
     */
    @GetMapping("/list")
    TableDataInfo list(@RequestParam(value = "pageNum", required = false) Integer pageNum,
                       @RequestParam(value = "pageSize", required = false) Integer pageSize,
                       @RequestParam(value = "orderByColumn", required = false) String orderByColumn,
                       @RequestParam(value = "isAsc", required = false) String isAsc,
                       @RequestParam(value = "dictType", required = false) String dictType,
                       @RequestParam(value = "dictLabel", required = false) String dictLabel,
                       @RequestParam(value = "status", required = false) String status);

    /**
     * 导出字典数据列表
     */
    @PostMapping("/export")
    @RequestMapping(value = "/export", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    byte[] export(@RequestParam(value = "dictType", required = false) String dictType,
                  @RequestParam(value = "dictLabel", required = false) String dictLabel,
                  @RequestParam(value = "status", required = false) String status);

    /**
     * 根据字典编码获取详细信息
     */
    @GetMapping("/{dictCode}")
    AjaxResult getInfo(@PathVariable("dictCode") Long dictCode);

    /**
     * 根据字典类型获取字典数据
     */
    @GetMapping("/type/{dictType}")
    AjaxResult dictType(@PathVariable("dictType") String dictType);

    /**
     * 新增字典数据
     */
    @PostMapping
    AjaxResult add(@RequestBody SysDictData dict);

    /**
     * 修改字典数据
     */
    @PutMapping
    AjaxResult edit(@RequestBody SysDictData dict);

    /**
     * 删除字典数据
     */
    @DeleteMapping("/{dictCodes}")
    AjaxResult remove(@PathVariable("dictCodes") String dictCodes);
}