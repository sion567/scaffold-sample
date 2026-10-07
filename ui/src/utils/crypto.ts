import { sm2, sm3, sm4 } from 'sm-crypto'

// ============================================================
// 国密算法薄封装（统一走 sm-crypto 现成实现，勿手写算法）
// 契约与后端 zdhr-crypto 库对齐：
//   SM2 加密：C1C3C2，密文 hex，密钥传输时由 utils/sm2.js 的 encryptBySm2 补 04 前缀
//   SM2 签名：SM3withSM2（含 ZA 预处理，userId 标准值），与后端 Sm2Engine.verify 互验
//   SM4 加密：ECB + PKCS7Padding，输出 hex，与后端 SM4/ECB/PKCS7Padding 互通
// ============================================================

const SM2_USER_ID = '1234567812345678'

/** sm2.doSignature 的扩展签名选项（sm-crypto 支持 publicKey/userId，shims 声明只列了常用项） */
interface Sm2SignOptions {
  hash?: boolean
  der?: boolean
  publicKey?: string
  userId?: string
}

/**
 * 生成 16 进制随机字符串（用于 SM4 密钥 / nonce）
 * @param {number} byteLen 字节数
 * @returns {string} hex（2 倍长度字符）
 */
export function randomHex(byteLen: number) {
  const arr = new Uint8Array(byteLen)
  crypto.getRandomValues(arr)
  return Array.from(arr, b => b.toString(16).padStart(2, '0')).join('')
}

/**
 * SM3 杂凑
 * @param {string} msg 普通字符串
 * @returns {string} 64 字符 hex 摘要
 */
export function sm3Hash(msg: string) {
  return sm3(msg)
}

/**
 * SM4-ECB-PKCS7 加密（信封内层：业务数据加密）
 * @param {string} plainText 明文
 * @param {string} keyHex 32 字符（16 字节）hex 密钥
 * @returns {string} hex 密文
 */
export function sm4EncryptHex(plainText: string, keyHex: string) {
  if (!plainText) return plainText
  return sm4.encrypt(plainText, keyHex, { padding: 'pkcs#7' })
}

/**
 * SM2 签名（SM3withSM2，含 ZA，DER 编码）
 * 契约已实测：与后端 zdhr Sm2Engine.verify（JCA "SM3withSM2"）互验通过——
 * 必须 publicKey 参与 ZA 计算，必须 der:true（后端 JCA 输出/解析 DER 格式）。
 * @param {string} msg 待签名原文
 * @param {string} privateKeyHex 64 字符 hex 私钥
 * @param {string} publicKeyHex 130 字符 hex 公钥（04 前缀）
 * @returns {string} DER hex 签名
 */
export function sm2Sign(msg: string, privateKeyHex: string, publicKeyHex: string) {
  const options: Sm2SignOptions = {
    hash: true,
    publicKey: publicKeyHex,
    userId: SM2_USER_ID,
    der: true
  }
  return sm2.doSignature(msg, privateKeyHex, options)
}
