import request from '@/utils/request.ts'
import type { AjaxResult, TableDataInfo, BaseEntity, PageQuery } from '@/types/api'

/** 调度日志实体（后端 SysJobLog，sys_job_log 表） */
export interface SysJobLog extends BaseEntity {
  /** 日志ID */
  jobLogId?: number
  /** 任务名称 */
  jobName?: string
  /** 任务组名 */
  jobGroup?: string
  /** 调用目标字符串 */
  invokeTarget?: string
  /** 日志信息 */
  jobMessage?: string
  /** 执行状态（0正常 1失败） */
  status?: string
  /** 异常信息 */
  exceptionInfo?: string
  /** 开始时间 */
  startTime?: string
  /** 结束时间 */
  endTime?: string
}

/** 调度日志列表查询参数 */
export interface SysJobLogQuery extends PageQuery {
  /** 任务名称 */
  jobName?: string
  /** 任务组名 */
  jobGroup?: string
  /** 执行状态（0正常 1失败） */
  status?: string
  /** 执行时间范围-开始 */
  beginTime?: string
  /** 执行时间范围-结束 */
  endTime?: string
  /** 动态请求参数（后端 BaseEntity.params，addDateRange 注入 beginTime/endTime） */
  params?: Record<string, unknown>
}

// 查询调度日志列表
export function listJobLog(query: SysJobLogQuery): Promise<TableDataInfo<SysJobLog>> {
  return request({
    url: '/monitor/jobLog/list',
    method: 'get',
    params: query
  })
}

// 删除调度日志
export function delJobLog(jobLogId: number | string | number[]): Promise<AjaxResult<null>> {
  return request({
    url: '/monitor/jobLog/' + jobLogId,
    method: 'delete'
  })
}

// 清空调度日志
export function cleanJobLog(): Promise<AjaxResult<null>> {
  return request({
    url: '/monitor/jobLog/clean',
    method: 'delete'
  })
}
