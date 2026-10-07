import request from '@/utils/request.ts'
import type { AjaxResult, TableDataInfo, BaseEntity, PageQuery } from '@/types/api'

/** 参数配置实体（后端 SysConfig，sys_config 表） */
export interface SysConfig extends BaseEntity {
  /** 参数主键 */
  configId?: number
  /** 参数名称 */
  configName?: string
  /** 参数键名 */
  configKey?: string
  /** 参数键值 */
  configValue?: string
  /** 系统内置（Y是 N否） */
  configType?: string
}

/** 参数列表查询参数 */
export interface SysConfigQuery extends PageQuery {
  /** 参数名称 */
  configName?: string
  /** 参数键名 */
  configKey?: string
  /** 系统内置（Y是 N否） */
  configType?: string
  /** 创建时间范围-开始 */
  beginTime?: string
  /** 创建时间范围-结束 */
  endTime?: string
  /** 动态请求参数（后端 BaseEntity.params，addDateRange 注入 beginTime/endTime） */
  params?: Record<string, unknown>
}

// 查询参数列表
export function listConfig(query: SysConfigQuery): Promise<TableDataInfo<SysConfig>> {
  return request({
    url: '/system/config/list',
    method: 'get',
    params: query
  })
}

// 查询参数详细
export function getConfig(configId: number | string | number[]): Promise<AjaxResult<SysConfig>> {
  return request({
    url: '/system/config/' + configId,
    method: 'get'
  })
}

// 根据参数键名查询参数值（后端 success(String) 重载，参数值位于 msg 字段）
export function getConfigKey(configKey: string): Promise<AjaxResult<null>> {
  return request({
    url: '/system/config/configKey/' + configKey,
    method: 'get'
  })
}

// 新增参数配置
export function addConfig(data: SysConfig): Promise<AjaxResult<null>> {
  return request({
    url: '/system/config',
    method: 'post',
    data: data
  })
}

// 修改参数配置
export function updateConfig(data: SysConfig): Promise<AjaxResult<null>> {
  return request({
    url: '/system/config',
    method: 'put',
    data: data
  })
}

// 删除参数配置
export function delConfig(configId: number | string | number[]): Promise<AjaxResult<null>> {
  return request({
    url: '/system/config/' + configId,
    method: 'delete'
  })
}

// 刷新参数缓存
export function refreshCache(): Promise<AjaxResult<null>> {
  return request({
    url: '/system/config/refreshCache',
    method: 'delete'
  })
}
