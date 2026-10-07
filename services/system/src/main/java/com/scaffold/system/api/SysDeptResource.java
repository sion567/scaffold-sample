package com.scaffold.system.api;

import org.springframework.web.bind.annotation.ModelAttribute;
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
    @GetMapping("/list")
    AjaxResult list(@ModelAttribute SysDept dept);

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