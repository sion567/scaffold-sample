package com.scaffold.system.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.scaffold.common.core.jpa.ScaffoldRepository;
import com.scaffold.system.domain.SysNotice;

/**
 * 通知公告数据访问。
 *
 * @author scaffold
 */
@Repository
public interface SysNoticeRepository extends ScaffoldRepository<SysNotice, Long> {

    /** 正常状态公告（selectNoticeListWithReadStatus 主体，read 状态由服务侧装配） */
    List<SysNotice> findByStatusOrderByNoticeIdDesc(String status, Pageable pageable);
}
