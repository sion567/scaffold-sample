import request from '@/utils/request.ts'
import type { AjaxResult, TableDataInfo, BaseEntity, PageQuery } from '@/types/api'

/** 订单明细实体（后端 SampleOrderItem，不继承 BaseEntity） */
export interface SampleOrderItem {
  /** 明细ID */
  itemId?: number
  /** 订单ID */
  orderId?: number
  /** 商品名称 */
  productName?: string
  /** 数量 */
  quantity?: number
  /** 单价 */
  price?: number
}

/** 审批流转历史（后端 FlowHistory） */
export interface FlowHistory {
  /** 业务ID */
  bizId?: string
  /** 流程编码 */
  processCode?: string
  /** 节点编码 */
  nodeCode?: string
  /** 审批动作（提交/通过/退回） */
  action?: string
  /** 操作人 */
  operator?: string
  /** 审批意见 */
  remark?: string
  /** 操作时间 */
  operateTime?: string
}

/** 样例订单实体（后端 SampleOrder） */
export interface SampleOrder extends BaseEntity {
  /** 订单ID */
  orderId?: number
  /** 订单号 */
  orderNo?: string
  /** 客户ID */
  customerId?: number
  /** 订单总金额 */
  totalAmount?: number
  /** 状态（draft待提交 audit审批中 approved已通过 rejected已退回） */
  status?: string
  /** 审批意见 */
  auditRemark?: string
  /** 订单明细 */
  items?: SampleOrderItem[]
}

/** 订单列表查询参数 */
export interface SampleOrderQuery extends PageQuery {
  /** 订单号 */
  orderNo?: string
  /** 状态 */
  status?: string
}

// 查询订单列表
export function listOrder(query: SampleOrderQuery): Promise<TableDataInfo<SampleOrder>> {
  return request({ url: '/sample/order/list', method: 'get', params: query })
}

// 查询订单详细（含明细）
export function getOrder(orderId: number | string): Promise<AjaxResult<SampleOrder>> {
  return request({ url: '/sample/order/' + orderId, method: 'get' })
}

// 新增订单（含明细）
export function addOrder(data: SampleOrder): Promise<AjaxResult<null>> {
  return request({ url: '/sample/order', method: 'post', data: data })
}

// 修改订单（明细全删全插）
export function updateOrder(data: SampleOrder): Promise<AjaxResult<null>> {
  return request({ url: '/sample/order', method: 'put', data: data })
}

// 删除订单（orderIds 为逗号拼接串）
export function delOrder(orderIds: number | string): Promise<AjaxResult<null>> {
  return request({ url: '/sample/order/' + orderIds, method: 'delete' })
}

// 提交订单进入审批流
export function submitOrder(orderId: number | string): Promise<AjaxResult<null>> {
  return request({ url: '/sample/order/submit/' + orderId, method: 'post' })
}

// 审批：pass=false 相邻退回提交人（返回信息位于 msg 字段）
export function auditOrder(orderId: number | string, pass: boolean, remark?: string): Promise<AjaxResult<null>> {
  return request({ url: '/sample/order/audit/' + orderId, method: 'post', params: { pass, remark } })
}

// 审批流转历史
export function flowHistory(orderId: number | string): Promise<AjaxResult<FlowHistory[]>> {
  return request({ url: '/sample/order/flow/' + orderId, method: 'get' })
}
