package com.scaffold.system.service;

import java.util.List;

/**
 * 用户密码历史记录 服务层
 *
 * @author ct
 */
public interface ISysUserPasswordHistoryService
{
    /**
     * 记录用户新密码到历史表（存储 SM3 密文）
     *
     * @param userId 用户ID
     * @param encryptedPassword SM3 加密后的密码
     * @return 结果
     */
    public int insertPasswordHistory(Long userId, String encryptedPassword);

    /**
     * 查询用户最近 N 条密码历史（密文）
     *
     * @param userId 用户ID
     * @param limit 最近条数（<=0 返回空列表）
     * @return 密码密文列表
     */
    public List<String> selectRecentPasswords(Long userId, int limit);

    /**
     * 校验明文密码是否命中该用户最近 historyCount 条历史密码（SM3 逐条比对）
     *
     * @param rawPassword 明文密码
     * @param userId 用户ID
     * @param historyCount 历史条数（<=0 表示不校验历史，直接返回 false）
     * @return true=命中历史密码（重复，应拒绝）
     */
    public boolean matchesRecentHistory(String rawPassword, Long userId, int historyCount);

    /**
     * 清理该用户超出 keepCount 条之外的更早密码历史
     *
     * @param userId 用户ID
     * @param keepCount 保留条数（<=0 表示清空全部历史）
     * @return 删除条数
     */
    public int cleanupHistory(Long userId, int keepCount);
}
