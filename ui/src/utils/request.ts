import axios from 'axios'
import type { AxiosRequestConfig, InternalAxiosRequestConfig } from 'axios'
import { ElNotification , ElMessageBox, ElMessage, ElLoading } from 'element-plus'
import { getToken } from '@/utils/auth.ts'
import errorCode from '@/utils/errorCode.ts'
import { tansParams, blobValidate } from '@/utils/ct.ts'
import cache from '@/plugins/cache.ts'
import { saveAs } from 'file-saver'
import useUserStore from '@/store/modules/user.ts'
import { applyGmEnvelope, isEnvelopeCandidate, ensureSignKeys, initSignKeys, clearSignKeys } from '@/utils/gmEnvelope.ts'

// 国密信封重放所需的原始报文缓存（请求拦截器加密前暂存，响应拦截器重放用）
declare module 'axios' {
  export interface InternalAxiosRequestConfig {
    __gmOrig?: { url?: string; params?: Record<string, unknown>; data?: unknown }
    __gmRetried?: boolean
  }
}

let downloadLoadingInstance: ReturnType<typeof ElLoading.service> | undefined
// 是否显示重新登录
export const isRelogin = { show: false }

axios.defaults.headers['Content-Type'] = 'application/json;charset=utf-8'
// 创建axios实例
const service = axios.create({
  // axios中请求配置有baseURL选项，表示请求URL公共部分
  baseURL: import.meta.env.VITE_APP_BASE_API,
  // 超时
  timeout: 10000
})

// request拦截器
service.interceptors.request.use(async config => {
  // 是否需要设置 token
  const isToken = (config.headers || {}).isToken === false
  // 是否需要防止数据重复提交
  const isRepeatSubmit = (config.headers || {}).repeatSubmit === false
  // 间隔时间(ms)，小于此时间视为重复提交
  const interval = (config.headers || {}).interval || 1000
  if (getToken() && !isToken) {
    config.headers['Authorization'] = 'Bearer ' + getToken() // 让每个请求携带自定义token 请根据实际情况自行修改
  }
  // 透传 TraceId
  const traceId = localStorage.getItem('traceId')
  if (traceId) {
    config.headers['X-Trace-Id'] = traceId
  }
  // 防重复提交：必须用信封加密前的原始数据判重（密文里 nonce 每次都变，判重会失效）
  if (!isRepeatSubmit && (config.method === 'post' || config.method === 'put')) {
    const requestObj = {
      url: config.url,
      data: typeof config.data === 'object' ? JSON.stringify(config.data) : config.data,
      time: new Date().getTime()
    }
    const requestSize = Object.keys(JSON.stringify(requestObj)).length // 请求数据大小
    const limitSize = 5 * 1024 * 1024 // 限制存放数据5M
    if (requestSize >= limitSize) {
      console.warn(`[${config.url}]: ` + '请求数据大小超出允许的5M限制，无法进行防重复提交验证。')
      return config
    }
    const sessionObj = cache.session.getJSON<{ url?: string; data?: unknown; time?: number } | ''>('sessionObj')
    if (sessionObj === undefined || sessionObj === null || sessionObj === '') {
      cache.session.setJSON('sessionObj', requestObj)
    } else {
      const s_url = sessionObj.url                // 请求地址
      const s_data = sessionObj.data              // 请求数据
      const s_time = sessionObj.time              // 请求时间
      if (s_data === requestObj.data && requestObj.time - s_time! < interval && s_url === requestObj.url) {
        const message = '数据正在处理，请勿重复提交'
        console.warn(`[${s_url}]: ` + message)
        return Promise.reject(new Error(message))
      } else {
        cache.session.setJSON('sessionObj', requestObj)
      }
    }
  }
  // 国密信封加密：随机 SM4 加密业务数据 + SM2 加密 SM4 密钥 + 会话 SM2 签名
  // 已登录且钥未就绪时先向后端签发会话签名密钥（后端对带 token 的请求强制验签）
  if (isEnvelopeCandidate(config) && !isToken && getToken()) {
    await ensureSignKeys()
  }
  // 失败时阻断请求：安全开关打开的后端会拒绝非信封请求，早失败早暴露问题
  if (isEnvelopeCandidate(config)) {
    await applyGmEnvelope(config)
  }
  // get请求映射params参数（GET 已约定走明文，不进信封，params 正常拼接）
  if (config.method === 'get' && config.params) {
    let url = config.url + '?' + tansParams(config.params)
    url = url.slice(0, -1)
    config.params = {}
    config.url = url
  }
  return config
}, error => {
    console.log(error)
    Promise.reject(error)
})

