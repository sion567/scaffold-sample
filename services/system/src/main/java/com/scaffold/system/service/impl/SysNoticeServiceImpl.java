package com.scaffold.system.service.impl;

import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.scaffold.common.core.jpa.JpaSpecs;
import com.scaffold.system.domain.SysNotice;
import com.scaffold.system.repository.SysNoticeRepository;
import com.scaffold.system.service.ISysNoticeService;

/**
 * 公告 服务层实现（JPA：Repository + Specification）
 *
 * @author ct
 */
@Service
public class SysNoticeServiceImpl implements ISysNoticeService
{
    private final SysNoticeRepository noticeRepository;

    public SysNoticeServiceImpl(SysNoticeRepository noticeRepository)
    {
        this.noticeRepository = noticeRepository;
    }

    /**
     * 查询公告信息
     *
     * @param noticeId 公告ID
     * @return 公告信息
     */
    @Override
    public SysNotice selectNoticeById(Long noticeId)
    {
        return noticeRepository.findById(noticeId).orElse(null);
    }

    /**
     * 查询公告列表（noticeTitle/noticeContent 模糊、noticeType 等值、createBy 等值）
     *
     * @param notice 公告信息
     * @return 公告集合
     */
    @Override
    public List<SysNotice> selectNoticeList(SysNotice notice)
    {
        if (notice == null)
        {
            notice = new SysNotice();
        }
        Specification<?> spec = JpaSpecs.likeIf("noticeTitle", notice.getNoticeTitle())
                .and(JpaSpecs.eqIfNotBlank("noticeType", notice.getNoticeType()))
                .and(JpaSpecs.eqIfNotBlank("createBy", notice.getCreateBy()))
                .and(JpaSpecs.dateRangeIf("createTime", notice.getParams()));
        return noticeRepository.list(spec, org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Direction.DESC, "noticeId"));
    }

    @Override
    public com.scaffold.common.core.web.page.TableDataInfo selectNoticePage(SysNotice notice, com.scaffold.common.core.web.page.PageDomain page)
    {
        if (notice == null)
        {
            notice = new SysNotice();
        }
        Specification<?> spec = JpaSpecs.likeIf("noticeTitle", notice.getNoticeTitle())
                .and(JpaSpecs.eqIfNotBlank("noticeType", notice.getNoticeType()))
                .and(JpaSpecs.eqIfNotBlank("createBy", notice.getCreateBy()))
                .and(JpaSpecs.dateRangeIf("createTime", notice.getParams()));
        return com.scaffold.common.core.web.page.TableDataInfo.from(noticeRepository.page(spec,
                com.scaffold.common.core.utils.PageUtils.toPageRequest(page)));
    }

    /**
     * 新增公告
     *
     * @param notice 公告信息
     * @return 结果
     */
    @Override
    public int insertNotice(SysNotice notice)
    {
        noticeRepository.save(notice);
        return 1;
    }

    /**
     * 修改公告
     *
     * @param notice 公告信息
     * @return 结果
     */
    @Override
    public int updateNotice(SysNotice notice)
    {
        noticeRepository.save(notice);
        return 1;
    }

    /**
     * 删除公告对象
     *
     * @param noticeId 公告ID
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteNoticeById(Long noticeId)
    {
        noticeRepository.deleteById(noticeId);
        return 1;
    }

    /**
     * 批量删除公告信息
     *
     * @param noticeIds 需要删除的公告ID
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteNoticeByIds(Long[] noticeIds)
    {
        noticeRepository.deleteAllById(java.util.Arrays.asList(noticeIds));
        return noticeIds.length;
    }
}
