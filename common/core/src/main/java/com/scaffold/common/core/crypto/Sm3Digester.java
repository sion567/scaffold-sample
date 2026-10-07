package com.scaffold.common.core.crypto;

import java.nio.charset.StandardCharsets;

import org.bouncycastle.crypto.digests.SM3Digest;

/**
 * SM3 哈希，返回 64 字符大写 hex（BouncyCastle 实现）。
 * <p>
 * 原 zdhr-crypto Sm3Digester 的同语义替代：标准 SM3（32 字节摘要），
 * 审计留痕摘要链（curr = SM3(prev|canonical)）对存量数据逐字节兼容。
 */
public final class Sm3Digester {

    private Sm3Digester() {}

    public static String digest(String input) {
        byte[] data = input == null ? new byte[0] : input.getBytes(StandardCharsets.UTF_8);
        return digest(data);
    }

    public static String digest(byte[] input) {
        SM3Digest digest = new SM3Digest();
        digest.update(input, 0, input.length);
        byte[] out = new byte[digest.getDigestSize()];
        digest.doFinal(out, 0);
        return Hexs.encodeHexStr(out);
    }
}
