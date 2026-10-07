import request from '@/utils/request.ts'
import type { AjaxResult, TableDataInfo, PageQuery } from '@/types/api'

/** 库存实体（后端 SampleStock，不继承 BaseEntity） */
export interface SampleStock {
  /** 库存ID */
  stockId?: number
  /** 商品名称 */
  productName?: string
  /** 分类ID */
  categoryId?: number
  /** 库存数量 */
  quantity?: number
  /** 仓库 */
  warehouse?: string
  /** 更新时间 */
  updateTime?: string
  /** 更新者 */
  updateBy?: string
}

/** 库存列表查询参数 */
export interface StockQuery extends PageQuery {
  /** 商品名称 */
  productName?: string
  /** 仓库 */
  warehouse?: string
}

// 查询库存列表
export function listStock(query: StockQuery): Promise<TableDataInfo<SampleStock>> {
  return request({ url: '/sample/stock/list', method: 'get', params: query })
}

// 查询库存详细
export function getStock(stockId: number | string): Promise<AjaxResult<SampleStock>> {
  return request({ url: '/sample/stock/' + stockId, method: 'get' })
}

// 新增库存
export function addStock(data: SampleStock): Promise<AjaxResult<null>> {
  return request({ url: '/sample/stock', method: 'post', data: data })
}

// 修改库存
export function updateStock(data: SampleStock): Promise<AjaxResult<null>> {
  return request({ url: '/sample/stock', method: 'put', data: data })
}

// 删除库存（stockIds 为逗号拼接串）
export function delStock(stockIds: number | string): Promise<AjaxResult<null>> {
  return request({ url: '/sample/stock/' + stockIds, method: 'delete' })
}
