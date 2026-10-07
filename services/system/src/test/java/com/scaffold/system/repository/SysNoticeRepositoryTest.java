package com.scaffold.system.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.data.domain.PageRequest;

import com.scaffold.common.test.H2JpaTest;
import com.scaffold.system.domain.SysNotice;

/**
 * SysNotice / SysNoticeRead 数据访问切片测试（JPA + H2 Oracle 模式）。
 * 覆盖：状态公告派生查询（需在 SysNoticeRepository 上解析）、未读数 JPQL、已读集合与清理。
 *
 * @author scaffold
 */
@H2JpaTest(
        ddl = {"sql/system/sys_notice_h2.sql", "sql/system/sys_notice_read_h2.sql"},
        entityPackages = {"com.scaffold.system.domain", "com.scaffold.system.api.domain"})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SysNoticeRepositoryTest
{
    @Test
    @Order(1)
    @DisplayName("findByStatusOrderByNoticeIdDesc：只取正常状态公告，按 ID 倒序")
    void findByStatus(SysNoticeRepository repository)
    {
        List<SysNotice> notices = repository.findByStatusOrderByNoticeIdDesc("0", PageRequest.of(0, 10));
        assertEquals(Arrays.asList(4L, 2L, 1L),
                notices.stream().map(SysNotice::getNoticeId).toList(),
                "停用公告(3)被排除，其余倒序");

        List<SysNotice> top2 = repository.findByStatusOrderByNoticeIdDesc("0", PageRequest.of(0, 2));
        assertEquals(2, top2.size());
        assertEquals(4L, top2.get(0).getNoticeId(), "top-N 走 Pageable 截断");
    }

    @Test
    @Order(2)
    @DisplayName("countUnread：已读记录之外的正常状态公告数")
    void countUnread(SysNoticeReadRepository repository)
    {
        assertEquals(1L, repository.countUnread(1L), "用户1已读1、2，未读只剩4");
        assertEquals(3L, repository.countUnread(2L), "用户2无已读记录");
    }

    @Test
    @Order(3)
    @DisplayName("countByNoticeIdAndUserId：是否已读判定")
    void countByNoticeIdAndUserId(SysNoticeReadRepository repository)
    {
        assertEquals(1L, repository.countByNoticeIdAndUserId(1L, 1L));
        assertEquals(0L, repository.countByNoticeIdAndUserId(4L, 1L));
    }

    @Test
    @Order(4)
    @DisplayName("findReadNoticeIds：返回范围内已读的公告ID")
    void findReadNoticeIds(SysNoticeReadRepository repository)
    {
        List<Long> readIds = repository.findReadNoticeIds(1L, Arrays.asList(1L, 2L, 4L));
        assertEquals(Arrays.asList(1L, 2L), readIds);
    }

    @Test
    @Order(5)
    @DisplayName("deleteByNoticeIds：删除公告时清理已读记录")
    void deleteByNoticeIds(SysNoticeReadRepository repository)
    {
        assertEquals(2, repository.deleteByNoticeIds(Arrays.asList(1L, 2L)));
        assertTrue(repository.findReadNoticeIds(1L, Arrays.asList(1L, 2L)).isEmpty(), "清理后不可再查到");
    }
}
