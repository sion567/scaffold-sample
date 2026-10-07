package com.scaffold.system.api.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.springframework.web.bind.annotation.PathVariable;
import org.apache.dubbo.config.annotation.DubboService;
import com.scaffold.common.core.constant.CacheConstants;
import com.scaffold.common.core.utils.StringUtils;
import com.scaffold.common.core.web.controller.BaseController;
import com.scaffold.common.core.web.domain.AjaxResult;
import com.scaffold.common.core.web.page.TableDataInfo;
import com.scaffold.common.log.annotation.Log;
import com.scaffold.common.log.enums.BusinessType;
import com.scaffold.common.redis.service.RedisService;
import com.scaffold.common.security.annotation.RequiresPermissions;
import com.scaffold.system.api.SysUserOnlineResource;
import com.scaffold.system.api.model.LoginUser;
import com.scaffold.system.domain.SysUserOnline;
import com.scaffold.system.service.ISysUserOnlineService;

/**
 * 在线用户信息服务实现（Triple REST）
 *
 * @author ct
 */
@DubboService(protocol = "tri")
public class SysUserOnlineResourceImpl extends BaseController implements SysUserOnlineResource
{
    private final ISysUserOnlineService userOnlineService;
    private final RedisService redisService;

    public SysUserOnlineResourceImpl(ISysUserOnlineService userOnlineService, RedisService redisService)
    {
        this.userOnlineService = userOnlineService;
        this.redisService = redisService;
    }

    @RequiresPermissions("monitor:online:list")
    @Override
    public TableDataInfo list(String ipaddr, String userName)
    {
        Collection<String> keys = redisService.keys(CacheConstants.LOGIN_TOKEN_KEY + "*");
        List<SysUserOnline> userOnlineList = new ArrayList<SysUserOnline>();
        for (String key : keys)
        {
            LoginUser user = redisService.getCacheObject(key);
            if (StringUtils.isNotEmpty(ipaddr) && StringUtils.isNotEmpty(userName))
            {
                userOnlineList.add(userOnlineService.selectOnlineByInfo(ipaddr, userName, user));
            }
            else if (StringUtils.isNotEmpty(ipaddr))
            {
                userOnlineList.add(userOnlineService.selectOnlineByIpaddr(ipaddr, user));
            }
            else if (StringUtils.isNotEmpty(userName))
            {
                userOnlineList.add(userOnlineService.selectOnlineByUserName(userName, user));
            }
            else
            {
                userOnlineList.add(userOnlineService.loginUserToUserOnline(user));
            }
        }
        Collections.reverse(userOnlineList);
        userOnlineList.removeAll(Collections.singleton(null));
        return getDataTable(userOnlineList);
    }

    @RequiresPermissions("monitor:online:forceLogout")
    @Log(title = "在线用户", businessType = BusinessType.FORCE)
    @Override
    public AjaxResult forceLogout(@PathVariable String tokenId)
    {
        redisService.deleteObject(CacheConstants.LOGIN_TOKEN_KEY + tokenId);
        return success();
    }
}