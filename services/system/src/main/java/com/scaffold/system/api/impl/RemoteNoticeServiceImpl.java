package com.scaffold.system.api.impl;

import org.apache.dubbo.config.annotation.DubboService;
import com.scaffold.common.core.utils.StringUtils;
import com.scaffold.system.api.RemoteNoticeService;
import com.scaffold.system.domain.SysNotice;
import com.scaffold.system.service.ISysNoticeService;

/**
 * 站内通知内部 Dubbo 实现：供无登录态的跨模块系统通知使用（规则治理 G20、复盘反哺等）。
 *
 * <p>调用方已在远端做 try/catch 降级，本实现只做最小入参守卫（标题必填）。</p>
 *
 * @author ct
 */
@DubboService
public class RemoteNoticeServiceImpl implements RemoteNoticeService
{
    /** 默认通知类型：1通知（区别于 2公告） */
    private static final String TYPE_DEFAULT = "1";

    /** 状态 0=正常（通知发布即可见） */
    private static final String STATUS_NORMAL = "0";

    private final ISysNoticeService noticeService;

    public RemoteNoticeServiceImpl(ISysNoticeService noticeService)
    {
        this.noticeService = noticeService;
    }

    @Override
    public int push(String noticeType, String title, String content, String operator)
    {
        if (StringUtils.isEmpty(title))
        {
            return 0;
        }
        SysNotice notice = new SysNotice();
        notice.setNoticeTitle(title);
        notice.setNoticeType(StringUtils.isEmpty(noticeType) ? TYPE_DEFAULT : noticeType);
        notice.setNoticeContent(content);
        notice.setStatus(STATUS_NORMAL);
        notice.setCreateBy(operator);
        return noticeService.insertNotice(notice);
    }
}
