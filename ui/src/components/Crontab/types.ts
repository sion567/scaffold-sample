/**
 * Crontab 组件共享类型
 */

/** cron 表达式七个字段（每一位的字符串值） */
export interface CronValue {
  /** 秒 */
  second: string
  /** 分 */
  min: string
  /** 时 */
  hour: string
  /** 日 */
  day: string
  /** 月 */
  month: string
  /** 周 */
  week: string
  /** 年 */
  year: string
}

/** cron 字段名（子组件 update 事件的第一参数） */
export type CronFieldKey = keyof CronValue

/** 子组件数字范围校验函数（父组件 checkNumber 传入，越界自动回夹） */
export type CronCheckFn = (value: number, minLimit: number, maxLimit: number) => number

/** 子组件公共 props（cron 当前值集合 + 数字校验函数） */
export interface CronFieldProps {
  /** 当前 cron 表达式字段值集合 */
  cron?: CronValue
  /** 数字范围校验函数 */
  check: CronCheckFn
}

/** 子组件默认的 cron 值（全通配） */
export const defaultCronValue: CronValue = {
  second: '*',
  min: '*',
  hour: '*',
  day: '*',
  month: '*',
  week: '?',
  year: ''
}

/** 子组件 update 事件签名 */
export type CronUpdateEmits = (e: 'update', name: CronFieldKey, value: string, from: string) => void
