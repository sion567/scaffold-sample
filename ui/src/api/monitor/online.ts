import request from '@/utils/request.ts'
import type { AjaxResult, TableDataInfo, PageQuery } from '@/types/api'

/** 在线用户实体（后端 SysUserOnline，由 Redis 会话构建） */
export interface SysUserOnline {
  /** 会话编号 */
  tokenId?: string
  /** 用户账号 */
  userName?: string
  /** 登录IP地址 */
  ipaddr?: string
  /** 登录地点 */
  loginLocation?: string
  /** 浏览器类型 */
  browser?: string
  /** 操作系统 */
  os?: string
  /** 登录时间（毫秒时间戳） */
  loginTime?: number
}

/** 在线用户列表查询参数 */
export interface OnlineUserQuery extends PageQuery {
  /** 登录IP地址 */
  ipaddr?: string
  /** 用户账号 */
  userName?: string
}

// 查询在线用户列表（网关 /system/** 路由 → system 服务 /online/list）
export function list(query: OnlineUserQuery): Promise<TableDataInfo<SysUserOnline>> {
  return request({
    url: '/system/online/list',
    method: 'get',
    params: query
  })
}

// 强退用户
export function forceLogout(tokenId: string): Promise<AjaxResult<null>> {
  return request({
    url: '/system/online/' + tokenId,
    method: 'delete'
  })
}
