import request from '@/utils/request.ts'
import type { AjaxResult, TableDataInfo, BaseEntity, PageQuery } from '@/types/api'

/** 定时任务实体（后端 SysJob，sys_job 表） */
export interface SysJob extends BaseEntity {
  /** 任务ID */
  jobId?: number
  /** 任务名称 */
  jobName?: string
  /** 任务组名（DEFAULT SYSTEM） */
  jobGroup?: string
  /** 调用目标字符串 */
  invokeTarget?: string
  /** cron执行表达式 */
  cronExpression?: string
  /** 计划执行错误策略（1立即执行 2执行一次 3放弃执行） */
  misfirePolicy?: string
  /** 是否并发执行（0允许 1禁止） */
  concurrent?: string
  /** 任务状态（0正常 1暂停） */
  status?: string
  /** 下次执行时间（后端任务详情返回） */
  nextValidTime?: string
}

/** 定时任务列表查询参数 */
export interface SysJobQuery extends PageQuery {
  /** 任务名称 */
  jobName?: string
  /** 任务组名 */
  jobGroup?: string
  /** 任务状态（0正常 1暂停） */
  status?: string
}

// 查询定时任务调度列表
export function listJob(query: SysJobQuery): Promise<TableDataInfo<SysJob>> {
  return request({
    url: '/monitor/job/list',
    method: 'get',
    params: query
  })
}

// 查询定时任务调度详细
export function getJob(jobId: number | string | number[]): Promise<AjaxResult<SysJob>> {
  return request({
    url: '/monitor/job/' + jobId,
    method: 'get'
  })
}

// 新增定时任务调度
export function addJob(data: SysJob): Promise<AjaxResult<null>> {
  return request({
    url: '/monitor/job',
    method: 'post',
    data: data
  })
}

// 修改定时任务调度
export function updateJob(data: SysJob): Promise<AjaxResult<null>> {
  return request({
    url: '/monitor/job',
    method: 'put',
    data: data
  })
}

// 删除定时任务调度
export function delJob(jobId: number | string | number[]): Promise<AjaxResult<null>> {
  return request({
    url: '/monitor/job/' + jobId,
    method: 'delete'
  })
}

// 任务状态修改
export function changeJobStatus(jobId: number | string | number[], status: string): Promise<AjaxResult<null>> {
  const data = {
    jobId,
    status
  }
  return request({
    url: '/monitor/job/changeStatus',
    method: 'put',
    data: data
  })
}


// 定时任务立即执行一次
export function runJob(jobId: number | string | number[], jobGroup: string): Promise<AjaxResult<null>> {
  const data = {
    jobId,
    jobGroup
  }
  return request({
    url: '/monitor/job/run',
    method: 'put',
    data: data
  })
}
