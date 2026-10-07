package com.scaffold.gen.api;

import java.util.Map;
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
import com.scaffold.gen.domain.GenTable;
import com.scaffold.gen.domain.GenTableColumn;

/**
 * 代码生成信息服务（Triple REST 对外接口）
 *
 * @author ct
 */
@RequestMapping("/tool/gen")
public interface GenResource
{
    /**
     * 查询代码生成列表
     */
    @GetMapping("/list")
    TableDataInfo genList(@RequestParam(value = "pageNum", required = false) Integer pageNum,
                          @RequestParam(value = "pageSize", required = false) Integer pageSize,
                          @RequestParam(value = "orderByColumn", required = false) String orderByColumn,
                          @RequestParam(value = "isAsc", required = false) String isAsc,
                          @RequestBody GenTable genTable);

    /**
     * 获取代码生成信息
     */
    @GetMapping("/{tableId}")
    AjaxResult getInfo(@PathVariable("tableId") Long tableId);

    /**
     * 查询数据库列表
     */
    @GetMapping("/db/list")
    TableDataInfo dataList(@RequestParam(value = "pageNum", required = false) Integer pageNum,
                          @RequestParam(value = "pageSize", required = false) Integer pageSize,
                          @RequestParam(value = "orderByColumn", required = false) String orderByColumn,
                          @RequestParam(value = "isAsc", required = false) String isAsc,
                          @RequestBody GenTable genTable);

    /**
     * 查询数据表字段列表
     */
    @GetMapping("/column/{tableId}")
    TableDataInfo columnList(@PathVariable("tableId") Long tableId);

    /**
     * 导入表结构（保存）
     */
    @PostMapping("/importTable")
    AjaxResult importTableSave(@RequestParam(value = "tables") String tables, @RequestParam(value = "tplWebType", required = false) String tplWebType);

    /**
     * 修改保存代码生成业务
     */
    @PutMapping
    AjaxResult editSave(@RequestBody GenTable genTable);

    /**
     * 删除代码生成
     */
    @DeleteMapping("/{tableIds}")
    AjaxResult remove(@RequestParam("tableIds") Long[] tableIds);

    /**
     * 预览代码
     */
    @GetMapping("/preview/{tableId}")
    AjaxResult preview(@PathVariable("tableId") Long tableId);

    /**
     * 生成代码（下载方式）
     */
    @GetMapping("/download/{tableName}")
    @RequestMapping(value = "/download/{tableName}", produces = "application/octet-stream")
    byte[] download(@PathVariable("tableName") String tableName);

    /**
     * 生成代码（自定义路径）
     */
    @GetMapping("/genCode/{tableName}")
    AjaxResult genCode(@PathVariable("tableName") String tableName);

    /**
     * 同步数据库
     */
    @GetMapping("/synchDb/{tableName}")
    AjaxResult synchDb(@PathVariable("tableName") String tableName);

    /**
     * 批量生成代码
     */
    @GetMapping("/batchGenCode")
    @RequestMapping(value = "/batchGenCode", produces = "application/octet-stream")
    byte[] batchGenCode(@RequestParam(value = "tables") String tables);
}
