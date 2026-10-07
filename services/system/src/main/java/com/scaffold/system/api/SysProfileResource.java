package com.scaffold.system.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import com.scaffold.common.core.web.domain.AjaxResult;
import com.scaffold.system.api.domain.SysUser;

/**
 * 个人信息管理（Triple REST 对外接口）
 *
 * @author scaffold
 */
@RequestMapping("/user/profile")
public interface SysProfileResource
{
    /**
     * 个人信息
     */
    @GetMapping
    AjaxResult profile();

    /**
     * 修改用户
     */
    @PutMapping
    AjaxResult updateProfile(@RequestBody SysUser user);

    /**
     * 重置密码
     */
    @PutMapping("/updatePwd")
    AjaxResult updatePwd(@RequestParam("oldPassword") String oldPassword, @RequestParam("newPassword") String newPassword);

    /**
     * 头像上传
     */
    @PostMapping("/avatar")
    AjaxResult avatar(@RequestParam("bytes") String bytesBase64, @RequestParam("filename") String filename);
}