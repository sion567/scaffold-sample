package com.scaffold.system.api;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import com.scaffold.common.core.web.domain.AjaxResult;
import com.scaffold.common.core.web.page.PageDomain;
import com.scaffold.common.core.web.page.TableDataInfo;
import com.scaffold.system.api.domain.SysDept;
import com.scaffold.system.api.domain.SysUser;

/**
 * 用户管理（Triple REST 对外接口）
 *
 * @author scaffold
 */
@RequestMapping("/user")
public interface SysUserResource
{
    /**
     * 获取用户列表
     */
    @GetMapping("/list")
    TableDataInfo list(@RequestParam(value = "pageNum", required = false) Integer pageNum,
                       @RequestParam(value = "pageSize", required = false) Integer pageSize,
                       @RequestParam(value = "orderByColumn", required = false) String orderByColumn,
                       @RequestParam(value = "isAsc", required = false) String isAsc,
                       @RequestParam(value = "userId", required = false) Long userId,
                       @RequestParam(value = "userName", required = false) String userName,
                       @RequestParam(value = "phonenumber", required = false) String phonenumber,
                       @RequestParam(value = "status", required = false) String status,
                       @RequestParam(value = "deptId", required = false) Long deptId);

    /**
     * 导出用户列表
     */
    @PostMapping("/export")
    @RequestMapping(value = "/export", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    byte[] export(@RequestParam(value = "userId", required = false) Long userId,
                  @RequestParam(value = "userName", required = false) String userName,
                  @RequestParam(value = "phonenumber", required = false) String phonenumber,
                  @RequestParam(value = "status", required = false) String status,
                  @RequestParam(value = "deptId", required = false) Long deptId);

    /**
     * 导入用户数据
     */
    @PostMapping("/importData")
    AjaxResult importData(@RequestParam(value = "fileBase64") String fileBase64, @RequestParam(value = "updateSupport") boolean updateSupport) throws Exception;

    /**
     * 下载导入模板
     */
    @PostMapping("/importTemplate")
    @RequestMapping(value = "/importTemplate", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    byte[] importTemplate();

    /**
     * 获取当前用户信息
     */
    @GetMapping("/getInfo")
    AjaxResult getInfo();

    /**
     * 根据用户编号获取详细信息
     */
    @GetMapping("/{userId}")
    AjaxResult getUserInfo(@PathVariable("userId") Long userId);

    /**
     * 新增用户
     */
    @PostMapping
    AjaxResult add(@RequestBody SysUser user);

    /**
     * 修改用户
     */
    @PutMapping
    AjaxResult edit(@RequestBody SysUser user);

    /**
     * 删除用户
     */
    @DeleteMapping("/{userIds}")
    AjaxResult remove(@PathVariable("userIds") String userIds);

    /**
     * 重置密码
     */
    @PutMapping("/resetPwd")
    AjaxResult resetPwd(@RequestParam(value = "userId") Long userId, @RequestParam(value = "password") String password);

    /**
     * 状态修改
     */
    @PutMapping("/changeStatus")
    AjaxResult changeStatus(@RequestParam(value = "userId") Long userId, @RequestParam(value = "status") String status);

    /**
     * 根据用户编号获取授权角色
     */
    @GetMapping("/authRole/{userId}")
    AjaxResult authRole(@PathVariable("userId") Long userId);

    /**
     * 用户授权角色
     */
    @PutMapping("/authRole")
    AjaxResult insertAuthRole(@RequestParam(value = "userId") Long userId, @RequestParam(value = "roleIds") String roleIds);

    /**
     * 获取部门树列表
     */
    @GetMapping("/deptTree")
    AjaxResult deptTree(@RequestParam(value = "deptName", required = false) String deptName, @RequestParam(value = "status", required = false) String status);

    /**
     * 账户解锁（清除登录失败计数缓存；登录日志由审计日志服务负责）
     */
    @GetMapping("/unlock/{userName}")
    AjaxResult unlock(@PathVariable("userName") String userName);
}
