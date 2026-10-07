package com.scaffold.system.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.scaffold.common.core.jpa.ScaffoldRepository;
import com.scaffold.system.domain.SysUserPasswordHistory;

/**
 * 用户密码历史数据访问。
 *
 * @author scaffold
 */
@Repository
public interface SysUserPasswordHistoryRepository extends ScaffoldRepository<SysUserPasswordHistory, Long> {

    /** 最近 N 条历史密码密文（selectRecentPasswords，配合 Pageable 限条数） */
    @Query("select h.password from SysUserPasswordHistory h where h.userId = :userId order by h.createTime desc")
    List<String> findRecentPasswords(@Param("userId") Long userId, Pageable pageable);

    /** 保留集合的记录ID（cleanupHistory 两步删除的第一步） */
    @Query("select h.id from SysUserPasswordHistory h where h.userId = :userId order by h.createTime desc")
    List<Long> findRecentIds(@Param("userId") Long userId, Pageable pageable);

    /** 清理超出保留数量的历史（cleanupHistory：删除不在保留集合内的记录） */
    @Transactional
    @Modifying
    @Query("delete from SysUserPasswordHistory h where h.userId = :userId and h.id not in :keepIds")
    int deleteByUserIdAndIdNotIn(@Param("userId") Long userId, @Param("keepIds") Collection<Long> keepIds);
}
