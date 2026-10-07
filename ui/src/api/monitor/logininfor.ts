import request from '@/utils/request.ts'
import type { AjaxResult, TableDataInfo, BaseEntity, PageQuery } from '@/types/api'

/** 登录日志实体（后端 SysLogininfor，sys_logininfor 表） */
export interface SysLogininfor extends BaseEntity {
  /** 访问ID */
  infoId?: number
  /** 用户账号 */
  userName?: string
  /** 登录状态（0成功 1失败） */
  status?: string
  /** 登录IP地址 */
  ipaddr?: string
  /** 提示消息 */
  msg?: string
  /** 访问时间 */
  accessTime?: string
}

/** 登录日志列表查询参数 */
export interface SysLogininforQuery extends PageQuery {
  /** 登录IP地址 */
  ipaddr?: string
  /** 用户账号 */
  userName?: string
  /** 登录状态（0成功 1失败） */
  status?: string
  /** 登录时间范围-开始 */
  beginTime?: string
  /** 登录时间范围-结束 */
  endTime?: string
  /** 动态请求参数（后端 BaseEntity.params，addDateRange 注入 beginTime/endTime） */
  params?: Record<string, unknown>
}

// 查询登录日志列表（审计中心 ct-audit /audit/logininfor）
export function list(query: SysLogininforQuery): Promise<TableDataInfo<SysLogininfor>> {
  return request({
    url: '/audit/logininfor',
    method: 'get',
    params: query
  })
}

// 解锁用户登录状态（用户域接口，清除登录失败计数）
export function unlockLogininfor(userName: string | string[]): Promise<AjaxResult<null>> {
  return request({
    url: '/system/user/unlock/' + userName,
    method: 'get'
  })
}
