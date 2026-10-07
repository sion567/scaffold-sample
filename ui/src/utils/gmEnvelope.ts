// ============================================================
// 国密信封加密请求封装（规范 V1.2：随机 SM4 加密业务数据 + SM2 加密 SM4 密钥 + 会话 SM2 签名）
//
// 流程（每次请求）：
//   1. 随机生成临时 SM4 密钥（16 字节 hex）
//   2. 用临时 SM4 密钥加密业务数据 → encData
//   3. 用后端 SM2 公钥（/auth/gm-key 下发）加密临时 SM4 密钥 → encKey
//   4. 对 {encKey, encData, timestamp, nonce} ASCII 升序拼串做 SM2 签名 → signature
//   5. 后端 Dubbo provider 过滤器验签、防重放后解开信封，业务方法拿到明文
//
// 范围：仅 POST/PUT/PATCH 且命中 ENVELOPE_PATHS 的请求走信封（后端解密按
// GmEnvelopeBody 参数识别，只覆盖 auth 凭证类接口）；GET/DELETE 与其余接口
// 保持明文，传输安全由 HTTPS 承担。
// 签名密钥（V1.2）：登录后由后端按会话签发（GET /auth/gm-sign-key，私钥经 TLS 下发一次），
// 仅存本页内存，刷新页面由路由守卫重新拉取，登出即销毁——无构建期注入，签名恒在。
// 原始报文备份在 config.__gmOrig：签名钥过期（40006/40003）时用原始报文重放一次。
//
// 由 utils/request.ts 统一调用，业务代码禁止单独调用加密函数（规范 5.1）。
// ============================================================

import type { InternalAxiosRequestConfig } from 'axios'
import { getKey, getSignKey } from '@/api/crypto.ts'
import { getToken } from '@/utils/auth.ts'
import { randomHex, sm4EncryptHex, sm2Sign } from '@/utils/crypto.ts'
import { encryptBySm2 } from '@/utils/sm2.ts'

interface GmKey { publicKey: string; keyId: string; fetchedAt: number }
let _gmKey: GmKey | null = null // { publicKey, keyId, fetchedAt }
const KEY_TTL_MS = 4 * 60 * 1000 // 后端 Cache-Control max-age=300

interface SignKeys { privateKey: string; publicKey: string }
let _signKeys: SignKeys | null = null // { privateKey, publicKey }，仅内存持有

/** /auth/gm-key、/auth/gm-sign-key 的响应形状（兼容 R 包装与裸返回两种报文） */
interface GmKeyResp {
  data?: { publicKey?: string; privateKey?: string; keyId?: string } | null
  publicKey?: string
  privateKey?: string
  keyId?: string
}

async function ensureGmKey() {
  if (_gmKey && Date.now() - _gmKey.fetchedAt < KEY_TTL_MS) {
    return _gmKey
  }
  const res = await getKey() as GmKeyResp
  // 兼容两种响应：R 包装 {data:{publicKey}} 或后端直接返回 {keyId, publicKey}
  const data = (res?.data?.publicKey ? res.data : (res || {})) as { publicKey?: string; keyId?: string }
  if (!data.publicKey) {
    throw new Error('获取国密公钥失败')
  }
  _gmKey = { publicKey: data.publicKey, keyId: data.keyId || '', fetchedAt: Date.now() }
  return _gmKey
}

/**
 * 向后端签发/重签会话签名密钥对（登录后与收到 40006/40003 时调用）
 * @returns {Promise<boolean>} 是否成功
 */
export async function initSignKeys() {
  if (!getToken()) {
    _signKeys = null
    return false
  }
  try {
    const res = await getSignKey() as GmKeyResp
    const data = (res?.data?.privateKey ? res.data : (res || {})) as { privateKey?: string; publicKey?: string }
    if (data.privateKey && data.publicKey) {
      _signKeys = { privateKey: data.privateKey, publicKey: data.publicKey }
      return true
    }
  } catch (e) {
    console.warn('[国密] 会话签名密钥签发失败', e)
  }
  return false
}

/** 登出时清除内存中的会话签名密钥 */
export function clearSignKeys() {
  _signKeys = null
}

/** 会话签名密钥是否已就绪 */
export function signKeysReady() {
  return !!_signKeys
}

/** 已登录但钥未就绪时自动签发一次（路由守卫/请求前调用，已就绪零开销） */
export async function ensureSignKeys() {
  if (!_signKeys && getToken()) {
    await initSignKeys()
  }
}

/** 信封加密生效路径（与后端继承 GmEnvelopeBody 的接口一一对应） */
const ENVELOPE_PATHS = ['/auth/login', '/auth/register', '/auth/unlockscreen']

/** 该请求是否适用信封加密：仅 POST/PUT/PATCH 且命中 ENVELOPE_PATHS；GET/DELETE 保持明文 */
export function isEnvelopeCandidate(config: InternalAxiosRequestConfig) {
  const method = (config.method || 'get').toLowerCase()
  if (!['post', 'put', 'patch'].includes(method)) return false
  const headers = config.headers || {}
  if (headers.secure === false) return false
  if (config.responseType === 'blob' || config.responseType === 'arraybuffer') return false
  const ct = headers['Content-Type'] as string | undefined || ''
  if (ct.includes('application/x-www-form-urlencoded')) return false // 下载等表单提交
  if (typeof FormData !== 'undefined' && config.data instanceof FormData) return false // 文件上传
  const url = (config.url || '').split('?')[0]
  return ENVELOPE_PATHS.some(path => url === path || url.endsWith(path))
}

/**
 * 就地对请求做信封加密（修改 config.data / config.headers）
 * 改写前把原始 url/params/data 备份到 config.__gmOrig，供 40006/40003 重放
 * @param {object} config axios 请求配置
 */
export async function applyGmEnvelope(config: InternalAxiosRequestConfig) {
  const gmKey = await ensureGmKey()
  await ensureSignKeys()

  const sm4Key = randomHex(16)
  const payload = config.data ?? {}
  const encData = sm4EncryptHex(JSON.stringify(payload ?? {}), sm4Key)
  const encKey = encryptBySm2(sm4Key, gmKey.publicKey)

  const timestamp = Date.now().toString()
  const nonce = randomHex(8)
  const fields = { encKey, encData, timestamp, nonce }

  const signature = (_signKeys && getToken())
    ? sm2Sign(canonical(fields), _signKeys.privateKey, _signKeys.publicKey)
    : undefined

  // 备份原始报文（深拷贝，信封改写不破坏重放源）
  try {
    config.__gmOrig = {
      url: config.url,
      params: config.params ? JSON.parse(JSON.stringify(config.params)) : undefined,
      data: (config.data && typeof config.data === 'object' && !(config.data instanceof FormData))
        ? JSON.parse(JSON.stringify(config.data)) : config.data
    }
  } catch (e) {
    config.__gmOrig = undefined // 不可序列化的报文不支持重放
  }

  config.data = { encKey, encData, timestamp, nonce, signature }
  config.headers['X-Gm-Envelope'] = '1'
}

/** 签名串：参数名 ASCII 升序，空值不参与（与后端 EnvelopeCanonical 一致） */
function canonical(fields: Record<string, string>) {
  return Object.keys(fields)
    .filter(k => fields[k] !== '' && fields[k] !== null && fields[k] !== undefined)
    .sort()
    .map(k => `${k}=${fields[k]}`)
    .join('&')
}
