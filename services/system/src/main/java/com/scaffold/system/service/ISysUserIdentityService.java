package com.scaffold.system.service;

import java.util.List;
import com.scaffold.system.domain.SysUserIdentity;

/**
 * 用户第三方绑定表 服务层（外部 ID 映射）
 *
 * 给本地 sys_user.user_id 提供一层外部账号 ID 映射，支持用第三方唯一 ID 反查本地用户。
 *
 * @author ct
 */
public interface ISysUserIdentityService
{
    /**
     * 绑定外部账号到本地用户
     *
     * @param identity 绑定关系（userId/idpType/idpUid 必填）
     * @return 结果（1=成功；uk_idp / uk_user_idp 唯一冲突由数据库抛 DuplicateKeyException）
     */
    public int bindIdentity(SysUserIdentity identity);

    /**
     * 通过外部 ID 反查绑定关系
     *
     * @param idpType 身份源类型（local/wechat/qq/dingtalk/corp_sso）
     * @param idpUid 身份源唯一ID
     * @return 绑定关系（未绑定时返回 null）
     */
    public SysUserIdentity selectByExternalId(String idpType, String idpUid);

    /**
     * 通过外部 ID 反查本地用户 ID（"用别人的 ID 反查自己的 ID"）
     *
     * @param idpType 身份源类型
     * @param idpUid 身份源唯一ID
     * @return 本地 sys_user.user_id（未绑定时返回 null）
     */
    public Long findUserIdByExternalId(String idpType, String idpUid);

    /**
     * 查询本地用户的全部绑定关系
     *
     * @param userId 本地用户ID
     * @return 绑定关系列表
     */
    public List<SysUserIdentity> selectListByUserId(Long userId);

    /**
     * 解绑外部账号
     *
     * @param idpType 身份源类型
     * @param idpUid 身份源唯一ID
     * @return 删除条数
     */
    public int unbindIdentity(String idpType, String idpUid);

    /**
     * 解绑本地用户全部外部账号（注销/清空绑定时使用）
     *
     * @param userId 本地用户ID
     * @return 删除条数
     */
    public int unbindAllByUserId(Long userId);
}
