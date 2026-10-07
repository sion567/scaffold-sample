package com.scaffold.system.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.scaffold.common.core.jpa.ScaffoldRepository;
import com.scaffold.system.domain.SysUserPost;

/**
 * SysUserPost 数据访问。
 *
 * @author scaffold
 */
@Repository
public interface SysUserPostRepository extends ScaffoldRepository<SysUserPost, com.scaffold.system.domain.SysUserPostId>
{
    /** 岗位使用数量（删除前校验） */
    long countByPostId(Long postId);

    /** 注销清理（deleteUserPostByUserId） */
    @Transactional
    @Modifying
    @Query("delete from SysUserPost up where up.userId = :userId")
    int deleteByUserId(@Param("userId") Long userId);

    /** 批量删除用户岗位（deleteUserPost） */
    @Transactional
    @Modifying
    @Query("delete from SysUserPost up where up.userId in :userIds")
    int deleteByUserIdIn(@Param("userIds") List<Long> userIds);
}
