package com.scaffold.message.service.impl;

import com.scaffold.system.api.RemoteUserDirectoryService;
import com.scaffold.system.api.UserDirectoryItem;
import java.util.Collection;
import java.util.List;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

/**
 * 用户目录客户端：经 RemoteUserDirectoryService（scaffold-system）查询有效用户三元组，
 * 替代跨 schema 直查 sys_user 的单体习惯（本地 H2 内存库无 SYSTEM_DB，直查必挂）。
 *
 * @author ct
 */
@Component
public class UserDirectoryClient
{
    @DubboReference(check = false)
    private RemoteUserDirectoryService userDirectoryService;

    /** 全部有效用户（按用户ID排序） */
    public List<UserDirectoryItem> listAll()
    {
        return userDirectoryService.listAll();
    }

    /** 按用户ID查有效用户 */
    public List<UserDirectoryItem> listByIds(Collection<Long> ids)
    {
        return userDirectoryService.listByIds(ids);
    }

    /** 按角色键查有效用户 */
    public List<UserDirectoryItem> listByRoleKeys(Collection<String> roleKeys)
    {
        return userDirectoryService.listByRoleKeys(roleKeys);
    }
}
