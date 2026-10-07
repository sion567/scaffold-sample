package com.scaffold.common.core.crypto;

/**
 * SM4 root key 版本化字段加密（等保密钥轮换），由 starter 侧 GmFieldCrypto 实现。
 * <p>
 * 密文格式: {@code $SM4$<version>$GCM$<nonce-hex>$<cipher-b64>$<tag-hex>}
 * - version: 写入时取当前活跃版本；解密时以密文头 version 为准选择 root key 文件
 * - nonce: 每条记录随机 12 字节 hex（一次一密）
 * - cipher: SM4-GCM 密文 base64（不含 tag）
 * - tag: 16 字节 GCM 认证标签 hex（防篡改；密文格式头 "$SM4$<version>$GCM$" 作为 AAD，
 *   防篡改 version/模式重定向密钥选择）
 * <p>
 * 任意历史版本密文可解 ⟺ 对应 {@code sm4-root-<version>} 文件保留。
 */
public interface FieldCrypto {

    /** 加密：返回 "$SM4$<version>$GCM$<nonce-hex>$<cipher-b64>$<tag-hex>" */
    String encrypt(String plain);

    /** 解密：严格模式，失败抛异常 */
    String decrypt(String cipher);

    /**
     * 解密：throwOnFail=false 时，非版本化密文或解密失败均返回原始输入
     * （兼容旧 SmCryptoUtils.doSm4CbcDecrypt(str, false) 语义）
     */
    String decrypt(String cipher, boolean throwOnFail);

    /** 判断是否为版本化密文（"$SM4$" 前缀） */
    boolean isVersioned(String value);

    /** 重载当前活跃版本的 root key（mtime 检测 / 强制） */
    void reload();

    /** 运行期切换活跃版本（新加密用 newVersion），无视 mtime；需 newVersion 的 key 文件已存在 */
    void reload(String newVersion);

    /** 当前活跃版本（新加密用），供管理接口/状态展示 */
    String getActiveVersion();
}
