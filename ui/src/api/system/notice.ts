import request from '@/utils/request.ts'
import type { AjaxResult, TableDataInfo, BaseEntity, PageQuery } from '@/types/api'

/** 公告实体（后端 SysNotice，sys_notice 表） */
export interface SysNotice extends BaseEntity {
  /** 公告ID */
  noticeId?: number
  /** 公告标题 */
  noticeTitle?: string
  /** 公告类型（1通知 2公告） */
  noticeType?: string
  /** 公告内容 */
  noticeContent?: string
  /** 公告状态（0正常 1关闭） */
  status?: string
  /** 当前用户是否已读 */
  isRead?: boolean
}

/** 公告列表查询参数 */
export interface SysNoticeQuery extends PageQuery {
  /** 公告标题 */
  noticeTitle?: string
  /** 创建者 */
  createBy?: string
  /** 公告状态（0正常 1关闭） */
  status?: string
  /** 公告类型 */
  noticeType?: string
}

/** 公告已读用户（后端 selectReadUsersByNoticeId 返回的 Map 投影） */
export interface NoticeReadUser {
  /** 用户ID */
  userId: number
  /** 登录名称 */
  userName: string
  /** 用户名称 */
  nickName?: string
  /** 所属部门 */
  deptName?: string
  /** 手机号码 */
  phonenumber?: string
  /** 阅读时间 */
  readTime?: string
}

/** 公告已读用户查询参数 */
export interface NoticeReadUserQuery extends PageQuery {
  /** 公告ID */
  noticeId?: number
  /** 搜索值（登录名称/用户名称模糊匹配） */
  searchValue?: string
}

// 查询公告列表
export function listNotice(query: SysNoticeQuery): Promise<TableDataInfo<SysNotice>> {
  return request({
    url: '/system/notice/list',
    method: 'get',
    params: query
  })
}

// 查询公告详细
export function getNotice(noticeId: number | string | number[]): Promise<AjaxResult<SysNotice>> {
  return request({
    url: '/system/notice/' + noticeId,
    method: 'get'
  })
}

// 新增公告
export function addNotice(data: SysNotice): Promise<AjaxResult<null>> {
  return request({
    url: '/system/notice',
    method: 'post',
    data: data
  })
}

// 修改公告
export function updateNotice(data: SysNotice): Promise<AjaxResult<null>> {
  return request({
    url: '/system/notice',
    method: 'put',
    data: data
  })
}

// 删除公告
export function delNotice(noticeId: number | string | number[]): Promise<AjaxResult<null>> {
  return request({
    url: '/system/notice/' + noticeId,
    method: 'delete'
  })
}

// 首页顶部公告列表（带已读状态，unreadCount 为响应平铺字段）
export function listNoticeTop(): Promise<AjaxResult<SysNotice[]> & { unreadCount: number }> {
  return request({
    url: '/system/notice/listTop',
    method: 'get'
  })
}

// 标记公告已读
export function markNoticeRead(noticeId: number | string | number[]): Promise<AjaxResult<null>> {
  return request({
    url: '/system/notice/markRead',
    method: 'post',
    params: { noticeId }
  })
}

// 批量标记已读（ids 为逗号拼接串）
export function markNoticeReadAll(ids: string): Promise<AjaxResult<null>> {
  return request({
    url: '/system/notice/markReadAll',
    method: 'post',
    params: { ids }
  })
}

// 查询公告已读用户列表
export function listNoticeReadUsers(query: NoticeReadUserQuery): Promise<TableDataInfo<NoticeReadUser>> {
  return request({
    url: '/system/notice/readUsers/list',
    method: 'get',
    params: query
  })
}
