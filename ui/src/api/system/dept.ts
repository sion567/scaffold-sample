import request from '@/utils/request.ts'
import type { AjaxResult, BaseEntity, PageQuery } from '@/types/api'

/** 部门实体（后端 SysDept，sys_dept 表） */
export interface SysDept extends BaseEntity {
  /** 部门ID */
  deptId?: number
  /** 部门组（角色数据权限勾选，提交用） */
  deptIds?: number[]
  /** 父部门ID */
  parentId?: number
  /** 祖级列表 */
  ancestors?: string
  /** 部门名称 */
  deptName?: string
  /** 显示顺序 */
  orderNum?: number
  /** 负责人 */
  leader?: string
  /** 联系电话 */
  phone?: string
  /** 邮箱 */
  email?: string
  /** 部门状态（0正常 1停用） */
  status?: string
  /** 删除标志（0代表存在 2代表删除） */
  delFlag?: string
  /** 父部门名称 */
  parentName?: string
  /** 子部门 */
  children?: SysDept[]
}

/** 部门列表查询参数 */
export interface SysDeptQuery extends PageQuery {
  /** 部门名称 */
  deptName?: string
  /** 部门状态（0正常 1停用） */
  status?: string
}

// 查询部门列表
export function listDept(query?: SysDeptQuery): Promise<AjaxResult<SysDept[]>> {
  return request({
    url: '/system/dept/list',
    method: 'get',
    params: query
  })
}

// 查询部门列表（排除节点）
export function listDeptExcludeChild(deptId: number | string | number[]): Promise<AjaxResult<SysDept[]>> {
  return request({
    url: '/system/dept/list/exclude/' + deptId,
    method: 'get'
  })
}

// 查询部门详细
export function getDept(deptId: number | string | number[]): Promise<AjaxResult<SysDept>> {
  return request({
    url: '/system/dept/' + deptId,
    method: 'get'
  })
}

// 新增部门
export function addDept(data: SysDept): Promise<AjaxResult<null>> {
  return request({
    url: '/system/dept',
    method: 'post',
    data: data
  })
}

// 修改部门
export function updateDept(data: SysDept): Promise<AjaxResult<null>> {
  return request({
    url: '/system/dept',
    method: 'put',
    data: data
  })
}

// 保存部门排序
export function updateDeptSort(data: { deptIds: string; orderNums: string }): Promise<AjaxResult<null>> {
  return request({
    url: '/system/dept/updateSort',
    method: 'put',
    data: data
  })
}

// 删除部门
export function delDept(deptId: number | string | number[]): Promise<AjaxResult<null>> {
  return request({
    url: '/system/dept/' + deptId,
    method: 'delete'
  })
}
