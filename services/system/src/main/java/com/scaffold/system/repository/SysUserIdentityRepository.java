package com.scaffold.system.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.scaffold.common.core.jpa.ScaffoldRepository;
import com.scaffold.system.domain.SysUserIdentity;

/**
 * 用户第三方绑定数据访问。
 *
 * @author scaffold
 */
@Repository
public interface SysUserIdentityRepository extends ScaffoldRepository<SysUserIdentity, Long> {

    /** 按身份源定位绑定（selectByIdp） */
    Optional<SysUserIdentity> findByIdpTypeAndIdpUid(String idpType, String idpUid);

    /** 用户全部绑定（selectListByUserId） */
    List<SysUserIdentity> findByUserId(Long userId);

    /** 解绑（deleteByIdp） */
    @Transactional
    @Modifying
    long deleteByIdpTypeAndIdpUid(String idpType, String idpUid);

    /** 注销清理（deleteByUserId） */
    @Transactional
    @Modifying
    long deleteByUserId(Long userId);
}
