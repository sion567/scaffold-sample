/**
 * 通用js方法封装处理
 * Copyright (c) 2019 ct
 */

import type { DictDataOption } from '@/types/api'

// 日期格式化
export function parseTime(time?: string | number | Date | null, pattern?: string) {
  if (arguments.length === 0 || !time) {
    return null
  }
  const format = pattern || '{y}-{m}-{d} {h}:{i}:{s}'
  let date
  if (typeof time === 'object') {
    date = time
  } else {
    if ((typeof time === 'string') && (/^[0-9]+$/.test(time))) {
      time = parseInt(time)
    } else if (typeof time === 'string') {
      time = time.replace(new RegExp(/-/gm), '/').replace('T', ' ').replace(new RegExp(/\.[\d]{3}/gm), '')
    }
    if ((typeof time === 'number') && (time.toString().length === 10)) {
      time = time * 1000
    }
    date = new Date(time)
  }
  const formatObj: Record<string, number> = {
    y: date.getFullYear(),
    m: date.getMonth() + 1,
    d: date.getDate(),
    h: date.getHours(),
    i: date.getMinutes(),
    s: date.getSeconds(),
    a: date.getDay()
  }
  const time_str = format.replace(/{(y|m|d|h|i|s|a)+}/g, (result: string, key: string) => {
    const value = formatObj[key]
    // Note: getDay() returns 0 on Sunday
    if (key === 'a') { return ['日', '一', '二', '三', '四', '五', '六'][value] }
    if (result.length > 0 && value < 10) {
      // 补零后直接返回（与原实现先赋值再 `value || 0` 的结果一致）
      return '0' + value
    }
    return String(value || 0)
  })
  return time_str
}

// 表单重置
export function resetForm(this: { $refs: Record<string, { resetFields: () => void } | undefined> }, refName: string) {
  if (this.$refs[refName]) {
    this.$refs[refName].resetFields()
  }
}

// 添加日期范围
export function addDateRange<T extends object>(params: T, dateRange?: string[] | null, propName?: string): T {
  const search = params as Record<string, unknown>
  search.params = typeof (search.params) === 'object' && search.params !== null && !Array.isArray(search.params) ? search.params : {}
  dateRange = Array.isArray(dateRange) ? dateRange : []
  const rangeParams = search.params as Record<string, unknown>
  if (typeof (propName) === 'undefined') {
    rangeParams['beginTime'] = dateRange[0]
    rangeParams['endTime'] = dateRange[1]
  } else {
    rangeParams['begin' + propName] = dateRange[0]
    rangeParams['end' + propName] = dateRange[1]
  }
  return params
}

// 回显数据字典
export function selectDictLabel(datas: DictDataOption[], value: unknown) {
  if (value === undefined) {
    return ""
  }
  const actions: unknown[] = []
  datas.some((item) => {
    if (item.value == ('' + value)) {
      actions.push(item.label)
      return true
    }
    return false
  })
  if (actions.length === 0) {
    actions.push(value)
  }
  return actions.join('')
}

// 回显数据字典（字符串、数组）
export function selectDictLabels(datas: DictDataOption[], value: string | string[], separator?: string) {
  if (value === undefined || value.length ===0) {
    return ""
  }
  if (Array.isArray(value)) {
    value = value.join(",")
  }
  const actions: unknown[] = []
  const currentSeparator = undefined === separator ? "," : separator
  const temp = value.split(currentSeparator)
  temp.forEach((val) => {
    let match = false
    datas.forEach((item) => {
      if (item.value == ('' + val)) {
        actions.push(item.label + currentSeparator)
        match = true
      }
    })
    if (!match) {
      actions.push(val + currentSeparator)
    }
  })
  return actions.join('').substring(0, actions.join('').length - 1)
}

// 字符串格式化(%s )
export function sprintf(this: unknown, str: string, ...args: unknown[]) {
  let flag = true, i = 0
  str = str.replace(/%s/g, () => {
    const arg = args[i++]
    if (typeof arg === 'undefined') {
      flag = false
      return ''
    }
    return String(arg)
  })
  return flag ? str : ''
}

// 转换字符串，undefined,null等转化为""
export function parseStrEmpty(str?: string | null | undefined) {
  if (!str || str == "undefined" || str == "null") {
    return ""
  }
  return str
}

// 数据合并
export function mergeRecursive<T extends object, S extends object>(source: T, target: S): T & S {
  for (const p in target) {
    try {
      const sourceRecord = source as Record<string, unknown>
      const targetValue = (target as Record<string, unknown>)[p]
      if ((targetValue as { constructor: unknown }).constructor == Object) {
        sourceRecord[p] = mergeRecursive(sourceRecord[p] as object, targetValue as object) as unknown
      } else {
        sourceRecord[p] = targetValue
      }
    } catch (e) {
      (source as Record<string, unknown>)[p] = (target as Record<string, unknown>)[p]
    }
  }
  return source as T & S
}

/**
 * 构造树型结构数据
 * @param {*} data 数据源
 * @param {*} id id字段 默认 'id'
 * @param {*} parentId 父节点字段 默认 'parentId'
 * @param {*} children 孩子节点字段 默认 'children'
 */
export function handleTree<T extends object>(data: T[], id?: string, parentId?: string, children?: string): T[] {
  const config = {
    id: id || 'id',
    parentId: parentId || 'parentId',
    childrenList: children || 'children'
  }

  const childrenListMap: Record<string, T> = {}
  const tree: T[] = []
  for (const d of data) {
    const row = d as Record<string, unknown>
    const key = row[config.id] as unknown as string
    childrenListMap[key] = d
    if (!row[config.childrenList]) {
      row[config.childrenList] = []
    }
  }

  for (const d of data) {
    const row = d as Record<string, unknown>
    const parentKey = row[config.parentId] as unknown as string
    const parentObj = childrenListMap[parentKey]
    if (!parentObj) {
      tree.push(d)
    } else {
      const childrenList = (parentObj as Record<string, unknown>)[config.childrenList] as T[]
      childrenList.push(d)
    }
  }
  return tree
}

/**
* 参数处理
* @param {*} params  参数
*/
export function tansParams(params: Record<string, unknown>) {
  let result = ''
  for (const propName of Object.keys(params)) {
    const value = params[propName]
    const part = encodeURIComponent(propName) + "="
    if (value !== null && value !== "" && typeof (value) !== "undefined") {
      if (typeof value === 'object') {
        const valueRecord = value as Record<string, unknown>
        for (const key of Object.keys(valueRecord)) {
          if (valueRecord[key] !== null && valueRecord[key] !== "" && typeof (valueRecord[key]) !== 'undefined') {
            const params = propName + '[' + key + ']'
            const subPart = encodeURIComponent(params) + "="
            result += subPart + encodeURIComponent(String(valueRecord[key])) + "&"
          }
        }
      } else {
        result += part + encodeURIComponent(String(value)) + "&"
      }
    }
  }
  return result
}

// 返回项目路径
export function getNormalPath(p: string) {
  if (p.length === 0 || !p || p == 'undefined') {
    return p
  }
  const res = p.replace('//', '/')
  if (res[res.length - 1] === '/') {
    return res.slice(0, res.length - 1)
  }
  return res
}

// 验证是否为blob格式
export function blobValidate(data: unknown) {
  return (data as { type?: unknown }).type !== 'application/json'
}
