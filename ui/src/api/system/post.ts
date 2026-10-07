import request from '@/utils/request.ts'
import type { AjaxResult, TableDataInfo, BaseEntity, PageQuery } from '@/types/api'

/** 岗位实体（后端 SysPost，sys_post 表） */
export interface SysPost extends BaseEntity {
  /** 岗位ID */
  postId?: number
  /** 岗位编码 */
  postCode?: string
  /** 岗位名称 */
  postName?: string
  /** 显示顺序 */
  postSort?: number
  /** 状态（0正常 1停用） */
  status?: string
  /** 用户是否存在此岗位标识 默认不存在 */
  flag?: boolean
}

/** 岗位列表查询参数 */
export interface SysPostQuery extends PageQuery {
  /** 岗位编码 */
  postCode?: string
  /** 岗位名称 */
  postName?: string
  /** 状态（0正常 1停用） */
  status?: string
}

// 查询岗位列表
export function listPost(query: SysPostQuery): Promise<TableDataInfo<SysPost>> {
  return request({
    url: '/system/post/list',
    method: 'get',
    params: query
  })
}

// 查询岗位详细
export function getPost(postId: number | string | number[]): Promise<AjaxResult<SysPost>> {
  return request({
    url: '/system/post/' + postId,
    method: 'get'
  })
}

// 新增岗位
export function addPost(data: SysPost): Promise<AjaxResult<null>> {
  return request({
    url: '/system/post',
    method: 'post',
    data: data
  })
}

// 修改岗位
export function updatePost(data: SysPost): Promise<AjaxResult<null>> {
  return request({
    url: '/system/post',
    method: 'put',
    data: data
  })
}

// 删除岗位
export function delPost(postId: number | string | number[]): Promise<AjaxResult<null>> {
  return request({
    url: '/system/post/' + postId,
    method: 'delete'
  })
}
