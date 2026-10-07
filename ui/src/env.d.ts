/// <reference types="vite/client" />

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<Record<string, unknown>, Record<string, unknown>, unknown>
  export default component
}

interface ImportMetaEnv {
  readonly VITE_APP_ENV?: 'development' | 'production' | 'staging'
  readonly VITE_APP_PROXY_TARGET?: string
  readonly VITE_APP_BASE_API?: string
  readonly VITE_APP_CLIENT_ID?: string
  readonly VITE_APP_ENCRYPT?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
