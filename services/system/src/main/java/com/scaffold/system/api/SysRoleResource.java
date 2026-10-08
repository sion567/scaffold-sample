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
import com.scaffold.common.core.web.page.PageDomain;
import com.scaffold.common.core.web.page.TableDataInfo;
import com.scaffold.system.api.domain.SysDept;
import com.scaffold.system.api.domain.SysRole;
import com.scaffold.system.api.domain.SysUser;
import com.scaffold.system.domain.SysUserRole;

/**
 * 角色信息服务（Triple REST 对外接口）
 *
 * @author scaffold
 */
@RequestMapping("/role")
public interface SysRoleResource
{
    /**
     * 获取角色列表
     *
     * <p>过滤字段摊平为 @RequestParam：Dubbo Triple REST 的 ModelAttributeArgumentResolver
     * 会读取注解的 required 属性，而 Spring {@code @ModelAttribute} 无该属性，显式标注必 500
     * （本服务无内嵌 Web 容器，HTTP 由 Triple REST 承载）。</p>
     */
    @GetMapping("/list")
    TableDataInfo list(@RequestParam(value = "pageNum", required = false) Integer pageNum,
                       @RequestParam(value = "pageSize", required = false) Integer pageSize,
                       @RequestParam(value = "orderByColumn", required = false) String orderByColumn,
                       @RequestParam(value = "isAsc", required = false) String isAsc,
                       @RequestParam(value = "roleName", required = false) String roleName,
                       @RequestParam(value = "roleKey", required = false) String roleKey,
                       @RequestParam(value = "status", required = false) String status,
                       @RequestParam(value = "beginTime", required = false) String beginTime,
                       @RequestParam(value = "endTime", required = false) String endTime);

    /**
     * 导出角色列表
     */
    @PostMapping("/export")
    @RequestMapping(value = "/export", produces = "application/octet-stream")
    byte[] export(@RequestBody SysRole role);

    /**
     * 根据角色编号获取详细信息
     */
    @GetMapping("/{roleId}")
    AjaxResult getInfo(@PathVariable("roleId") Long roleId);

    /**
     * 新增角色
     */
    @PostMapping
    AjaxResult add(@RequestBody SysRole role);

    /**
     * 修改角色
     */
    @PutMapping
    AjaxResult edit(@RequestBody SysRole role);

    /**
     * 修改角色数据权限
     */
    @PutMapping("/dataScope")
    AjaxResult dataScope(@RequestBody SysRole role);

    /**
     * 修改角色状态
     */
    @PutMapping("/changeStatus")
    AjaxResult changeStatus(@RequestBody SysRole role);

    /**
     * 删除角色
     */
    @DeleteMapping("/{roleIds}")
    AjaxResult remove(@PathVariable("roleIds") Long[] roleIds);

    /**
     * 获取角色选择框列表
     */
    @GetMapping("/optionselect")
    AjaxResult optionselect();

    /**
     * 获取已分配用户列表（过滤字段摊平为 @RequestParam，原因见 {@link #list}）
     */
    @GetMapping("/authUser/allocatedList")
    TableDataInfo allocatedList(@RequestParam(value = "pageNum", required = false) Integer pageNum,
                                @RequestParam(value = "pageSize", required = false) Integer pageSize,
                                @RequestParam(value = "orderByColumn", required = false) String orderByColumn,
                                @RequestParam(value = "isAsc", required = false) String isAsc,
                                @RequestParam(value = "roleId", required = false) Long roleId,
                                @RequestParam(value = "userName", required = false) String userName,
                                @RequestParam(value = "phonenumber", required = false) String phonenumber);

    /**
     * 获取未分配用户列表（过滤字段摊平为 @RequestParam，原因见 {@link #list}）
     */
    @GetMapping("/authUser/unallocatedList")
    TableDataInfo unallocatedList(@RequestParam(value = "pageNum", required = false) Integer pageNum,
                                  @RequestParam(value = "pageSize", required = false) Integer pageSize,
                                  @RequestParam(value = "orderByColumn", required = false) String orderByColumn,
                                  @RequestParam(value = "isAsc", required = false) String isAsc,
                                  @RequestParam(value = "roleId", required = false) Long roleId,
                                  @RequestParam(value = "userName", required = false) String userName,
                                  @RequestParam(value = "phonenumber", required = false) String phonenumber);

    /**
     * 取消授权用户
     */
    @PutMapping("/authUser/cancel")
    AjaxResult cancelAuthUser(@RequestBody SysUserRole userRole);

    /**
     * 批量取消授权用户
     */
    @PutMapping("/authUser/cancelAll")
    AjaxResult cancelAuthUserAll(@RequestParam(value = "roleId") Long roleId, @RequestParam(value = "userIds") Long[] userIds);

    /**
     * 批量授权用户
     */
    @PutMapping("/authUser/selectAll")
    AjaxResult selectAuthUserAll(@RequestParam(value = "roleId") Long roleId, @RequestParam(value = "userIds") Long[] userIds);

    /**
     * 获取角色部门权限树
     */
    @GetMapping("/deptTree/{roleId}")
    AjaxResult deptTree(@PathVariable("roleId") Long roleId);
}
