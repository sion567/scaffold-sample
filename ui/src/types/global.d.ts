import type { ComponentCustomProperties, ToRefs } from 'vue'
import type { AxiosRequestConfig } from 'axios'
import type { ElMessageBox } from 'element-plus'
import type { AjaxResult, DictDataOption } from '@/types/api'

// main.ts 通过 app.config.globalProperties 注册的模板全局函数类型声明，
// 使 <template> 中直接使用 parseTime/handleTree 等通过 vue-tsc 检查。
// 各实现签名见 src/utils/ct.ts、src/utils/dict.ts、src/plugins/*。

// 各全局插件类型直接取自实现对象，保证与真实签名同步
type ModalPlugin = typeof import('@/plugins/modal').default
type TabPlugin = typeof import('@/plugins/tab').default
type AuthPlugin = typeof import('@/plugins/auth').default
type CachePlugin = typeof import('@/plugins/cache').default
type DownloadPlugin = typeof import('@/plugins/download').default

declare module 'vue' {
  interface ComponentCustomProperties {
    useDict: (...args: string[]) => ToRefs<Record<string, DictDataOption[]>>
    download: (url: string, params?: unknown, filename?: string, config?: AxiosRequestConfig) => Promise<void>
    parseTime: (time?: string | number | Date | null, pattern?: string) => string | null
    resetForm: (refName: string) => void
    handleTree: <T extends object>(data: T[], id?: string, parentId?: string, children?: string) => T[]
    addDateRange: <T extends object>(params: T, dateRange?: string[] | null, propName?: string) => T
    getConfigKey: (configKey: string) => Promise<AjaxResult<null>>
    selectDictLabel: (dicts: DictDataOption[], value: unknown) => string
    selectDictLabels: (dicts: DictDataOption[], value: string | string[], separator?: string) => string
    $alert: typeof ElMessageBox.alert
    $modal: ModalPlugin
    $download: DownloadPlugin
    $tab: TabPlugin
    $cache: CachePlugin
    $auth: AuthPlugin
  }
}

export {}
