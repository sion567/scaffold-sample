package com.scaffold.common.core.utils.crypto;

import com.scaffold.common.core.crypto.FieldCryptoHolder;

/**
 * SM4 字段加密静态门面。
 * <p>
 * 内部委托 {@code FieldCryptoHolder}（由 starter-gm 的 GmFieldCrypto
 * 在启动时注入），让存量 50+ 调用点从 SmCryptoUtils 平滑迁移。
 * <p>
 * 密文格式: {@code $SM4$<version>$GCM$<nonce-hex>$<cipher-b64>$<tag-hex>}，
 * 历史版本可解 ⟺ 对应 {@code sm4-root-<version>} 文件保留。
 */
public final class Sm4FieldCrypto {

    private Sm4FieldCrypto() {}

    public static String encrypt(String plain) {
        return FieldCryptoHolder.get().encrypt(plain);
    }

    public static String decrypt(String cipher) {
        return FieldCryptoHolder.get().decrypt(cipher);
    }

    public static String decrypt(String cipher, boolean throwOnFail) {
        return FieldCryptoHolder.get().decrypt(cipher, throwOnFail);
    }

    /** 是否版本化密文（"$SM4$" 前缀） */
    public static boolean isVersioned(String value) {
        return FieldCryptoHolder.get().isVersioned(value);
    }

    /** 是否"像明文"（含非 ASCII 字符），保留旧 SmCryptoUtils.isLikelyPlainText 语义，供登录链路判断 */
    public static boolean isLikelyPlainText(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) > 127) {
                return true;
            }
        }
        return false;
    }
}
