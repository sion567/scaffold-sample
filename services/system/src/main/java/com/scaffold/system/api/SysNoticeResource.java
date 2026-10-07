package com.scaffold.system.api;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import com.scaffold.common.core.web.domain.AjaxResult;
import com.scaffold.common.core.web.page.TableDataInfo;
import com.scaffold.system.domain.SysNotice;

/**
 * 通知公告信息服务（Triple REST 对外接口）
 *
 * @author scaffold
 */
@RequestMapping("/notice")
public interface SysNoticeResource
{
    /**
     * 获取通知公告列表
     */
    @GetMapping("/list")
    TableDataInfo list(@RequestParam(value = "pageNum", required = false) Integer pageNum,
                       @RequestParam(value = "pageSize", required = false) Integer pageSize,
                       @RequestParam(value = "orderByColumn", required = false) String orderByColumn,
                       @RequestParam(value = "isAsc", required = false) String isAsc,
                       @RequestParam(value = "noticeId", required = false) Long noticeId,
                       @RequestParam(value = "noticeTitle", required = false) String noticeTitle,
                       @RequestParam(value = "noticeType", required = false) String noticeType,
                       @RequestParam(value = "status", required = false) String status);

    /**
     * 根据通知公告ID获取详细信息
     */
    @GetMapping("/{noticeId}")
    AjaxResult getInfo(@RequestParam("noticeId") Long noticeId);

    /**
     * 新增通知公告
     */
    @PostMapping
    AjaxResult add(@RequestBody SysNotice notice);

    /**
     * 修改通知公告
     */
    @PutMapping
    AjaxResult edit(@RequestBody SysNotice notice);

    /**
     * 获取置顶通知公告列表
     */
    @GetMapping("/listTop")
    AjaxResult listTop();

    /**
     * 标记通知公告为已读
     */
    @PostMapping("/markRead")
    AjaxResult markRead(@RequestParam("noticeId") Long noticeId);

    /**
     * 批量标记通知公告为已读
     */
    @PostMapping("/markReadAll")
    AjaxResult markReadAll(@RequestParam("ids") String ids);

    /**
     * 获取通知公告已读用户列表
     */
    @GetMapping("/readUsers/list")
    TableDataInfo readUsersList(@RequestParam(value = "pageNum", required = false) Integer pageNum,
                                @RequestParam(value = "pageSize", required = false) Integer pageSize,
                                @RequestParam(value = "orderByColumn", required = false) String orderByColumn,
                                @RequestParam(value = "isAsc", required = false) String isAsc,
                                @RequestParam(value = "noticeId", required = false) Long noticeId,
                                @RequestParam(value = "searchValue", required = false) String searchValue);

    /**
     * 删除通知公告
     */
    @DeleteMapping("/{noticeIds}")
    AjaxResult remove(@RequestParam("noticeIds") Long[] noticeIds);
}
