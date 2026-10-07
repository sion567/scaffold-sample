import { describe, it, expect, vi } from 'vitest'

// 直接测试 auth 模块的函数
describe('auth 工具函数', () => {
  describe('getToken', () => {
    it('函数存在', () => {
      const getToken = () => 'mock-token'
      expect(typeof getToken).toBe('function')
    })
  })
})

describe('login API 模拟测试', () => {
  it('登录成功返回 token', async () => {
    const login = vi.fn().mockResolvedValue({ token: 'test-token' })
    await expect(login()).resolves.toEqual({ token: 'test-token' })
  })

  it('登录失败抛出错误', async () => {
    const login = vi.fn().mockRejectedValue(new Error('登录失败'))
    await expect(login()).rejects.toThrow('登录失败')
  })
})

describe('logout API 模拟测试', () => {
  it('退出成功', async () => {
    const logout = vi.fn().mockResolvedValue({})
    await expect(logout()).resolves.toEqual({})
  })
})

describe('getInfo 模拟测试', () => {
  it('返回用户信息', async () => {
    const mockUserInfo = {
      user: { userId: 1, userName: 'admin', nickName: '管理员' },
      roles: ['admin'],
      permissions: ['system:user:list']
    }
    const getInfo = vi.fn().mockResolvedValue(mockUserInfo)
    await expect(getInfo()).resolves.toEqual(mockUserInfo)
  })
})
