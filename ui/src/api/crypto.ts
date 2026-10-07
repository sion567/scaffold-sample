import request from '@/utils/request.ts'

/** 国密 SM2 公钥（信封加密密钥传输 + 登录密码加密共用） */
export interface GmPublicKeyResult {
  /** 密钥标识 */
  keyId: string
  /** SM2 公钥（04 前缀 130 位 hex） */
  publicKey: string
}

/** 会话签名密钥对（规范 V1.2，仅本页内存持有，登出即销毁） */
export interface GmSignKeyResult {
  /** SM2 私钥（64 位 hex） */
  privateKey: string
  /** SM2 公钥（130 位 hex） */
  publicKey: string
}

/**
 * 获取国密 SM2 公钥（信封加密密钥传输 + 登录密码加密共用）。
 * 后端 CryptoKeyResource：网关 /auth/gm-key → ct-auth /gm-key。
 * 返回 { keyId, publicKey }，publicKey 为 04 前缀 130 位 hex。
 * secure:false — 本请求本身不走信封加密，避免循环取钥。
 */
export function getKey(): Promise<GmPublicKeyResult> {
    return request({
        url: '/auth/gm-key',
        headers: {
            isToken: false,
            secure: false
        },
        method: 'get'
    })
}

/**
 * 签发会话签名密钥对（规范 V1.2，需登录态）。
 * 后端 CryptoKeyResource：网关 /auth/gm-sign-key → ct-auth /gm-sign-key。
 * 返回 { privateKey, publicKey }（64/130 位 hex），仅本页内存持有，登出即销毁。
 * secure:false — 后端已将该路径列入信封白名单（bootstrap 请求不套信封、不验签）。
 */
export function getSignKey(): Promise<GmSignKeyResult> {
    return request({
        url: '/auth/gm-sign-key',
        headers: {
            secure: false
        },
        method: 'get'
    })
}
