import request from '@/utils/request.ts'
import type { AjaxResult } from '@/types/api'

/** CPU 信息 */
export interface CpuInfo {
  /** 核心数 */
  cpuNum?: number
  /** CPU 总使用率 */
  total?: number
  /** CPU 系统使用率 */
  sys?: number
  /** CPU 用户使用率 */
  used?: number
  /** CPU 当前等待率 */
  wait?: number
  /** CPU 当前空闲率 */
  free?: number
}

/** 内存信息 */
export interface MemInfo {
  /** 内存总量 */
  total?: number
  /** 内存已用 */
  used?: number
  /** 内存剩余 */
  free?: number
  /** 内存使用率 */
  usage: number
}

/** JVM 信息 */
export interface JvmInfo {
  /** JVM 总内存 */
  total?: number
  /** JVM 最大内存 */
  max?: number
  /** JVM 空闲内存 */
  free?: number
  /** JVM 已用内存 */
  used?: number
  /** JVM 使用率 */
  usage: number
  /** JDK 版本 */
  version?: string
  /** JDK 安装路径 */
  home?: string
  /** JVM 名称 */
  name?: string
  /** JVM 启动时间 */
  startTime?: string
  /** JVM 运行时长 */
  runTime?: string
  /** JVM 启动参数 */
  inputArgs?: string
}

/** 系统信息 */
export interface SysInfo {
  /** 服务器名称 */
  computerName?: string
  /** 服务器 IP */
  computerIp?: string
  /** 操作系统架构 */
  osArch?: string
  /** 操作系统名称 */
  osName?: string
  /** 项目路径 */
  userDir?: string
}

/** 磁盘信息 */
export interface SysFileInfo {
  /** 盘符路径 */
  dirName?: string
  /** 盘符类型 */
  sysTypeName?: string
  /** 文件类型 */
  typeName?: string
  /** 总大小 */
  total?: string
  /** 剩余大小 */
  free?: string
  /** 已经使用量 */
  used?: string
  /** 资源的使用率 */
  usage: number
}

/** 服务监控信息（后端 /monitor/server 响应 data） */
export interface ServerInfo {
  /** CPU 相关信息 */
  cpu?: CpuInfo
  /** 内存相关信息 */
  mem?: MemInfo
  /** JVM 相关信息 */
  jvm?: JvmInfo
  /** 服务器相关信息 */
  sys?: SysInfo
  /** 磁盘相关信息 */
  sysFiles?: SysFileInfo[]
}

// 获取服务信息
export function getServer(): Promise<AjaxResult<ServerInfo>> {
  return request({
    url: '/monitor/server',
    method: 'get'
  })
}
