package com.scaffold.system.api;

import java.util.Collection;
import java.util.List;

/**
 * 用户目录内部 Dubbo 契约：跨服务查询有效用户（状态正常、未删除）的轻量三元组
 * （用户ID/账号/昵称）。实现方：scaffold-system。
 *
 * <p>替代消费方跨 schema 直查 sys_user 的单体习惯（如 message 站内信收件人展开）。
 * 与 {@link RemoteNoticeService} 同为纯 Java 契约（Triple 直发）。</p>
 *
 * @author ct
 */
public interface RemoteUserDirectoryService
{
    /**
     * 按用户ID查询有效用户
     *
     * @param ids 用户ID集合（空集合返回空列表）
     * @return 用户目录条目
     */
    List<UserDirectoryItem> listByIds(Collection<Long> ids);

    /**
     * 按角色键查询有效用户（用户/角色均状态正常且未删除）
     *
     * @param roleKeys 角色键集合（空集合返回空列表）
     * @return 去重后的用户目录条目
     */
    List<UserDirectoryItem> listByRoleKeys(Collection<String> roleKeys);

    /**
     * 查询全部有效用户（按用户ID排序）
     *
     * @return 用户目录条目
     */
    List<UserDirectoryItem> listAll();
}
