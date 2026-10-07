/**
 * 全局 API 类型约定
 *
 * 后端统一响应封装（拦截器已解包 res.data，这里描述解包后的业务报文）：
 * - 单对象/标量：AjaxResult<T>  → { code, msg, data }
 * - 分页列表：  TableDataInfo<T> → { code, msg, rows, total }
 * - 无业务数据：AjaxResult<null>（仅判断 code）
 *
 * api 模块写法：
 *   export function listConfig(query: SysConfigQuery): Promise<TableDataInfo<SysConfig>> {
 *     return request({ url: '/system/config/list', method: 'get', params: query })
 *   }
 */

/** 后端统一响应封装（单对象） */
export interface AjaxResult<T = unknown> {
  /** 状态码：200 成功，其余见后端全局异常码 */
  code: number
  /** 提示消息 */
  msg: string
  /** 业务数据 */
  data: T
}

/** 后端统一响应封装（分页列表） */
export interface TableDataInfo<T = unknown> {
  /** 状态码：200 成功 */
  code: number
  /** 提示消息 */
  msg: string
  /** 当前页数据 */
  rows: T[]
  /** 总条数 */
  total: number
}

/** 实体公共审计字段（对应后端 BaseEntity） */
export interface BaseEntity {
  /** 创建者 */
  createBy?: string
  /** 创建时间 */
  createTime?: string
  /** 更新者 */
  updateBy?: string
  /** 更新时间 */
  updateTime?: string
  /** 备注 */
  remark?: string
  /** 请求参数集合（后端 dataScope 等动态注入） */
  params?: Record<string, unknown>
}

/** 通用分页查询参数（对应后端 PageHelper 分页入参） */
export interface PageQuery {
  /** 页码 */
  pageNum?: number
  /** 每页条数 */
  pageSize?: number
  /** 排序字段（orderByColumn） */
  orderByColumn?: string
  /** 排序方向（后端 PageHelper 接受 asc/desc；前端表格默认值可能是 descending） */
  isAsc?: string
}

/** 字典项（后端 SysDictData 的前端投影，全站通用） */
export interface DictDataOption {
  /** 字典标签 */
  label: string
  /** 字典值 */
  value: string
  /** el-tag 类型（对应 listClass） */
  elTagType?: 'primary' | 'success' | 'info' | 'warning' | 'danger' | 'default' | ''
  /** el-tag class（对应 cssClass） */
  elTagClass?: string
  /** 是否默认（Y/N） */
  isDefault?: string
  /** 状态（0 正常 1 停用） */
  status?: string
}

/** 树选择节点（菜单/部门树通用） */
export interface TreeSelectNode {
  /** 节点 ID */
  id: number
  /** 节点名称 */
  label: string
  /** 子节点 */
  children?: TreeSelectNode[]
  /** 是否禁用（部门树含停用部门时） */
  disabled?: boolean
}
