package com.scaffold.system.service.impl;

import org.springframework.stereotype.Service;
import com.scaffold.common.core.utils.StringUtils;
import com.scaffold.system.api.model.LoginUser;
import com.scaffold.system.domain.SysUserOnline;
import com.scaffold.system.service.ISysUserOnlineService;
import com.scaffold.system.service.convert.SysUserOnlineConverter;

/**
 * 在线用户 服务层处理
 * 
 * @author ct
 */
@Service
public class SysUserOnlineServiceImpl implements ISysUserOnlineService
{
    private final SysUserOnlineConverter sysUserOnlineMapper;

    public SysUserOnlineServiceImpl(SysUserOnlineConverter sysUserOnlineMapper)
    {
        this.sysUserOnlineMapper = sysUserOnlineMapper;
    }
    /**
     * 通过登录地址查询信息
     * 
     * @param ipaddr 登录地址
     * @param user 用户信息
     * @return 在线用户信息
     */
    @Override
    public SysUserOnline selectOnlineByIpaddr(String ipaddr, LoginUser user)
    {
        if (StringUtils.equals(ipaddr, user.getIpaddr()))
        {
            return loginUserToUserOnline(user);
        }
        return null;
    }

    /**
     * 通过用户名称查询信息
     * 
     * @param userName 用户名称
     * @param user 用户信息
     * @return 在线用户信息
     */
    @Override
    public SysUserOnline selectOnlineByUserName(String userName, LoginUser user)
    {
        if (StringUtils.equals(userName, user.getUsername()))
        {
            return loginUserToUserOnline(user);
        }
        return null;
    }

    /**
     * 通过登录地址/用户名称查询信息
     * 
     * @param ipaddr 登录地址
     * @param userName 用户名称
     * @param user 用户信息
     * @return 在线用户信息
     */
    @Override
    public SysUserOnline selectOnlineByInfo(String ipaddr, String userName, LoginUser user)
    {
        if (StringUtils.equals(ipaddr, user.getIpaddr()) && StringUtils.equals(userName, user.getUsername()))
        {
            return loginUserToUserOnline(user);
        }
        return null;
    }

    /**
     * 设置在线用户信息
     * 
     * @param user 用户信息
     * @return 在线用户
     */
    @Override
    public SysUserOnline loginUserToUserOnline(LoginUser user)
    {
        if (StringUtils.isNull(user))
        {
            return null;
        }
        return sysUserOnlineMapper.fromLoginUser(user);
    }
}
