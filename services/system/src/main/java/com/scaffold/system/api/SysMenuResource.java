package com.scaffold.system.api;

import org.springframework.web.bind.annotation.ModelAttribute;
import java.util.Map;
import com.scaffold.common.core.web.domain.AjaxResult;
import com.scaffold.system.domain.SysMenu;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RequestMapping("/menu")
public interface SysMenuResource
{
    @GetMapping("/list")
    AjaxResult list(@ModelAttribute SysMenu menu);

    @GetMapping("/{menuId}")
    AjaxResult getInfo(@PathVariable("menuId") Long menuId);

    @GetMapping("/treeselect")
    AjaxResult treeselect(@ModelAttribute SysMenu menu);

    @GetMapping("/roleMenuTreeselect/{roleId}")
    AjaxResult roleMenuTreeselect(@PathVariable("roleId") Long roleId);

    @PostMapping
    AjaxResult add(@RequestBody SysMenu menu);

    @PutMapping
    AjaxResult edit(@RequestBody SysMenu menu);

    @PutMapping("/updateSort")
    AjaxResult updateSort(@RequestBody Map<String, String> params);

    @DeleteMapping("/{menuId}")
    AjaxResult remove(@PathVariable("menuId") Long menuId);

    @GetMapping("/getRouters")
    AjaxResult getRouters();
}