package com.scaffold.system.api;

import java.util.Map;
import com.scaffold.common.core.web.domain.AjaxResult;
import com.scaffold.system.domain.SysMenu;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;

@RequestMapping("/menu")
public interface SysMenuResource
{
    /**
     * 菜单列表（过滤字段摊平为 @RequestParam：Dubbo Triple REST 对显式
     * {@code @ModelAttribute} 会误读 required 属性而 500，参见 SysRoleResource#list）
     */
    @GetMapping("/list")
    AjaxResult list(@RequestParam(value = "menuName", required = false) String menuName,
                    @RequestParam(value = "status", required = false) String status);

    @GetMapping("/{menuId}")
    AjaxResult getInfo(@PathVariable("menuId") Long menuId);

    /** 菜单树选择（过滤字段摊平为 @RequestParam，原因见 {@link #list}） */
    @GetMapping("/treeselect")
    AjaxResult treeselect(@RequestParam(value = "menuName", required = false) String menuName,
                          @RequestParam(value = "status", required = false) String status);

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