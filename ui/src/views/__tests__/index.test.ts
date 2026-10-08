import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { ElButton } from 'element-plus'

// Mock 首页统计接口（api/dashboard）：四个计数独立返回，可单独模拟失败
const { mockCountUsers, mockCountRoles, mockCountOnline, mockCountJobs, pushMock } = vi.hoisted(() => {
  return {
    mockCountUsers: vi.fn(),
    mockCountRoles: vi.fn(),
    mockCountOnline: vi.fn(),
    mockCountJobs: vi.fn(),
    pushMock: vi.fn()
  }
})

vi.mock('@/api/dashboard', () => ({
  countUsers: (...args: unknown[]) => mockCountUsers(...args),
  countRoles: (...args: unknown[]) => mockCountRoles(...args),
  countOnline: (...args: unknown[]) => mockCountOnline(...args),
  countJobs: (...args: unknown[]) => mockCountJobs(...args)
}))
vi.mock('vue-router', () => ({
  useRouter: () => ({ push: pushMock })
}))

import Index from '@/views/index.vue'

function mountPage() {
  // hasPermi 为全局注册指令，单测环境用空实现代替（元素全部渲染）；
  // ElButton 显式注册以便点击断言（v-loading 等未注册指令仅告警不影响渲染）
  return mount(Index, { global: { components: { ElButton }, directives: { hasPermi: {} } } })
}

describe('首页平台统计', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockCountUsers.mockResolvedValue({ code: 200, rows: [], total: 11 })
    mockCountRoles.mockResolvedValue({ code: 200, rows: [], total: 5 })
    mockCountOnline.mockResolvedValue({ code: 200, rows: [], total: 7 })
    mockCountJobs.mockResolvedValue({ code: 200, rows: [], total: 3 })
  })

  it('渲染四张统计卡片，数值来自接口 total', async () => {
    const wrapper = mountPage()
    await flushPromises()

    const text = wrapper.text()
    expect(text).toContain('用户总数')
    expect(text).toContain('角色总数')
    expect(text).toContain('当前在线')
    expect(text).toContain('定时任务')
    expect(text).toContain('11')
    expect(text).toContain('5')
    expect(text).toContain('7')
    expect(text).toContain('3')
  })

  it('统计查询只取一页一条（pageSize=1 拿 total）', async () => {
    mountPage()
    await flushPromises()

    expect(mockCountUsers).toHaveBeenCalledWith()
    expect(mockCountRoles).toHaveBeenCalledWith()
    expect(mockCountOnline).toHaveBeenCalledWith()
    expect(mockCountJobs).toHaveBeenCalledWith()
  })

  it('单个接口失败显示占位符“–”，不阻塞其余卡片', async () => {
    mockCountRoles.mockRejectedValue(new Error('no permission'))
    const wrapper = mountPage()
    await flushPromises()

    const text = wrapper.text()
    expect(text).toContain('角色总数')
    expect(text).toContain('–')
    expect(text).toContain('11')
    expect(text).toContain('7')
  })

  it('渲染快捷入口，点击后跳转对应路由', async () => {
    const wrapper = mountPage()
    await flushPromises()

    const text = wrapper.text()
    expect(text).toContain('用户管理')
    expect(text).toContain('角色管理')
    expect(text).toContain('在线用户')
    expect(text).toContain('定时任务')

    const buttons = wrapper.findAll('button')
    const userBtn = buttons.find((b) => b.text().includes('用户管理'))
    await userBtn!.trigger('click')
    expect(pushMock).toHaveBeenCalledWith('/system/user')
  })
})
