package com.scaffold.system.service.impl;

import java.util.Date;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.scaffold.system.domain.SysUserIdentity;
import com.scaffold.system.repository.SysUserIdentityRepository;
import com.scaffold.system.service.ISysUserIdentityService;

/**
 * 用户第三方绑定表 服务层实现（JPA：Repository 派生方法）
 *
 * @author ct
 */
@Service
public class SysUserIdentityServiceImpl implements ISysUserIdentityService
{
    private final SysUserIdentityRepository identityRepository;

    public SysUserIdentityServiceImpl(SysUserIdentityRepository identityRepository)
    {
        this.identityRepository = identityRepository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int bindIdentity(SysUserIdentity identity)
    {
        if (identity.getCreateTime() == null)
        {
            identity.setCreateTime(new Date());
        }
        identityRepository.save(identity);
        return 1;
    }

    @Override
    public SysUserIdentity selectByExternalId(String idpType, String idpUid)
    {
        return identityRepository.findByIdpTypeAndIdpUid(idpType, idpUid).orElse(null);
    }

    @Override
    public Long findUserIdByExternalId(String idpType, String idpUid)
    {
        SysUserIdentity identity = identityRepository.findByIdpTypeAndIdpUid(idpType, idpUid).orElse(null);
        return identity != null ? identity.getUserId() : null;
    }

    @Override
    public List<SysUserIdentity> selectListByUserId(Long userId)
    {
        return identityRepository.findByUserId(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int unbindIdentity(String idpType, String idpUid)
    {
        return (int) identityRepository.deleteByIdpTypeAndIdpUid(idpType, idpUid);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int unbindAllByUserId(Long userId)
    {
        return (int) identityRepository.deleteByUserId(userId);
    }
}
