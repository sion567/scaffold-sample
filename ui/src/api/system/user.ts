import request from '@/utils/request.ts'
import { parseStrEmpty } from "@/utils/ct.ts";
import type { AjaxResult, TableDataInfo, BaseEntity, PageQuery, TreeSelectNode } from '@/types/api'
import type { SysRole } from './role'
import type { SysPost } from './post'
import type { SysDept } from './dept'

/** 用户实体（后端 SysUser，sys_user 表） */
export interface SysUser extends BaseEntity {
  /** 用户ID */
  userId?: number
  /** 部门ID */
  deptId?: number
  /** 用户账号 */
  userName?: string
  /** 用户昵称 */
  nickName?: string
  /** 用户邮箱 */
  email?: string
  /** 手机号码 */
  phonenumber?: string
  /** 用户性别（0男 1女 2未知） */
  sex?: string
  /** 用户头像 */
  avatar?: string
  /** 密码 */
  password?: string
  /** 账号状态（0正常 1停用） */
  status?: string
  /** 删除标志（0代表存在 2代表删除） */
  delFlag?: string
  /** 最后登录IP */
  loginIp?: string
  /** 最后登录时间 */
  loginDate?: string
  /** 密码最后更新时间 */
  pwdUpdateDate?: string
  /** 部门对象 */
  dept?: SysDept
  /** 角色对象 */
  roles?: SysRole[]
  /** 角色组 */
  roleIds?: number[]
  /** 岗位组 */
  postIds?: number[]
  /** 角色ID */
  roleId?: number
}

/** 用户列表查询参数 */
export interface SysUserQuery extends PageQuery {
  /** 用户账号 */
  userName?: string
  /** 手机号码 */
  phonenumber?: string
  /** 账号状态（0正常 1停用） */
  status?: string
  /** 部门ID */
  deptId?: number
  /** 创建时间范围-开始 */
  beginTime?: string
  /** 创建时间范围-结束 */
  endTime?: string
  /** 动态请求参数（后端 BaseEntity.params，addDateRange 注入 beginTime/endTime） */
  params?: Record<string, unknown>
}

/** 用户详情响应（后端 getUserInfo：data + roles/posts/postIds/roleIds 平铺） */
export interface SysUserDetailResult {
  /** 状态码 */
  code: number
  /** 提示消息 */
  msg: string
  /** 用户详情（未指定 userId 时为 null） */
  data: SysUser | null
  /** 角色列表（分配角色下拉） */
  roles: SysRole[]
  /** 岗位列表（分配岗位下拉） */
  posts: SysPost[]
  /** 用户已选岗位ID */
  postIds?: number[]
  /** 用户已选角色ID */
  roleIds?: number[]
}

/** 个人中心响应（后端 profile：data + roleGroup/postGroup 平铺） */
export interface SysProfileResult extends AjaxResult<SysUser> {
  /** 所属角色组 */
  roleGroup: string
  /** 所属岗位组 */
  postGroup: string
}

/** 头像上传响应（后端 avatar：data + imgUrl 平铺） */
export interface SysAvatarResult extends AjaxResult<null> {
  /** 头像访问地址 */
  imgUrl: string
}

/** 授权角色响应（后端 authRole：user + roles 平铺） */
export interface SysAuthRoleResult {
  /** 状态码 */
  code: number
  /** 提示消息 */
  msg: string
  /** 用户信息 */
  user: SysUser
  /** 全部角色列表（非管理员过滤掉 admin 角色） */
  roles: SysRole[]
}

// 查询用户列表
export function listUser(query: SysUserQuery): Promise<TableDataInfo<SysUser>> {
  return request({
    url: '/system/user/list',
    method: 'get',
    params: query
  })
}

// 查询用户详细
export function getUser(userId?: number | string): Promise<SysUserDetailResult> {
  return request({
    url: '/system/user/' + parseStrEmpty(userId?.toString()),
    method: 'get'
  })
}

// 新增用户
export function addUser(data: SysUser): Promise<AjaxResult<null>> {
  return request({
    url: '/system/user',
    method: 'post',
    data: data
  })
}

// 修改用户
export function updateUser(data: SysUser): Promise<AjaxResult<null>> {
  return request({
    url: '/system/user',
    method: 'put',
    data: data
  })
}

// 删除用户
export function delUser(userId: number | string | number[]): Promise<AjaxResult<null>> {
  return request({
    url: '/system/user/' + userId,
    method: 'delete'
  })
}

// 用户密码重置
export function resetUserPwd(userId: number | string | number[], password: string): Promise<AjaxResult<null>> {
  const data = {
    userId,
    password
  }
  return request({
    url: '/system/user/resetPwd',
    method: 'put',
    data: data
  })
}

// 用户状态修改
export function changeUserStatus(userId: number | string | number[], status: string): Promise<AjaxResult<null>> {
  const data = {
    userId,
    status
  }
  return request({
    url: '/system/user/changeStatus',
    method: 'put',
    data: data
  })
}

// 查询用户个人信息
export function getUserProfile(): Promise<SysProfileResult> {
  return request({
    url: '/system/user/profile',
    method: 'get'
  })
}

// 修改用户个人信息
export function updateUserProfile(data: SysUser): Promise<AjaxResult<null>> {
  return request({
    url: '/system/user/profile',
    method: 'put',
    data: data
  })
}

// 用户密码重置
export function updateUserPwd(oldPassword: string, newPassword: string): Promise<AjaxResult<null>> {
  const data = {
    oldPassword,
    newPassword
  }
  return request({
    url: '/system/user/profile/updatePwd',
    method: 'put',
    data: data
  })
}

// 用户头像上传
export function uploadAvatar(data: FormData): Promise<SysAvatarResult> {
  return request({
    url: '/system/user/profile/avatar',
    method: 'post',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    data: data
  })
}

// 查询授权角色
export function getAuthRole(userId: number | string | number[]): Promise<SysAuthRoleResult> {
  return request({
    url: '/system/user/authRole/' + userId,
    method: 'get'
  })
}

// 保存授权角色
export function updateAuthRole(data: { userId: number | string | number[]; roleIds: string }): Promise<AjaxResult<null>> {
  return request({
    url: '/system/user/authRole',
    method: 'put',
    params: data
  })
}

// 查询部门下拉树结构
export function deptTreeSelect(): Promise<AjaxResult<TreeSelectNode[]>> {
  return request({
    url: '/system/user/deptTree',
    method: 'get'
  })
}

// 导出用户列表（响应为二进制流）
export function exportUser(query: SysUserQuery): Promise<Blob> {
  return request({
    url: '/system/user/export',
    method: 'post',
    data: query,
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    responseType: 'blob'
  })
}