// 响应拦截器
service.interceptors.response.use(async res => {
    // 提取并存储 TraceId
    const traceId = res.headers['x-trace-id'] || res.headers['X-Trace-Id']
    if (traceId) {
      localStorage.setItem('traceId', traceId)
    }
    // 未设置状态码则默认成功状态
    const code: number = res.data.code || 200
    // 获取错误信息（message 兼容国密过滤器 4000x 报文）
    const msg = errorCode[code] || res.data.msg || res.data.message || errorCode['default']
    // 二进制数据则直接返回
    if (res.request.responseType ===  'blob' || res.request.responseType ===  'arraybuffer') {
      return res.data
    }
    if (code === 401) {
      if (!isRelogin.show) {
        isRelogin.show = true
        ElMessageBox.confirm('登录状态已过期，您可以继续留在该页面，或者重新登录', '系统提示', { confirmButtonText: '重新登录', cancelButtonText: '取消', type: 'warning' }).then(() => {
          isRelogin.show = false
          useUserStore().logOut().then(() => {
            location.href = '/index'
          })
      }).catch(() => {
        isRelogin.show = false
      })
    }
      return Promise.reject('无效的会话，或者会话已过期，请重新登录。')
    } else if (code === 500) {
      ElMessage({ message: msg, type: 'error' })
      return Promise.reject(new Error(msg))
    } else if (code === 601) {
      ElMessage({ message: msg, type: 'warning' })
      return Promise.reject(new Error(msg))
    } else if (code === 40006 || code === 40003) {
      // 会话签名钥未初始化/已失效：重新签发后用原始报文（__gmOrig）重放一次，仍失败才报错
      const cfg = res.config
      if (!cfg.__gmRetried) {
        cfg.__gmRetried = true
        const ok = await initSignKeys()
        if (ok && cfg.__gmOrig) {
          const retry = {
            ...cfg,
            url: cfg.__gmOrig.url,
            params: cfg.__gmOrig.params,
            data: cfg.__gmOrig.data
          }
          delete retry.__gmOrig
          return service(retry)
        }
      }
      clearSignKeys()
      ElNotification.error({ title: msg })
      return Promise.reject('error')
    } else if (code !== 200) {
      ElNotification.error({ title: msg })
      return Promise.reject('error')
    } else {
      return  Promise.resolve(res.data)
    }
  },
  error => {
    // 错误时也提取 TraceId，便于排查
    const traceId = error.response?.headers['x-trace-id'] || error.response?.headers['X-Trace-Id']
    if (traceId) {
      localStorage.setItem('traceId', traceId)
    }
    console.log('err' + error)
    let { message } = error
    if (message == "Network Error") {
      message = "后端接口连接异常"
    } else if (message.includes("timeout")) {
      message = "系统接口请求超时"
    } else if (message.includes("Request failed with status code")) {
      message = "系统接口" + message.slice(-3) + "异常"
    }
    ElMessage({ message: message, type: 'error', duration: 5 * 1000 })
    return Promise.reject(error)
  }
)

// 通用下载方法
export function download(url: string, params?: unknown, filename?: string, config?: AxiosRequestConfig): Promise<void> {
  downloadLoadingInstance = ElLoading.service({ text: "正在下载数据，请稍候", background: "rgba(0, 0, 0, 0.7)", })
  return service.post(url, params, {
    transformRequest: [(params) => { return tansParams(params) }],
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    responseType: 'blob',
    ...config
  }).then(async (data: unknown) => {
    const isBlob = blobValidate(data)
    if (isBlob) {
      const blob = new Blob([data as BlobPart])
      saveAs(blob, filename)
    } else {
      const resText = await (data as Blob).text()
      const rspObj = JSON.parse(resText) as { code?: number; msg?: string }
      const errMsg = errorCode[rspObj.code!] || rspObj.msg || errorCode['default']
      ElMessage.error(errMsg)
    }
    downloadLoadingInstance?.close()
  }).catch((r) => {
    console.error(r)
    ElMessage.error('下载文件出现错误，请联系管理员！')
    downloadLoadingInstance?.close()
  })
}

// 响应拦截器已解包 res.data，这里把实例方法重声明为直接返回业务数据
type UnwrappedAxios = {
  <T = unknown>(config: AxiosRequestConfig): Promise<T>
  <T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T>
  get<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T>
  post<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T>
  put<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T>
  delete<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T>
}

export default service as unknown as UnwrappedAxios
