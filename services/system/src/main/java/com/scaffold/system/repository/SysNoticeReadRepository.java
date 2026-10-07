package com.scaffold.system.repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.scaffold.common.core.jpa.ScaffoldRepository;
import com.scaffold.system.domain.SysNoticeRead;

/**
 * 公告已读记录数据访问。
 *
 * @author scaffold
 */
@Repository
public interface SysNoticeReadRepository extends ScaffoldRepository<SysNoticeRead, Long> {

    /** 未读公告数（selectUnreadCount） */
    @Query("select count(n) from SysNotice n where n.status = '0' "
         + "and not exists (select 1 from SysNoticeRead r where r.noticeId = n.noticeId and r.userId = :userId)")
    long countUnread(@Param("userId") Long userId);

    /** 是否已读（selectIsRead） */
    long countByNoticeIdAndUserId(Long noticeId, Long userId);

    /** 用户已读的公告ID */
    @Query("select r.noticeId from SysNoticeRead r where r.userId = :userId and r.noticeId in :noticeIds")
    List<Long> findReadNoticeIds(@Param("userId") Long userId, @Param("noticeIds") Collection<Long> noticeIds);

    /** 删除公告时清理已读记录（deleteByNoticeIds） */
    @Modifying
    @Query("delete from SysNoticeRead r where r.noticeId in :noticeIds")
    int deleteByNoticeIds(@Param("noticeIds") Collection<Long> noticeIds);

    /** 已读用户列表（selectReadUsersByNoticeId，含登录名/昵称模糊筛选） */
    @Query("select new map(u.userId as userId, u.userName as userName, u.nickName as nickName, "
         + "d.deptName as deptName, u.phonenumber as phonenumber, r.readTime as readTime) "
         + "from SysNoticeRead r join SysUser u on u.userId = r.userId and u.delFlag = '0' "
         + "left join SysDept d on d.deptId = u.deptId "
         + "where r.noticeId = :noticeId "
         + "and (:searchValue is null or u.userName like concat('%', :searchValue, '%') "
         + "or u.nickName like concat('%', :searchValue, '%')) "
         + "order by r.readTime desc")
    List<Map<String, Object>> findReadUsers(@Param("noticeId") Long noticeId,
            @Param("searchValue") String searchValue);
}
