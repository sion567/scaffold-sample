import { sm2 } from 'sm-crypto'

// 国密 SM2 加密（登录密码加密传输，与后端 zdhr/Sm2Utils 对齐）
// - cipherMode: 1 = C1C3C2（GB/T 32918 标准，zdhr 与 sm-crypto 默认一致）
// - 公钥格式: 04 前缀非压缩点 hex（后端 /auth/sm2PublicKey 下发）
// - 输出: hex 字符串密文
const CIPHER_MODE = 1 // 1-C1C3C2  0-C1C2C3

/**
 * SM2 公钥加密
 * @param {string} plain 明文
 * @param {string} publicKey 04 前缀公钥 hex（130 位）
 * @returns {string} hex 密文
 */
export function encryptBySm2(plain: string, publicKey: string) {
  if (!plain || !publicKey) {
    return plain
  }
  const cipher = sm2.doEncrypt(plain, publicKey, CIPHER_MODE)
  return '04' + cipher
}
