import request from '@/utils/request.ts'
import type { TableDataInfo, BaseEntity, PageQuery } from '@/types/api'

/** 操作日志实体（后端 SysOperLog，sys_oper_log 表） */
export interface SysOperLog extends BaseEntity {
  /** 日志主键 */
  operId?: number
  /** 模块标题 */
  title?: string
  /** 业务类型（0其它 1新增 2修改 3删除） */
  businessType?: number
  /** 业务类型数组（批量操作） */
  businessTypes?: number[]
  /** 方法名称 */
  method?: string
  /** 请求方式 */
  requestMethod?: string
  /** 操作类别（0其它 1后台用户 2手机端用户） */
  operatorType?: number
  /** 操作人员 */
  operName?: string
  /** 部门名称 */
  deptName?: string
  /** 请求URL */
  operUrl?: string
  /** 主机地址 */
  operIp?: string
  /** 操作地点 */
  operLocation?: string
  /** 请求参数 */
  operParam?: string
  /** 返回参数 */
  jsonResult?: string
  /** 操作状态（0正常 1异常） */
  status?: number
  /** 错误消息 */
  errorMsg?: string
  /** 操作时间 */
  operTime?: string
  /** 消耗时间（毫秒） */
  costTime?: number
  /** 用户ID */
  userId?: number
  /** 会话ID */
  sessionId?: string
  /** 业务Key */
  bizKey?: string
  /** 业务类型标识 */
  bizType?: string
  /** 事件类型标识 */
  eventType?: number
  /** 响应码 */
  resultCode?: number
  /** 请求IP */
  requestIp?: string
  /** 变更前值 */
  beforeValue?: string
  /** 变更后值 */
  afterValue?: string
}

/** 操作日志列表查询参数 */
export interface SysOperLogQuery extends PageQuery {
  /** 系统模块 */
  title?: string
  /** 操作人员 */
  operName?: string
  /** 操作类型 */
  businessType?: number | string
  /** 操作状态 */
  status?: number | string
  /** 操作IP */
  operIp?: string
  /** 操作时间范围-开始 */
  beginTime?: string
  /** 操作时间范围-结束 */
  endTime?: string
  /** 动态请求参数（后端 BaseEntity.params，addDateRange 注入 beginTime/endTime） */
  params?: Record<string, unknown>
}

// 查询操作日志列表（审计中心 ct-audit /audit/oper-log）
export function list(query: SysOperLogQuery): Promise<TableDataInfo<SysOperLog>> {
  return request({
    url: '/audit/oper-log',
    method: 'get',
    params: query
  })
}
