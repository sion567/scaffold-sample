import request from '@/utils/request.ts'
import type { AjaxResult, TableDataInfo, BaseEntity, PageQuery } from '@/types/api'

/** 字典数据实体（后端 SysDictData，sys_dict_data 表） */
export interface SysDictData extends BaseEntity {
  /** 字典编码 */
  dictCode?: number
  /** 字典排序 */
  dictSort?: number
  /** 字典标签 */
  dictLabel?: string
  /** 字典键值 */
  dictValue?: string
  /** 字典类型 */
  dictType?: string
  /** 样式属性（回显样式） */
  cssClass?: string
  /** 表格回显样式 */
  listClass?: string
  /** 是否默认（Y是 N否） */
  isDefault?: string
  /** 状态（0正常 1停用） */
  status?: string
}

/** 字典数据列表查询参数 */
export interface SysDictDataQuery extends PageQuery {
  /** 字典类型 */
  dictType?: string
  /** 字典标签 */
  dictLabel?: string
  /** 状态（0正常 1停用） */
  status?: string
}

// 查询字典数据列表
export function listData(query: SysDictDataQuery): Promise<TableDataInfo<SysDictData>> {
  return request({
    url: '/system/dict/data/list',
    method: 'get',
    params: query
  })
}

// 查询字典数据详细
export function getData(dictCode: number | string | number[]): Promise<AjaxResult<SysDictData>> {
  return request({
    url: '/system/dict/data/' + dictCode,
    method: 'get'
  })
}

// 根据字典类型查询字典数据信息
export function getDicts(dictType: string): Promise<AjaxResult<SysDictData[]>> {
  return request({
    url: '/system/dict/data/type/' + dictType,
    method: 'get'
  })
}

// 新增字典数据
export function addData(data: SysDictData): Promise<AjaxResult<null>> {
  return request({
    url: '/system/dict/data',
    method: 'post',
    data: data
  })
}

// 修改字典数据
export function updateData(data: SysDictData): Promise<AjaxResult<null>> {
  return request({
    url: '/system/dict/data',
    method: 'put',
    data: data
  })
}

// 删除字典数据
export function delData(dictCode: number | string | number[]): Promise<AjaxResult<null>> {
  return request({
    url: '/system/dict/data/' + dictCode,
    method: 'delete'
  })
}
