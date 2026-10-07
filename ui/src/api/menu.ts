import request from '@/utils/request.ts'
import type { AjaxResult } from '@/types/api'

/** 路由菜单元信息（后端 MetaVo） */
export interface RouteMeta {
  /** 路由标题 */
  title?: string
  /** 菜单图标 */
  icon?: string
  /** 是否不缓存该路由 */
  noCache?: boolean
  /** 外链地址 */
  link?: string | null
}

/** 动态路由（后端 RouterVo） */
export interface RouterVO {
  /** 路由名称 */
  name?: string
  /** 路由地址 */
  path: string
  /** 是否隐藏路由 */
  hidden?: boolean
  /** 重定向地址 */
  redirect?: string
  /** 组件路径 */
  component?: string
  /** 路由参数 */
  query?: string
  /** 菜单是否始终显示 */
  alwaysShow?: boolean
  /** 路由元信息 */
  meta?: RouteMeta
  /** 子路由 */
  children?: RouterVO[]
}

// 获取路由
export const getRouters = (): Promise<AjaxResult<RouterVO[]>> => {
  return request({
    url: '/system/menu/getRouters',
    method: 'get'
  })
}
