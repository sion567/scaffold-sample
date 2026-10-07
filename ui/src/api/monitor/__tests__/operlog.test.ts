import { describe, it, expect, vi, beforeEach, type Mock } from 'vitest'
import { list } from '@/api/monitor/operlog'

// Mock request
vi.mock('@/utils/request', () => ({
  default: vi.fn(() => Promise.resolve({}))
}))

describe('操作日志 API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('list', () => {
    it('查询操作日志列表', async () => {
      const mockResponse = {
        rows: [{ operId: 1, title: '用户管理', operName: 'admin' }],
        total: 1
      }
      const request = (await import('@/utils/request')).default as unknown as Mock
      request.mockResolvedValue(mockResponse)

      const result = await list({ pageNum: 1, pageSize: 10 })

      expect(request).toHaveBeenCalledWith({
        url: '/audit/oper-log',
        method: 'get',
        params: { pageNum: 1, pageSize: 10 }
      })
      expect(result.rows).toHaveLength(1)
    })

    it('带条件查询操作日志', async () => {
      const request = (await import('@/utils/request')).default as unknown as Mock
      request.mockResolvedValue({ rows: [], total: 0 })

      await list({ title: '用户管理', businessType: '1', status: '0', beginTime: '2026-09-01 00:00:00', endTime: '2026-09-07 23:59:59' })

      expect(request).toHaveBeenCalled()
      expect(request.mock.calls[0][0].params.title).toBe('用户管理')
      expect(request.mock.calls[0][0].params.beginTime).toBe('2026-09-01 00:00:00')
    })
  })
})
