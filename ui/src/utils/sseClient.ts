// ============================================================
// 大屏 SSE 客户端（Fetch + ReadableStream，docs/webflux-dubbo-sse-dashboard.md §4.2）
//
// 原生 EventSource 不支持自定义请求头（无法携带 Token），大屏走 Fetch 手动解析流，
// 并内置指数退避重连（1s → 2s → 4s → ... → 30s 封顶，上限 10 次）。
// 后端事件契约：event=dashboard-update（业务数据）、注释行 heartbeat（40s 保活）。
// 使用方务必在 onMessage 里刷新「最后数据时间」——心跳只能证明连接活着，
// 只有真实业务数据才代表链路健康（§4.3 ④）。
// ============================================================

import { getToken } from '@/utils/auth'

export interface SseClientOptions {
  /** 覆盖默认 Token（默认自动取登录态 Cookie） */
  token?: string
  /** 最大重试次数，默认 10 */
  maxRetry?: number
  /** 初始重连延迟 ms，默认 1000 */
  retryDelay?: number
  /** 重连延迟封顶 ms，默认 30000 */
  maxDelay?: number
  /** 业务事件名，默认 dashboard-update */
  event?: string
}

type OnMessage = (data: unknown) => void
type OnError = (err: unknown) => void

export class SseClient {
  private url: string
  private options: SseClientOptions
  private abortController = new AbortController()
  private shouldReconnect = false
  private currentEvent = ''

  constructor(url: string, options: SseClientOptions = {}) {
    this.url = url
    this.options = options
  }

  /**
   * 建立连接并自动重连；onMessage 仅收到业务事件时回调
   */
  connect(onMessage: OnMessage, onError?: OnError): void {
    let attempt = 0
    this.shouldReconnect = true

    const run = async (): Promise<void> => {
      while (this.shouldReconnect) {
        try {
          await this.openStream(onMessage, onError)
          if (!this.shouldReconnect) break
        } catch (err) {
          if ((err as Error).name === 'AbortError' || !this.shouldReconnect) break
          onError?.(err)
        }

        // 指数退避：1s → 2s → 4s → ... → 30s（封顶）
        attempt++
        if (attempt > (this.options.maxRetry ?? 10)) {
          console.error('[SseClient] 重连次数超限，停止重试')
          break
        }
        const base = this.options.retryDelay ?? 1000
        const max = this.options.maxDelay ?? 30000
        const delay = Math.min(base * Math.pow(2, attempt - 1), max)
        console.warn(`[SseClient] ${delay}ms 后进行第 ${attempt} 次重连...`)
        await new Promise((r) => setTimeout(r, delay))
      }
    }

    run()
  }

  private async openStream(onMessage: OnMessage, onError?: OnError): Promise<void> {
    const base = import.meta.env.VITE_APP_BASE_API || ''
    const token = this.options.token ?? getToken() ?? ''
    const response = await fetch(base + this.url, {
      method: 'GET',
      headers: {
        Accept: 'text/event-stream',
        // 核心：Fetch 方案才能携带认证头
        Authorization: `Bearer ${token}`
      },
      signal: this.abortController.signal
    })

    if (!response.ok || !response.body) {
      throw new Error(`SSE 连接失败: ${response.status}`)
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() ?? '' // 保留未完成的行

      for (const line of lines) {
        const trimmed = line.trimEnd()
        if (trimmed.startsWith('event:')) {
          this.currentEvent = trimmed.slice(6).trim()
        } else if (trimmed.startsWith('data:')) {
          const dataStr = trimmed.slice(5).trim()
          if (this.currentEvent === (this.options.event ?? 'dashboard-update') && dataStr) {
            try {
              onMessage(JSON.parse(dataStr))
            } catch (err) {
              onError?.(err)
            }
          }
        }
        // 注释行（:heartbeat）保持连接活性，不触发业务回调
      }
    }
  }

  /** 断开连接（同时停止自动重连） */
  disconnect(): void {
    this.shouldReconnect = false
    this.abortController.abort()
  }
}
