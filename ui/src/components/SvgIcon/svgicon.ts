import type { App } from 'vue'
import * as components from '@element-plus/icons-vue'

/** 图标组件名 → 组件 的映射（icons-vue 每个导出都带 name 静态属性） */
type IconComponent = (typeof components)[keyof typeof components] & { name: string }

export default {
  install: (app: App) => {
    for (const key in components) {
      const componentConfig = components[key as keyof typeof components] as IconComponent
      app.component(componentConfig.name, componentConfig)
    }
  }
}
