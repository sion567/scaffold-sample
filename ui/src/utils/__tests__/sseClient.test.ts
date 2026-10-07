import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

// getToken 由 sseClient 内部调用，mock 固定 Token 便于断言请求头
vi.mock('@/utils/auth', () => ({
  getToken: vi.fn(() => 'test-token-123')
}))

import { SseClient } from '../sseClient'

const flushMicrotasks = () => new Promise<void>((resolve) => setTimeout(resolve, 0))

/** 构造 SSE 流式响应：每次 read 给一段（可模拟跨 chunk 断行），读完后按需断流 */
function sseResponse(chunks: string[], opts: { failAtEnd?: boolean } = {}): Response {
  const encoder = new TextEncoder()
  let index = 0
  const stream = new ReadableStream<Uint8Array>({
    pull(controller) {
      if (index < chunks.length) {
        controller.enqueue(encoder.encode(chunks[index++]))
      } else if (opts.failAtEnd) {
        controller.error(new Error('stream broken'))
      } else {
        controller.close()
      }
    }
  })
  return { ok: true, status: 200, body: stream } as unknown as Response
}

describe('SseClient', () => {
  let fetchMock: ReturnType<typeof vi.fn>

  beforeEach(() => {
    vi.useFakeTimers()
    fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.unstubAllGlobals()
  })

  it('解析 dashboard-update 事件并回调业务数据', async () => {
    fetchMock.mockResolvedValue(
      sseResponse(['event: dashboard-update\ndata: {"onlineUsers":66}\n\n'])
    )
    const onMessage = vi.fn()
    new SseClient('/dashboard/stream').connect(onMessage)

    await vi.advanceTimersByTimeAsync(0)

    expect(onMessage).toHaveBeenCalledTimes(1)
    expect(onMessage).toHaveBeenCalledWith({ onlineUsers: 66 })
  })

  it('跨 chunk 断行：未完成的行保留到下一段再解析', async () => {
    fetchMock.mockResolvedValue(
      sseResponse(['event: dashboard-update\nda', 'ta: {"todayVisits":120}\n\n'])
    )
    const onMessage = vi.fn()
    new SseClient('/dashboard/stream').connect(onMessage)

    await vi.advanceTimersByTimeAsync(0)

    expect(onMessage).toHaveBeenCalledWith({ todayVisits: 120 })
  })

  it('心跳注释行不触发业务回调', async () => {
    fetchMock.mockResolvedValue(sseResponse([':heartbeat\n\nevent: dashboard-update\ndata: {"a":1}\n\n']))
    const onMessage = vi.fn()
    new SseClient('/dashboard/stream').connect(onMessage)

    await vi.advanceTimersByTimeAsync(0)

    expect(onMessage).toHaveBeenCalledTimes(1)
    expect(onMessage).toHaveBeenCalledWith({ a: 1 })
  })

  it('携带 Authorization 请求头（Fetch 方案的核心价值）', async () => {
    fetchMock.mockResolvedValue(sseResponse(['event: dashboard-update\ndata: {}\n\n']))
    new SseClient('/dashboard/stream').connect(vi.fn())

    await vi.advanceTimersByTimeAsync(0)

    const [, init] = fetchMock.mock.calls[0]
    expect(init.headers.Authorization).toBe('Bearer test-token-123')
    expect(init.headers.Accept).toBe('text/event-stream')
  })

  it('断流后按指数退避重连（1s → 2s）', async () => {
    fetchMock
      .mockResolvedValueOnce(sseResponse(['event: dashboard-update\ndata: {"v":1}\n\n'], { failAtEnd: true }))
      .mockResolvedValueOnce(sseResponse(['event: dashboard-update\ndata: {"v":2}\n\n'], { failAtEnd: true }))

    const onMessage = vi.fn()
    const onError = vi.fn()
    new SseClient('/dashboard/stream').connect(onMessage, onError)

    await vi.advanceTimersByTimeAsync(0)
    expect(onMessage).toHaveBeenCalledWith({ v: 1 })
    expect(onError).toHaveBeenCalledTimes(1)
    expect(fetchMock).toHaveBeenCalledTimes(1)

    // 第一次重连：约 1s
    await vi.advanceTimersByTimeAsync(1000)
    expect(fetchMock).toHaveBeenCalledTimes(2)
    expect(onMessage).toHaveBeenCalledWith({ v: 2 })

    // 第二次重连：约 2s（指数增长）
    await vi.advanceTimersByTimeAsync(2000)
    expect(fetchMock).toHaveBeenCalledTimes(3)
  })

  it('disconnect 后停止重连，不再发起新请求', async () => {
    fetchMock.mockResolvedValue(sseResponse(['event: dashboard-update\ndata: {"v":1}\n\n'], { failAtEnd: true }))
    const client = new SseClient('/dashboard/stream')
    client.connect(vi.fn())

    await vi.advanceTimersByTimeAsync(0)
    expect(fetchMock).toHaveBeenCalledTimes(1)

    client.disconnect()
    await vi.advanceTimersByTimeAsync(60000)
    expect(fetchMock).toHaveBeenCalledTimes(1)
  })

  it('重连次数超限后停止（maxRetry=2 → 最多 3 次请求）', async () => {
    fetchMock.mockImplementation(async () => {
      throw new Error('network down')
    })
    const client = new SseClient('/dashboard/stream', { maxRetry: 2 })
    client.connect(vi.fn())

    await vi.advanceTimersByTimeAsync(0)
    await vi.advanceTimersByTimeAsync(1000)
    await vi.advanceTimersByTimeAsync(2000)
    await vi.advanceTimersByTimeAsync(4000)

    expect(fetchMock).toHaveBeenCalledTimes(3)
  })

  it('HTTP 非 2xx 视为连接失败并走重连', async () => {
    fetchMock
      .mockResolvedValueOnce({ ok: false, status: 502, body: null })
      .mockResolvedValueOnce(sseResponse(['event: dashboard-update\ndata: {"v":9}\n\n']))

    const onMessage = vi.fn()
    new SseClient('/dashboard/stream').connect(onMessage, vi.fn())

    await vi.advanceTimersByTimeAsync(0)
    expect(onMessage).not.toHaveBeenCalled()

    await vi.advanceTimersByTimeAsync(1000)
    expect(onMessage).toHaveBeenCalledWith({ v: 9 })
  })
})
