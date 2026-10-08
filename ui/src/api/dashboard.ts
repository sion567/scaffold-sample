import request from '@/utils/request.ts'
import type { TableDataInfo } from '@/types/api'

/**
 * 首页平台统计：仅取分页 total（pageNum=1 & pageSize=1），
 * 全部走网关静态路由可达的服务（/system/**、/job/**，StripPrefix 后命中
 * system 服务 /user /role /online 与 job 服务 /job 控制器）。
 * silent 静默：账号缺对应查询权限（403）或服务不可用时前端显示“–”，不弹全局错误。
 */

/** 通用计数查询：pageSize=1 只为拿 total */
function countOf(url: string): Promise<TableDataInfo<unknown>> {
  return request({
    url,
    method: 'get',
    params: { pageNum: 1, pageSize: 1 },
    headers: { silent: true }
  })
}

/** 用户总数（权限 system:user:list） */
export function countUsers(): Promise<TableDataInfo<unknown>> {
  return countOf('/system/user/list')
}

/** 角色总数（权限 system:role:list） */
export function countRoles(): Promise<TableDataInfo<unknown>> {
  return countOf('/system/role/list')
}

/** 当前在线用户数（权限 monitor:online:list） */
export function countOnline(): Promise<TableDataInfo<unknown>> {
  return countOf('/system/online/list')
}

/** 定时任务数（权限 monitor:job:list） */
export function countJobs(): Promise<TableDataInfo<unknown>> {
  return countOf('/job/job/list')
}
