import { mount, type VueWrapper } from '@vue/test-utils'
import type { Component, ComponentPublicInstance } from 'vue'
import { createPinia, setActivePinia, type Pinia } from 'pinia'
import ElementPlus from 'element-plus'
import useUserStore from '@/store/modules/user.ts'
import hasPermi from '@/directive/permission/hasPermi.ts'
import hasRole from '@/directive/permission/hasRole.ts'

// jsdom 缺 ResizeObserver/scrollTo，ElementPlus 表格需要
interface JsdomGlobal {
  ResizeObserver?: new () => { observe(): void; unobserve(): void; disconnect(): void }
  scrollTo?: (x: number, y: number) => void
}
const g = globalThis as unknown as JsdomGlobal
if (typeof g.ResizeObserver === 'undefined') {
  g.ResizeObserver = class {
    observe() {}
    unobserve() {}
    disconnect() {}
  }
}
if (typeof g.scrollTo === 'undefined') {
  g.scrollTo = () => {}
}

export interface MountPageOptions {
  /** 当前用户权限串（v-hasPermi 判定用）；默认 *:*:* 全量 */
  permissions?: string[]
  /** 当前用户名 */
  name?: string
}

/** 挂载业务页面：注入 pinia（预置权限）+ ElementPlus + hasPermi/hasRole 指令 */
export function mountPage(component: Component, options: MountPageOptions = {}): VueWrapper<ComponentPublicInstance> {
  const pinia: Pinia = createPinia()
  setActivePinia(pinia)
  const store = useUserStore(pinia)
  store.permissions = options.permissions ?? ['*:*:*']
  store.name = options.name ?? 'admin'
  store.id = 1

  return mount(component, {
    global: {
      plugins: [pinia, ElementPlus],
      directives: { hasPermi, hasRole }
    }
  })
}
