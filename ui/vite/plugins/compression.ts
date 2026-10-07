import type { Plugin } from 'vite'
import compression from 'vite-plugin-compression'

export default function createCompression(env: Record<string, string | undefined>) {
  const { VITE_BUILD_COMPRESS } = env
  const compress = VITE_BUILD_COMPRESS ?? ''
  const plugin: Plugin[] = []
  if (compress) {
    const compressList = compress.split(',')
    if (compressList.includes('gzip')) {
      // gzip 静态资源压缩说明见 vite 官方文档
      plugin.push(
        compression({
          ext: '.gz',
          deleteOriginFile: false
        })
      )
    }
    if (compressList.includes('brotli')) {
      plugin.push(
        compression({
          ext: '.br',
          algorithm: 'brotliCompress',
          deleteOriginFile: false
        })
      )
    }
  }
  return plugin
}
