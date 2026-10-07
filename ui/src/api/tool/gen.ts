import request from '@/utils/request.ts'
import type { AjaxResult, TableDataInfo, BaseEntity, PageQuery } from '@/types/api'

/** 代码生成业务表实体（后端 GenTable，gen_table 表） */
export interface GenTable extends BaseEntity {
  /** 编号 */
  tableId?: number
  /** 表名称 */
  tableName?: string
  /** 表描述 */
  tableComment?: string
  /** 关联父表的表名 */
  subTableName?: string
  /** 本表的外键名（关联父表） */
  subTableFkName?: string
  /** 实体类名称（全小写） */
  className?: string
  /** 使用的模板（crud 树表 sub） */
  tplCategory?: string
  /** 前端类型（element-ui / element-plus） */
  tplWebType?: string
  /** 生成包路径 */
  packageName?: string
  /** 生成模块名 */
  moduleName?: string
  /** 生成业务名 */
  businessName?: string
  /** 生成功能名 */
  functionName?: string
  /** 生成作者 */
  functionAuthor?: string
  /** 表单布局（单列/双列/三列） */
  formColNum?: number
  /** 生成代码方式（0zip压缩包 1自定义路径） */
  genType?: string
  /** 生成路径（不填默认项目路径） */
  genPath?: string
  /** 主键信息 */
  pkColumn?: GenTableColumn
  /** 子表信息 */
  subTable?: GenTable
  /** 表列信息 */
  columns?: GenTableColumn[]
  /** 其它生成选项 */
  options?: string
  /** 是否生成详情页（前端扩展字段，genInfoForm 勾选回显） */
  view?: boolean
  /** 树编码字段 */
  treeCode?: string
  /** 树父编码字段 */
  treeParentCode?: string
  /** 树名称字段 */
  treeName?: string
  /** 上级菜单ID */
  parentMenuId?: number
  /** 上级菜单名称 */
  parentMenuName?: string
  /** 是否视图 */
  isView?: boolean
}

/** 代码生成表列实体（后端 GenTableColumn，gen_table_column 表） */
export interface GenTableColumn extends BaseEntity {
  /** 编号 */
  columnId?: number
  /** 归属表编号 */
  tableId?: number
  /** 列名称 */
  columnName?: string
  /** 列描述 */
  columnComment?: string
  /** 数据库列类型 */
  columnType?: string
  /** JAVA类型 */
  javaType?: string
  /** JAVA字段名 */
  javaField?: string
  /** 是否主键（1是） */
  isPk?: string
  /** 是否自增（1是） */
  isIncrement?: string
  /** 是否必填（1是） */
  isRequired?: string
  /** 是否为插入字段（1是） */
  isInsert?: string
  /** 是否编辑字段（1是） */
  isEdit?: string
  /** 是否列表字段（1是） */
  isList?: string
  /** 是否查询字段（1是） */
  isQuery?: string
  /** 查询方式（EQ LIKE GT等） */
  queryType?: string
  /** 显示类型（文本框 文本域 下拉框等） */
  htmlType?: string
  /** 字典类型 */
  dictType?: string
  /** 排序 */
  sort?: number
}

/** 生成表列表查询参数 */
export interface GenTableQuery extends PageQuery {
  /** 表名称 */
  tableName?: string
  /** 表描述 */
  tableComment?: string
  /** 创建时间范围-开始 */
  beginTime?: string
  /** 创建时间范围-结束 */
  endTime?: string
  /** 动态请求参数（后端 BaseEntity.params，addDateRange 注入 beginTime/endTime） */
  params?: Record<string, unknown>
}

/** 表详情业务数据（后端 getInfo 的 data：info + rows + tables 组合） */
export interface GenTableDetail {
  /** 表信息 */
  info: GenTable
  /** 表列信息 */
  rows: GenTableColumn[]
  /** 全部业务表（用于关联子表选择） */
  tables: GenTable[]
}

// 查询生成表数据
export function listTable(query: GenTableQuery): Promise<TableDataInfo<GenTable>> {
  return request({
    url: '/tool/gen/list',
    method: 'get',
    params: query
  })
}
// 查询db数据库列表
export function listDbTable(query: GenTableQuery): Promise<TableDataInfo<GenTable>> {
  return request({
    url: '/tool/gen/db/list',
    method: 'get',
    params: query
  })
}

// 查询表详细信息
export function getGenTable(tableId: number | string | number[]): Promise<AjaxResult<GenTableDetail>> {
  return request({
    url: '/tool/gen/' + tableId,
    method: 'get'
  })
}

// 修改代码生成信息
export function updateGenTable(data: GenTable): Promise<AjaxResult<null>> {
  return request({
    url: '/tool/gen',
    method: 'put',
    data: data
  })
}

// 导入表（tables 为逗号拼接表名）
export function importTable(data: { tables: string | number[]; tplWebType?: string }): Promise<AjaxResult<null>> {
  return request({
    url: '/tool/gen/importTable',
    method: 'post',
    params: data
  })
}

// 创建表（sql 为建表语句）
export function createTable(data: { sql: string; tplWebType?: string }): Promise<AjaxResult<null>> {
  return request({
    url: '/tool/gen/createTable',
    method: 'post',
    params: data
  })
}

// 预览生成代码（data 为 模板名 → 代码内容 映射）
export function previewTable(tableId: number | string | number[]): Promise<AjaxResult<Record<string, string>>> {
  return request({
    url: '/tool/gen/preview/' + tableId,
    method: 'get'
  })
}

// 删除表数据
export function delTable(tableId: number | string | number[]): Promise<AjaxResult<null>> {
  return request({
    url: '/tool/gen/' + tableId,
    method: 'delete'
  })
}

// 批量生成代码（下载 zip，响应为二进制流；tables 为逗号拼接表名）
export function batchGenCode(tables: string | number[]): Promise<Blob> {
  return request({
    url: '/tool/gen/batchGenCode',
    method: 'get',
    params: { tables: tables },
    responseType: 'blob'
  })
}

// 生成代码（自定义路径）
export function genCode(tableName: string): Promise<AjaxResult<null>> {
  return request({
    url: '/tool/gen/genCode/' + tableName,
    method: 'get'
  })
}

// 同步数据库
export function synchDb(tableName: string): Promise<AjaxResult<null>> {
  return request({
    url: '/tool/gen/synchDb/' + tableName,
    method: 'get'
  })
}
