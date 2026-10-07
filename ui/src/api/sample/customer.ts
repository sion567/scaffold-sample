import request from '@/utils/request.ts'
import type { AjaxResult, TableDataInfo, BaseEntity, PageQuery } from '@/types/api'

/** 样例客户实体（后端 SampleCustomer） */
export interface SampleCustomer extends BaseEntity {
  /** 客户ID */
  customerId?: number
  /** 客户姓名 */
  customerName?: string
  /** 联系电话 */
  phone?: string
  /** 邮箱 */
  email?: string
  /** 状态（0正常 1停用） */
  status?: string
}

/** 客户列表查询参数 */
export interface SampleCustomerQuery extends PageQuery {
  /** 客户姓名 */
  customerName?: string
  /** 状态（0正常 1停用） */
  status?: string
}

// 查询样例客户列表
export function listCustomer(query: SampleCustomerQuery): Promise<TableDataInfo<SampleCustomer>> {
  return request({ url: '/sample/customer/list', method: 'get', params: query })
}

// 查询样例客户详细
export function getCustomer(customerId: number | string): Promise<AjaxResult<SampleCustomer>> {
  return request({ url: '/sample/customer/' + customerId, method: 'get' })
}

// 新增样例客户
export function addCustomer(data: SampleCustomer): Promise<AjaxResult<null>> {
  return request({ url: '/sample/customer', method: 'post', data: data })
}

// 修改样例客户
export function updateCustomer(data: SampleCustomer): Promise<AjaxResult<null>> {
  return request({ url: '/sample/customer', method: 'put', data: data })
}

// 删除样例客户（customerIds 为逗号拼接串）
export function delCustomer(customerIds: number | string): Promise<AjaxResult<null>> {
  return request({ url: '/sample/customer/' + customerIds, method: 'delete' })
}
