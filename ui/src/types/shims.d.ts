// 无官方类型定义的第三方模块声明

declare module 'sm-crypto' {
  export const sm2: {
    doEncrypt(msg: string, publicKey: string, cipherMode?: number): string
    doDecrypt(encryptData: string, privateKey: string, cipherMode?: number): string
    doSignature(msg: string, privateKey: string, options?: { hash?: boolean; der?: boolean }): string
    doVerifySignature(msg: string, sign: string, publicKey: string, options?: { hash?: boolean; der?: boolean }): boolean
  }
  export const sm3: {
    (msg: string): string
  }
  export const sm4: {
    encrypt(msg: ArrayLike<number> | string, key: ArrayLike<number> | string, options?: { padding?: string; mode?: string; iv?: ArrayLike<number> | string }): string
    decrypt(msg: ArrayLike<number> | string, key: ArrayLike<number> | string, options?: { padding?: string; mode?: string; iv?: ArrayLike<number> | string }): string
  }
}

declare module 'sortablejs' {
  export type SortableEvent = Event & {
    oldIndex?: number
    newIndex?: number
    item: HTMLElement
    from: HTMLElement
    to: HTMLElement
    clone: HTMLElement
  }
  export interface SortableOptions {
    handle?: string
    draggable?: string
    animation?: number
    disabled?: boolean
    ghostClass?: string
    chosenClass?: string
    dragClass?: string
    filter?: string | ((event: Event) => boolean)
    onEnd?: (event: SortableEvent) => void
    onAdd?: (event: SortableEvent) => void
    onUpdate?: (event: SortableEvent) => void
    [key: string]: unknown
  }
  export default class Sortable {
    constructor(el: HTMLElement, options?: SortableOptions)
    option(name: string, value?: unknown): unknown
    destroy(): void
    static create(el: HTMLElement, options?: SortableOptions): Sortable
  }
}

// vuedraggable 官方类型只覆盖主入口，本项目深度导入 common 构建以走 CJS 兼容路径
declare module 'vuedraggable/dist/vuedraggable.common' {
  import type { DefineComponent } from 'vue'
  const Draggable: DefineComponent<{
    /** 绑定的数组（与 modelValue 二选一） */
    list?: unknown[]
    modelValue?: unknown[]
    itemKey?: string | ((item: never) => unknown)
    clone?: (original: unknown) => unknown
    tag?: string
    move?: (...args: unknown[]) => unknown
    componentData?: Record<string, unknown>
    group?: string | Record<string, unknown>
    animation?: number
    handle?: string
    [key: string]: unknown
  }, Record<string, unknown>, unknown>
  export default Draggable
}

// js-beautify 无官方类型，本项目仅用到 html/js/css 格式化入口
declare module 'js-beautify' {
  interface BeautifyOptions {
    [key: string]: unknown
  }
  const beautifier: {
    html(source: string, options?: BeautifyOptions): string
    js(source: string, options?: BeautifyOptions): string
    css(source: string, options?: BeautifyOptions): string
  }
  export default beautifier
}
