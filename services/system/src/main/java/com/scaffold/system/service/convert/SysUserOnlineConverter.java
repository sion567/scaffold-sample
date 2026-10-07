package com.scaffold.system.service.convert;

import org.springframework.stereotype.Component;
import com.scaffold.system.api.model.LoginUser;
import com.scaffold.system.domain.SysUserOnline;

/**
 * LoginUser → SysUserOnline 映射（手写实现；MapStruct 注解处理器未接入本仓库，勿用 org.mapstruct.Mapper）。
 *
 * <p>映射语义：tokenId=token、userName=username、ipaddr/loginTime 同名拷贝；
 * loginLocation/browser/os 不在此设置（由调用方按 user-agent 解析补充）。</p>
 *
 * @author ct
 */
@Component
public class SysUserOnlineConverter
{
    /** 登录用户 → 在线用户 */
    public SysUserOnline fromLoginUser(LoginUser user)
    {
        SysUserOnline online = new SysUserOnline();
        online.setTokenId(user.getToken());
        online.setUserName(user.getUsername());
        online.setIpaddr(user.getIpaddr());
        online.setLoginTime(user.getLoginTime());
        return online;
    }
}
