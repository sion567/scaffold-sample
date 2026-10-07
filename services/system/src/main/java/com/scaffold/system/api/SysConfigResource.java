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
import com.scaffold.system.domain.SysConfig;

/**
 * 参数配置信息服务（Triple REST 对外接口）
 *
 * @author ct
 */
@RequestMapping("/config")
public interface SysConfigResource
{
    /**
     * 获取参数配置列表
     */
    @GetMapping("/list")
    TableDataInfo list(@RequestParam(value = "pageNum", required = false) Integer pageNum,
                       @RequestParam(value = "pageSize", required = false) Integer pageSize,
                       @RequestParam(value = "orderByColumn", required = false) String orderByColumn,
                       @RequestParam(value = "isAsc", required = false) String isAsc,
                       @RequestParam(value = "configName", required = false) String configName,
                       @RequestParam(value = "configKey", required = false) String configKey,
                       @RequestParam(value = "configType", required = false) String configType);

    /**
     * 导出参数配置列表
     */
    @PostMapping("/export")
    @RequestMapping(value = "/export", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    byte[] export(@RequestParam(value = "configName", required = false) String configName,
                  @RequestParam(value = "configKey", required = false) String configKey,
                  @RequestParam(value = "configType", required = false) String configType);

    /**
     * 根据参数编号获取详细信息
     */
    @GetMapping("/{configId}")
    AjaxResult getInfo(@PathVariable("configId") Long configId);

    /**
     * 根据参数键名查询参数值
     */
    @GetMapping("/configKey/{configKey}")
    AjaxResult getConfigKey(@PathVariable("configKey") String configKey);

    /**
     * 新增参数配置
     */
    @PostMapping
    AjaxResult add(@RequestBody SysConfig config);

    /**
     * 修改参数配置
     */
    @PutMapping
    AjaxResult edit(@RequestBody SysConfig config);

    /**
     * 删除参数配置
     */
    @DeleteMapping("/{configIds}")
    AjaxResult remove(@PathVariable("configIds") String configIds);

    /**
     * 刷新参数缓存
     */
    @DeleteMapping("/refreshCache")
    AjaxResult refreshCache();
}