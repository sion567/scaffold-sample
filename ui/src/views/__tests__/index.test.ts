import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import type { SysLogininfor } from '@/api/monitor/logininfor'

// Mock 审计/监控接口：登录日志按 status 参数区分成功/失败统计口径
// （vi.hoisted：vi.mock 工厂被提升到文件顶部，普通 const 会拿不到）
const { mockListLogininfor, mockListOperlog, mockListOnline, initMock, setOptionMock, disposeMock } = vi.hoisted(() => {
  const setOptionMock = vi.fn()
  const disposeMock = vi.fn()
  return {
    mockListLogininfor: vi.fn(),
    mockListOperlog: vi.fn(),
    mockListOnline: vi.fn(),
    initMock: vi.fn(() => ({ setOption: setOptionMock, dispose: disposeMock, resize: vi.fn() })),
    setOptionMock,
    disposeMock
  }
})

vi.mock('@/api/monitor/logininfor', () => ({ list: (...args: unknown[]) => mockListLogininfor(...args) }))
vi.mock('@/api/monitor/operlog', () => ({ list: (...args: unknown[]) => mockListOperlog(...args) }))
vi.mock('@/api/monitor/online', () => ({ list: (...args: unknown[]) => mockListOnline(...args) }))
vi.mock('echarts', () => ({ init: initMock }))

function fmtDate(date: Date): string {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

function daysAgo(n: number): string {
  const d = new Date()
  d.setDate(d.getDate() - n)
  return fmtDate(d)
}

/** 与组件一致的近7日登录明细（2 成功 + 1 失败，今天） */
function loginRows(): SysLogininfor[] {
  const today = daysAgo(0)
  return [
    { userName: 'admin', status: '0', accessTime: `${today} 09:00:00` },
    { userName: 'test', status: '0', accessTime: `${today} 10:00:00` },
    { userName: 'admin', status: '1', accessTime: `${today} 11:00:00` }
  ]
}

import Index from '@/views/index.vue'

function mountPage() {
  return mount(Index)
}

describe('首页统计仪表盘', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockListOnline.mockResolvedValue({ code: 200, rows: [], total: 7 })
    mockListLogininfor.mockImplementation((query: { status?: string }) =>
      Promise.resolve(
        query.status === '1'
          ? { code: 200, rows: [], total: 2 }
          : { code: 200, rows: loginRows(), total: 13 }
      )
    )
    mockListOperlog.mockResolvedValue({
      code: 200,
      rows: [
        { operName: 'admin', businessType: 1, operTime: `${daysAgo(0)} 09:00:00` },
        { operName: 'admin', businessType: 1, operTime: `${daysAgo(1)} 10:00:00` },
        { operName: 'admin', businessType: 2, operTime: `${daysAgo(1)} 11:00:00` },
        { operName: 'test', businessType: 3, operTime: `${daysAgo(2)} 12:00:00` }
      ],
      total: 25
    })
  })

  it('渲染四张统计卡片，数值来自接口 total', async () => {
    const wrapper = mountPage()
    await flushPromises()

    const text = wrapper.text()
    expect(text).toContain('在线用户')
    expect(text).toContain('今日登录')
    expect(text).toContain('今日操作')
    expect(text).toContain('今日登录失败')
    expect(text).toContain('7')
    expect(text).toContain('13')
    expect(text).toContain('25')
    expect(text).toContain('2')
  })

  it('统计卡片按当天日期区间查询，失败次数额外带 status=1', async () => {
    mountPage()
    await flushPromises()

    const today = daysAgo(0)
    const loginCalls = mockListLogininfor.mock.calls.map(([q]) => q)
    expect(loginCalls.some((q) => q.beginTime === today && q.endTime === today && q.status === undefined)).toBe(true)
    expect(loginCalls.some((q) => q.status === '1' && q.beginTime === today)).toBe(true)
    expect(mockListOperlog).toHaveBeenCalledWith(expect.objectContaining({ beginTime: today, endTime: today }))
  })

  it('初始化 3 张图表；登录趋势聚合成功/失败计数', async () => {
    mountPage()
    await flushPromises()

    expect(initMock).toHaveBeenCalledTimes(3)
    expect(setOptionMock).toHaveBeenCalledTimes(3)

    const trendOption = setOptionMock.mock.calls[0][0]
    expect(trendOption.series[0].data).toEqual([0, 0, 0, 0, 0, 0, 2])
    expect(trendOption.series[1].data).toEqual([0, 0, 0, 0, 0, 0, 1])
  })

  it('操作类型分布按 businessType 聚合并翻译标签', async () => {
    mountPage()
    await flushPromises()

    const pieOption = setOptionMock.mock.calls[1][0]
    const data = pieOption.series[0].data as { name: string; value: number }[]
    expect(data).toContainEqual({ name: '新增', value: 2 })
    expect(data).toContainEqual({ name: '修改', value: 1 })
    expect(data).toContainEqual({ name: '删除', value: 1 })
  })

  it('活跃用户 TOP5 按操作次数降序取前五', async () => {
    mountPage()
    await flushPromises()

    const barOption = setOptionMock.mock.calls[2][0]
    expect(barOption.yAxis.data).toEqual(['test', 'admin'])
    expect(barOption.series[0].data).toEqual([1, 3])
  })

  it('单个接口失败不阻塞页面（其余卡片与图表照常渲染）', async () => {
    mockListOnline.mockRejectedValue(new Error('no permission'))
    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.text()).toContain('今日登录')
    expect(initMock).toHaveBeenCalledTimes(3)
  })

  it('卸载时释放图表并移除 resize 监听', async () => {
    const spy = vi.spyOn(window, 'removeEventListener')
    const wrapper = mountPage()
    await flushPromises()
    wrapper.unmount()

    expect(disposeMock).toHaveBeenCalledTimes(3)
    expect(spy).toHaveBeenCalledWith('resize', expect.any(Function))
  })
})
