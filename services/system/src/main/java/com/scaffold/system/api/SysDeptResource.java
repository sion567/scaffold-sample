package com.scaffold.system.api;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.Map;
import com.scaffold.common.core.web.domain.AjaxResult;
import com.scaffold.system.api.domain.SysDept;

@RequestMapping("/dept")
public interface SysDeptResource
{
    /**
     * 部门列表（过滤字段摊平为 @RequestParam：Dubbo Triple REST 对显式
     * {@code @ModelAttribute} 会误读 required 属性而 500，参见 SysRoleResource#list）
     */
    @GetMapping("/list")
    AjaxResult list(@RequestParam(value = "deptName", required = false) String deptName,
                    @RequestParam(value = "status", required = false) String status);

    @GetMapping("/list/exclude/{deptId}")
    AjaxResult excludeChild(@PathVariable("deptId") Long deptId);

    @GetMapping("/{deptId}")
    AjaxResult getInfo(@PathVariable("deptId") Long deptId);

    @PostMapping
    AjaxResult add(@RequestBody SysDept dept);

    @PutMapping
    AjaxResult edit(@RequestBody SysDept dept);

    @PutMapping("/updateSort")
    AjaxResult updateSort(@RequestBody Map<String, String> params);

    @DeleteMapping("/{deptId}")
    AjaxResult remove(@PathVariable("deptId") Long deptId);
}