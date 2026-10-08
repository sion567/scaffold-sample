import { describe, it, expect, vi, beforeEach } from 'vitest'

// 首页统计轻量查询：验证走网关可达路由 + pageSize=1 取 total + silent 静默
const mockRequest = vi.hoisted(() => vi.fn())

vi.mock('@/utils/request.ts', () => ({ default: mockRequest }))

import { countUsers, countRoles, countOnline, countJobs } from '@/api/dashboard'

describe('api/dashboard 首页统计查询', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockRequest.mockResolvedValue({ code: 200, rows: [], total: 1 })
  })

  it('四个计数分别命中 /system/** 与 /job/** 可达路由', async () => {
    await countUsers()
    await countRoles()
    await countOnline()
    await countJobs()

    const urls = mockRequest.mock.calls.map(([cfg]) => cfg.url)
    expect(urls).toEqual(['/system/user/list', '/system/role/list', '/system/online/list', '/job/job/list'])
  })

  it('只取一页一条拿 total，且带 silent 静默头', async () => {
    await countUsers()

    expect(mockRequest).toHaveBeenCalledWith(
      expect.objectContaining({
        method: 'get',
        params: { pageNum: 1, pageSize: 1 },
        headers: { silent: true }
      })
    )
  })
})
