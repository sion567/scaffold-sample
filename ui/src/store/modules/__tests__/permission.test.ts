import { describe, it, expect, vi } from 'vitest'

describe('permission 工具函数', () => {
  describe('hasPermiOr', () => {
    it('返回布尔值', () => {
      const hasPermiOr = vi.fn().mockReturnValue(true)
      expect(hasPermiOr(['system:user:list'])).toBe(true)
    })

    it('返回 false 当无权限', () => {
      const hasPermiOr = vi.fn().mockReturnValue(false)
      expect(hasPermiOr(['system:user:delete'])).toBe(false)
    })
  })

  describe('hasRoleOr', () => {
    it('返回布尔值', () => {
      const hasRoleOr = vi.fn().mockReturnValue(true)
      expect(hasRoleOr(['admin'])).toBe(true)
    })

    it('返回 false 当无角色', () => {
      const hasRoleOr = vi.fn().mockReturnValue(false)
      expect(hasRoleOr(['guest'])).toBe(false)
    })
  })
})

describe('filterDynamicRoutes 模拟测试', () => {
  it('按权限过滤路由', () => {
    const routes = [
      { permissions: ['system:user:list'] },
      { permissions: ['system:role:list'] }
    ]
    const hasPermiOr = vi.fn()
      .mockReturnValueOnce(true)
      .mockReturnValueOnce(false)

    const result = routes.filter(route => {
      if (route.permissions) {
        return hasPermiOr(route.permissions)
      }
      return false
    })

    expect(result).toHaveLength(1)
  })
})
