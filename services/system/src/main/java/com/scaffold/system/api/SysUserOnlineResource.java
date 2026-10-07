package com.scaffold.system.api;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.scaffold.common.core.web.domain.AjaxResult;
import com.scaffold.common.core.web.page.TableDataInfo;

/**
 * 在线用户信息服务（Triple REST 对外接口）
 *
 * @author ct
 */
@RequestMapping("/online")
public interface SysUserOnlineResource
{
    /**
     * 获取在线用户列表
     */
    @GetMapping("/list")
    TableDataInfo list(@RequestParam(value = "ipaddr", required = false) String ipaddr,
                       @RequestParam(value = "userName", required = false) String userName);

    /**
     * 强退用户
     */
    @DeleteMapping("/{tokenId}")
    AjaxResult forceLogout(@PathVariable("tokenId") String tokenId);
}