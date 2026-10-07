import request from '@/utils/request.ts'
import type { AjaxResult, TableDataInfo, BaseEntity, PageQuery, TreeSelectNode } from '@/types/api'
import type { SysUser } from './user'
import type { SysDept } from './dept'

/** 角色实体（后端 SysRole，sys_role 表） */
export interface SysRole extends BaseEntity {
  /** 角色ID */
  roleId?: number
  /** 角色名称 */
  roleName?: string
  /** 角色权限字符 */
  roleKey?: string
  /** 角色排序 */
  roleSort?: number
  /** 数据范围（1所有 2自定义 3本部门 4本部门及以下 5仅本人） */
  dataScope?: string
  /** 菜单树选择项是否关联显示（0父子不互相关联 1父子互相关联） */
  menuCheckStrictly?: boolean
  /** 部门树选择项是否关联显示（0父子不互相关联 1父子互相关联） */
  deptCheckStrictly?: boolean
  /** 角色状态（0正常 1停用） */
  status?: string
  /** 删除标志（0代表存在 2代表删除） */
  delFlag?: string
  /** 用户是否存在此角色标识 默认不存在 */
  flag?: boolean
  /** 菜单组 */
  menuIds?: number[]
  /** 部门组（数据权限） */
  deptIds?: number[]
  /** 角色菜单权限字符 */
  permissions?: string[]
}

/** 角色列表查询参数 */
export interface SysRoleQuery extends PageQuery {
  /** 角色名称 */
  roleName?: string
  /** 角色权限字符 */
  roleKey?: string
  /** 角色状态（0正常 1停用） */
  status?: string
  /** 创建时间范围-开始 */
  beginTime?: string
  /** 创建时间范围-结束 */
  endTime?: string
  /** 动态请求参数（后端 BaseEntity.params，addDateRange 注入 beginTime/endTime） */
  params?: Record<string, unknown>
}

/** 角色-菜单树响应（后端 roleMenuTreeselect：checkedKeys + menus 平铺） */
export interface RoleMenuTreeResult {
  /** 状态码 */
  code: number
  /** 提示消息 */
  msg: string
  /** 角色已勾选菜单ID */
  checkedKeys: number[]
  /** 菜单下拉树结构 */
  menus: TreeSelectNode[]
}

/** 角色-部门树响应（后端 deptTree：checkedKeys + depts 平铺） */
export interface RoleDeptTreeResult {
  /** 状态码 */
  code: number
  /** 提示消息 */
  msg: string
  /** 角色已勾选部门ID（数据权限） */
  checkedKeys: number[]
  /** 部门树结构（后端 TreeSelect：id/label/children） */
  depts: TreeSelectNode[]
}

/** 角色授权用户（取消授权入参） */
export interface AuthUserCancel {
  /** 用户ID */
  userId: number
  /** 角色ID */
  roleId?: number
}

/** 批量授权/取消授权入参（userIds 为逗号拼接串） */
export interface AuthUserSelectAll {
  /** 角色ID */
  roleId?: number
  /** 用户ID串（逗号分隔） */
  userIds: string
}

/** 授权用户列表查询参数 */
export interface AuthUserQuery extends PageQuery {
  /** 角色ID */
  roleId?: number
  /** 用户账号 */
  userName?: string
  /** 手机号码 */
  phonenumber?: string
  /** 账号状态（0正常 1停用） */
  status?: string
}

// 查询角色列表
export function listRole(query: SysRoleQuery): Promise<TableDataInfo<SysRole>> {
  return request({
    url: '/system/role/list',
    method: 'get',
    params: query
  })
}

// 查询角色详细
export function getRole(roleId: number | string | number[]): Promise<AjaxResult<SysRole>> {
  return request({
    url: '/system/role/' + roleId,
    method: 'get'
  })
}

// 新增角色
export function addRole(data: SysRole): Promise<AjaxResult<null>> {
  return request({
    url: '/system/role',
    method: 'post',
    data: data
  })
}

// 修改角色
export function updateRole(data: SysRole): Promise<AjaxResult<null>> {
  return request({
    url: '/system/role',
    method: 'put',
    data: data
  })
}

// 角色数据权限
export function dataScope(data: SysRole): Promise<AjaxResult<null>> {
  return request({
    url: '/system/role/dataScope',
    method: 'put',
    data: data
  })
}

// 角色状态修改
export function changeRoleStatus(roleId: number | string | number[], status: string): Promise<AjaxResult<null>> {
  const data = {
    roleId,
    status
  }
  return request({
    url: '/system/role/changeStatus',
    method: 'put',
    data: data
  })
}

// 删除角色
export function delRole(roleId: number | string | number[]): Promise<AjaxResult<null>> {
  return request({
    url: '/system/role/' + roleId,
    method: 'delete'
  })
}

// 查询角色已授权用户列表
export function allocatedUserList(query: AuthUserQuery): Promise<TableDataInfo<SysUser>> {
  return request({
    url: '/system/role/authUser/allocatedList',
    method: 'get',
    params: query
  })
}

// 查询角色未授权用户列表
export function unallocatedUserList(query: AuthUserQuery): Promise<TableDataInfo<SysUser>> {
  return request({
    url: '/system/role/authUser/unallocatedList',
    method: 'get',
    params: query
  })
}

// 取消用户授权角色
export function authUserCancel(data: AuthUserCancel): Promise<AjaxResult<null>> {
  return request({
    url: '/system/role/authUser/cancel',
    method: 'put',
    data: data
  })
}

// 批量取消用户授权角色
export function authUserCancelAll(data: AuthUserSelectAll): Promise<AjaxResult<null>> {
  return request({
    url: '/system/role/authUser/cancelAll',
    method: 'put',
    params: data
  })
}

// 授权用户选择
export function authUserSelectAll(data: AuthUserSelectAll): Promise<AjaxResult<null>> {
  return request({
    url: '/system/role/authUser/selectAll',
    method: 'put',
    params: data
  })
}

// 根据角色ID查询部门树结构
export function deptTreeSelect(roleId: number | string | number[]): Promise<RoleDeptTreeResult> {
  return request({
    url: '/system/role/deptTree/' + roleId,
    method: 'get'
  })
}
