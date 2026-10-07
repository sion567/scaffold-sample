import request from '@/utils/request.ts'
import type { AjaxResult } from '@/types/api'
import type { SysUser } from './system/user'

/** 登录入参（后端 LoginBody 投影） */
export interface LoginBody {
  /** 用户账号 */
  username: string
  /** 密码（SM2 加密后的密文） */
  password: string
  /** 图形验证码（开启验证码时必填） */
  code?: string
  /** 验证码唯一标识 */
  uuid?: string
  /** 双因子验证码（命中双因子配置的账号） */
  secondAuthCode?: string
}

/** 登录成功返回的 token 信息（后端 TokenService.createToken 的 rspMap） */
export interface LoginTokenResult {
  /** JWT 访问令牌 */
  access_token: string
  /** 过期时长（分钟） */
  expires_in: number
}

/** 注册入参（后端 RegisterBody 投影） */
export interface RegisterBody {
  /** 用户账号 */
  username: string
  /** 密码 */
  password: string
  /** 确认密码 */
  confirmPassword?: string
  /** 图形验证码 */
  code?: string
  /** 验证码唯一标识 */
  uuid?: string
}

/** 解锁屏幕入参 */
export interface UnlockScreenBody {
  /** 锁屏密码 */
  password: string
}

/** 图形验证码响应（网关 ValidateCodeService 平铺字段） */
export interface CaptchaResult {
  /** 状态码 */
  code: number
  /** 提示消息 */
  msg: string
  /** 是否启用验证码 */
  captchaEnabled?: boolean
  /** 验证码唯一标识 */
  uuid?: string
  /** base64 图片内容 */
  img?: string
}

/** 用户详情响应（后端 getInfo：user/roles/permissions 等平铺字段） */
export interface UserInfoResult {
  /** 状态码 */
  code: number
  /** 提示消息 */
  msg: string
  /** 当前登录用户 */
  user: SysUser
  /** 角色权限字符集合 */
  roles: string[]
  /** 菜单权限字符集合 */
  permissions: string[]
  /** 密码字符类型配置（sys.account.chrtype） */
  pwdChrtype?: string
  /** 是否仍为初始密码 */
  isDefaultModifyPwd?: boolean
  /** 密码是否已过期 */
  isPasswordExpired?: boolean
}

// 登录方法
export function login(username: string, password: string, code?: string, uuid?: string): Promise<AjaxResult<LoginTokenResult>> {
  const data = {
    username,
    password,
    code,
    uuid
  }
  return request({
    url: '/auth/login',
    headers: {
      isToken: false,
      repeatSubmit: false
    },
    method: 'post',
    data: data
  })
}

// 注册方法
export function register(data: RegisterBody): Promise<AjaxResult<null>> {
  return request({
    url: '/auth/register',
    headers: {
      isToken: false
    },
    method: 'post',
    data: data
  })
}

// 获取用户详细信息
export function getInfo(): Promise<UserInfoResult> {
  return request({
    url: '/system/user/getInfo',
    method: 'get'
  })
}

// 解锁屏幕
export function unlockScreen(password: string): Promise<AjaxResult<null>> {
  return request({
    url: '/auth/unlockscreen',
    method: 'post',
    data: { password}
  })
}

// 退出方法
export function logout(token?: string): Promise<AjaxResult<null>> {
  return request({
    url: '/auth/logout',
    method: 'delete'
  })
}

// 获取验证码
export function getCodeImg(): Promise<CaptchaResult> {
  return request({
    url: '/captchaImage',
    headers: {
      isToken: false
    },
    method: 'get',
    timeout: 20000
  })
}
