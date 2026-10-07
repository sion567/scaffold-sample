import request from '@/utils/request.ts'
import type { AjaxResult, TableDataInfo, BaseEntity, PageQuery } from '@/types/api'

/** 字典类型实体（后端 SysDictType，sys_dict_type 表） */
export interface SysDictType extends BaseEntity {
  /** 字典主键 */
  dictId?: number
  /** 字典名称 */
  dictName?: string
  /** 字典类型 */
  dictType?: string
  /** 状态（0正常 1停用） */
  status?: string
}

/** 字典类型列表查询参数 */
export interface SysDictTypeQuery extends PageQuery {
  /** 字典名称 */
  dictName?: string
  /** 字典类型 */
  dictType?: string
  /** 状态（0正常 1停用） */
  status?: string
  /** 创建时间范围-开始 */
  beginTime?: string
  /** 创建时间范围-结束 */
  endTime?: string
  /** 动态请求参数（后端 BaseEntity.params，addDateRange 注入 beginTime/endTime） */
  params?: Record<string, unknown>
}

// 查询字典类型列表
export function listType(query: SysDictTypeQuery): Promise<TableDataInfo<SysDictType>> {
  return request({
    url: '/system/dict/type/list',
    method: 'get',
    params: query
  })
}

// 查询字典类型详细
export function getType(dictId: number | string | number[]): Promise<AjaxResult<SysDictType>> {
  return request({
    url: '/system/dict/type/' + dictId,
    method: 'get'
  })
}

// 新增字典类型
export function addType(data: SysDictType): Promise<AjaxResult<null>> {
  return request({
    url: '/system/dict/type',
    method: 'post',
    data: data
  })
}

// 修改字典类型
export function updateType(data: SysDictType): Promise<AjaxResult<null>> {
  return request({
    url: '/system/dict/type',
    method: 'put',
    data: data
  })
}

// 删除字典类型
export function delType(dictId: number | string | number[]): Promise<AjaxResult<null>> {
  return request({
    url: '/system/dict/type/' + dictId,
    method: 'delete'
  })
}

// 刷新字典缓存
export function refreshCache(): Promise<AjaxResult<null>> {
  return request({
    url: '/system/dict/type/refreshCache',
    method: 'delete'
  })
}

// 获取字典选择框列表
export function optionselect(): Promise<AjaxResult<SysDictType[]>> {
  return request({
    url: '/system/dict/type/optionselect',
    method: 'get'
  })
}
