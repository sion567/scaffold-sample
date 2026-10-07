import { describe, it, expect, vi, beforeEach, type Mock } from 'vitest'
import { listUser, getUser, addUser, updateUser, delUser, resetUserPwd, changeUserStatus, deptTreeSelect } from '@/api/system/user'

// Mock request
vi.mock('@/utils/request', () => ({
  default: vi.fn(() => Promise.resolve({}))
}))

describe('用户管理 API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('listUser', () => {
    it('查询用户列表', async () => {
      const mockResponse = {
        rows: [
          { userId: 1, userName: 'admin', nickName: '管理员', status: '0' },
          { userId: 2, userName: 'test', nickName: '测试', status: '0' }
        ],
        total: 2
      }
      const request = (await import('@/utils/request')).default as unknown as Mock
      request.mockResolvedValue(mockResponse)

      const result = await listUser({ pageNum: 1, pageSize: 10 })

      expect(request).toHaveBeenCalled()
      expect(result.rows).toHaveLength(2)
      expect(result.total).toBe(2)
    })
  })

  describe('getUser', () => {
    it('查询用户详情', async () => {
      const mockUser = {
        userId: 1,
        userName: 'admin',
        nickName: '管理员'
      }
      const request = (await import('@/utils/request')).default as unknown as Mock
      request.mockResolvedValue({ data: mockUser })

      const result = await getUser(1)

      expect(request).toHaveBeenCalled()
      expect(result.data?.userName).toBe('admin')
    })
  })

  describe('addUser', () => {
    it('新增用户', async () => {
      const request = (await import('@/utils/request')).default as unknown as Mock
      request.mockResolvedValue({ code: 200, msg: '操作成功' })

      const userData = {
        userName: 'newuser',
        nickName: '新用户',
        password: '123456'
      }
      const result = await addUser(userData)

      expect(request).toHaveBeenCalled()
      expect(result.code).toBe(200)
    })
  })

  describe('updateUser', () => {
    it('修改用户', async () => {
      const request = (await import('@/utils/request')).default as unknown as Mock
      request.mockResolvedValue({ code: 200, msg: '修改成功' })

      const userData = { userId: 1, userName: 'admin', nickName: '管理员更新' }
      const result = await updateUser(userData)

      expect(request).toHaveBeenCalled()
      expect(result.code).toBe(200)
    })
  })

  describe('delUser', () => {
    it('删除单个用户', async () => {
      const request = (await import('@/utils/request')).default as unknown as Mock
      request.mockResolvedValue({ code: 200, msg: '删除成功' })

      const result = await delUser(1)

      expect(request).toHaveBeenCalled()
      expect(result.code).toBe(200)
    })

    it('批量删除用户', async () => {
      const request = (await import('@/utils/request')).default as unknown as Mock
      request.mockResolvedValue({ code: 200, msg: '删除成功' })

      await delUser('1,2,3')

      expect(request).toHaveBeenCalled()
    })
  })

  describe('resetUserPwd', () => {
    it('重置用户密码', async () => {
      const request = (await import('@/utils/request')).default as unknown as Mock
      request.mockResolvedValue({ code: 200, msg: '重置成功' })

      const result = await resetUserPwd(1, '123456')

      expect(request).toHaveBeenCalled()
      expect(result.code).toBe(200)
    })
  })

  describe('changeUserStatus', () => {
    it('启用用户', async () => {
      const request = (await import('@/utils/request')).default as unknown as Mock
      request.mockResolvedValue({ code: 200, msg: '操作成功' })

      await changeUserStatus(1, '0')

      expect(request).toHaveBeenCalled()
    })

    it('禁用用户', async () => {
      const request = (await import('@/utils/request')).default as unknown as Mock
      request.mockResolvedValue({ code: 200, msg: '操作成功' })

      await changeUserStatus(1, '1')

      expect(request).toHaveBeenCalled()
    })
  })

  describe('deptTreeSelect', () => {
    it('查询部门树', async () => {
      const mockTree = [{ id: 1, label: '总公司', children: [{ id: 101, label: '研发部' }] }]
      const request = (await import('@/utils/request')).default as unknown as Mock
      request.mockResolvedValue({ data: mockTree })

      const result = await deptTreeSelect()

      expect(request).toHaveBeenCalled()
      expect(result.data).toHaveLength(1)
    })
  })
})
