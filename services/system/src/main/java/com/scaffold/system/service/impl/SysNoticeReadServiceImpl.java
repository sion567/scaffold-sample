package com.scaffold.system.service.impl;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.scaffold.system.domain.SysNotice;
import com.scaffold.system.domain.SysNoticeRead;
import com.scaffold.system.repository.SysNoticeReadRepository;
import com.scaffold.system.repository.SysNoticeRepository;
import com.scaffold.system.service.ISysNoticeReadService;

/**
 * 公告已读记录 服务层实现（JPA：Repository + 组合查询装配已读状态）
 *
 * @author ct
 */
@Service
public class SysNoticeReadServiceImpl implements ISysNoticeReadService
{
    private final SysNoticeReadRepository noticeReadRepository;
    private final SysNoticeRepository noticeRepository;

    public SysNoticeReadServiceImpl(SysNoticeReadRepository noticeReadRepository, SysNoticeRepository noticeRepository)
    {
        this.noticeReadRepository = noticeReadRepository;
        this.noticeRepository = noticeRepository;
    }

    /**
     * 标记已读
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long noticeId, Long userId)
    {
        SysNoticeRead record = new SysNoticeRead();
        record.setNoticeId(noticeId);
        record.setUserId(userId);
        record.setReadTime(new Date());
        noticeReadRepository.save(record);
    }

    /**
     * 查询某用户未读公告数量
     */
    @Override
    public int selectUnreadCount(Long userId)
    {
        return (int) noticeReadRepository.countUnread(userId);
    }

    /**
     * 查询公告列表并标记当前用户已读状态
     * （两条查询装配：公告主体 + 当前用户已读集合，避免 JPQL 无法回填 @Transient 字段）
     */
    @Override
    public List<SysNotice> selectNoticeListWithReadStatus(Long userId, int limit)
    {
        List<SysNotice> notices = noticeRepository.findByStatusOrderByNoticeIdDesc("0", PageRequest.of(0, limit));
        if (notices.isEmpty())
        {
            return notices;
        }
        Set<Long> noticeIds = new HashSet<>();
        for (SysNotice notice : notices)
        {
            noticeIds.add(notice.getNoticeId());
        }
        Set<Long> readIds = new HashSet<>(noticeReadRepository.findReadNoticeIds(userId, noticeIds));
        for (SysNotice notice : notices)
        {
            notice.setIsRead(readIds.contains(notice.getNoticeId()));
        }
        return notices;
    }

    /**
     * 批量标记已读
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markReadBatch(Long userId, Long[] noticeIds)
    {
        if (noticeIds == null || noticeIds.length == 0)
        {
            return;
        }
        Date now = new Date();
        List<SysNoticeRead> records = new java.util.ArrayList<>(noticeIds.length);
        for (Long noticeId : noticeIds)
        {
            SysNoticeRead record = new SysNoticeRead();
            record.setNoticeId(noticeId);
            record.setUserId(userId);
            record.setReadTime(now);
            records.add(record);
        }
        noticeReadRepository.saveAll(records);
    }

    /**
     * 查询已阅读某公告的用户列表
     */
    @Override
    public List<Map<String, Object>> selectReadUsersByNoticeId(Long noticeId, String searchValue)
    {
        return noticeReadRepository.findReadUsers(noticeId,
                com.scaffold.common.core.utils.StringUtils.isEmpty(searchValue) ? null : searchValue);
    }

    /**
     * 删除公告时清理对应已读记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByNoticeIds(Long[] noticeIds)
    {
        noticeReadRepository.deleteByNoticeIds(java.util.Arrays.asList(noticeIds));
    }

    /**
     * 已读用户分页
     */
    @Override
    public com.scaffold.common.core.web.page.TableDataInfo selectReadUsersPage(Long noticeId, String searchValue, com.scaffold.common.core.web.page.PageDomain page)
    {
        List<Map<String, Object>> rows = selectReadUsersByNoticeId(noticeId, searchValue);
        long total = rows.size();
        int pageNum = page.getPageNum() == null ? 1 : page.getPageNum();
        int pageSize = page.getPageSize() == null ? 10 : page.getPageSize();
        int from = Math.min(Math.max((pageNum - 1) * pageSize, 0), rows.size());
        int to = Math.min(from + pageSize, rows.size());
        return new com.scaffold.common.core.web.page.TableDataInfo(rows.subList(from, to), total);
    }
}
