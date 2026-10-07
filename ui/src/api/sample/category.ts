import request from '@/utils/request.ts'
import type { AjaxResult, TableDataInfo, BaseEntity, PageQuery } from '@/types/api'

/** 商品分类实体（后端 SampleCategory） */
export interface SampleCategory extends BaseEntity {
  /** 分类ID */
  categoryId?: number
  /** 分类名称 */
  categoryName?: string
  /** 父分类ID */
  parentId?: number
  /** 祖级列表 */
  ancestors?: string
  /** 显示顺序 */
  orderNum?: number
  /** 状态（0正常 1停用） */
  status?: string
  /** 子分类 */
  children?: SampleCategory[]
}

/** 分类查询参数 */
export interface SampleCategoryQuery extends PageQuery {
  /** 分类名称 */
  categoryName?: string
  /** 状态（0正常 1停用） */
  status?: string
}

// 查询商品分类树
export function treeCategory(query?: SampleCategoryQuery): Promise<AjaxResult<SampleCategory[]>> {
  return request({ url: '/sample/category/tree', method: 'get', params: query })
}

// 查询分类列表（平铺）
export function listCategory(query?: SampleCategoryQuery): Promise<AjaxResult<SampleCategory[]>> {
  return request({ url: '/sample/category/list', method: 'get', params: query })
}

// 查询分类详细
export function getCategory(categoryId: number | string): Promise<AjaxResult<SampleCategory>> {
  return request({ url: '/sample/category/' + categoryId, method: 'get' })
}

// 新增分类
export function addCategory(data: SampleCategory): Promise<AjaxResult<null>> {
  return request({ url: '/sample/category', method: 'post', data: data })
}

// 修改分类
export function updateCategory(data: SampleCategory): Promise<AjaxResult<null>> {
  return request({ url: '/sample/category', method: 'put', data: data })
}

// 删除分类
export function delCategory(categoryId: number | string): Promise<AjaxResult<null>> {
  return request({ url: '/sample/category/' + categoryId, method: 'delete' })
}
