package com.scaffold.system.api.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.dubbo.config.annotation.DubboService;
import com.scaffold.system.api.RemoteUserDirectoryService;
import com.scaffold.system.api.UserDirectoryItem;
import com.scaffold.system.repository.SysUserRepository;

/**
 * 用户目录内部 Dubbo 实现：供跨服务查询有效用户三元组（message 站内信收件人展开等），
 * 替代消费方跨 schema 直查 sys_user 的单体习惯。
 *
 * <p>空入参集合直接返回空列表，避免生成非法的 in () SQL。</p>
 *
 * @author ct
 */
@DubboService
public class RemoteUserDirectoryServiceImpl implements RemoteUserDirectoryService
{
    private final SysUserRepository userRepository;

    public RemoteUserDirectoryServiceImpl(SysUserRepository userRepository)
    {
        this.userRepository = userRepository;
    }

    @Override
    public List<UserDirectoryItem> listByIds(Collection<Long> ids)
    {
        if (ids == null || ids.isEmpty())
        {
            return Collections.emptyList();
        }
        return userRepository.selectDirectoryByIds(ids);
    }

    @Override
    public List<UserDirectoryItem> listByRoleKeys(Collection<String> roleKeys)
    {
        if (roleKeys == null || roleKeys.isEmpty())
        {
            return Collections.emptyList();
        }
        return userRepository.selectDirectoryByRoleKeys(roleKeys);
    }

    @Override
    public List<UserDirectoryItem> listAll()
    {
        return userRepository.selectDirectoryAll();
    }
}
