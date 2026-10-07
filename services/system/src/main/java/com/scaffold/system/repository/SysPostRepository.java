package com.scaffold.system.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.scaffold.common.core.jpa.ScaffoldRepository;
import com.scaffold.system.domain.SysPost;

/**
 * 岗位数据访问。
 *
 * @author scaffold
 */
@Repository
public interface SysPostRepository extends ScaffoldRepository<SysPost, Long> {

    Optional<SysPost> findByPostName(String postName);

    Optional<SysPost> findByPostCode(String postCode);

    /** 用户岗位ID（selectPostListByUserId） */
    @Query("select p.postId from SysPost p join SysUserPost up on up.postId = p.postId where up.userId = :userId")
    List<Long> selectPostIdsByUserId(@Param("userId") Long userId);

    /** 用户岗位（selectPostsByUserName） */
    @Query("select p from SysPost p join SysUserPost up on up.postId = p.postId "
         + "join SysUser u on u.userId = up.userId where u.userName = :userName")
    List<SysPost> selectPostsByUserName(@Param("userName") String userName);
}
