import request from '@/utils/request.ts'
import type { AjaxResult } from '@/types/api'

/** Redis 信息（info 子集，键为 Redis 属性名） */
export interface RedisInfo {
  /** 任意 Redis INFO 属性，如 redis_version / used_memory_human 等 */
  [key: string]: string
}

/** 命令统计项（echarts 饼图数据） */
export interface RedisCommandStat {
  /** 命令名 */
  name: string
  /** 命中次数 */
  value: string
}

/** 缓存监控信息（后端 /monitor/cache 响应 data） */
export interface CacheInfo {
  /** Redis INFO 信息 */
  info: RedisInfo
  /** key 总数 */
  dbSize: number
  /** 命令调用统计（用于图表） */
  commandStats: RedisCommandStat[]
}

// 查询缓存详细
export function getCache(): Promise<AjaxResult<CacheInfo>> {
  return request({
    url: '/monitor/cache',
    method: 'get'
  })
}

// 查询缓存名称列表
export function listCacheName(): Promise<AjaxResult<string[]>> {
  return request({
    url: '/monitor/cache/getNames',
    method: 'get'
  })
}

// 查询缓存键名列表
export function listCacheKey(cacheName: string): Promise<AjaxResult<string[]>> {
  return request({
    url: '/monitor/cache/getKeys/' + cacheName,
    method: 'get'
  })
}

// 查询缓存内容
export function getCacheValue(cacheName: string, cacheKey: string): Promise<AjaxResult<unknown>> {
  return request({
    url: '/monitor/cache/getValue/' + cacheName + '/' + cacheKey,
    method: 'get'
  })
}

// 清理指定名称缓存
export function clearCacheName(cacheName: string): Promise<AjaxResult<null>> {
  return request({
    url: '/monitor/cache/clearCacheName/' + cacheName,
    method: 'delete'
  })
}

// 清理指定键名缓存
export function clearCacheKey(cacheKey: string): Promise<AjaxResult<null>> {
  return request({
    url: '/monitor/cache/clearCacheKey/' + cacheKey,
    method: 'delete'
  })
}

// 清理全部缓存
export function clearCacheAll(): Promise<AjaxResult<null>> {
  return request({
    url: '/monitor/cache/clearCacheAll',
    method: 'delete'
  })
}
