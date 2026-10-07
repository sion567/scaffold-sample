package com.scaffold.system.api;

/**
 * 站内通知内部 Dubbo 契约：跨模块发布 sys_notice 通知。
 *
 * <p>区别于 {@code SysNoticeResource}（网关 REST 入口，需登录态与权限点），
 * 本契约供无登录态的系统侧通知使用——规则治理自动降级/零命中通知规则责任人（G20）、
 * 复盘反哺优化建议提醒等。实现方：scaffold-system。</p>
 *
 * @author ct
 */
public interface RemoteNoticeService
{
    /**
     * 发布站内通知（sys_notice 广播，平台内通知中心可见）
     *
     * @param noticeType 通知类型（1通知 2公告，空默认 1）
     * @param title      通知标题（必填，空直接返回 0）
     * @param content    通知内容（可空）
     * @param operator   操作人（记 create_by；系统自动场景传 "system(误报治理)" 之类）
     * @return 写入行数（0=未写入）
     */
    int push(String noticeType, String title, String content, String operator);
}
