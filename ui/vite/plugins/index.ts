import type { Plugin } from 'vite'
import vue from '@vitejs/plugin-vue'

import createAutoImport from './auto-import'
import createSvgIcon from './svg-icon'
import createCompression from './compression'
import createSetupExtend from './setup-extend'

export default function createVitePlugins(viteEnv: Record<string, string | undefined>, isBuild = false) {
  const vitePlugins: Plugin[] = [vue()]
  const pushAll = (plugin: Plugin | Plugin[]) => vitePlugins.push(...(Array.isArray(plugin) ? plugin : [plugin]))
  pushAll(createAutoImport())
  pushAll(createSetupExtend())
  pushAll(createSvgIcon(isBuild))
  if (isBuild) {
    for (const p of createCompression(viteEnv)) pushAll(p)
  }
  return vitePlugins
}
