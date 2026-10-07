package com.scaffold.common.core.crypto;

/**
 * Hex 编解码（大写输出、大小写不敏感解码）。
 * <p>
 * 国密链路的统一 hex 约定：SM3 摘要 / HMAC-SM3 / SM2 签名与密钥均输出大写 hex，
 * 解码接受大小写混排。原 zdhr-crypto Hexs 的同语义替代，格式逐字节兼容。
 */
public final class Hexs {

    private static final char[] HEX_CHARS = "0123456789ABCDEF".toCharArray();

    private Hexs() {}

    public static String encodeHexStr(byte[] data) {
        if (data == null) {
            return null;
        }
        char[] out = new char[data.length * 2];
        for (int i = 0; i < data.length; i++) {
            int v = data[i] & 0xFF;
            out[i * 2] = HEX_CHARS[v >>> 4];
            out[i * 2 + 1] = HEX_CHARS[v & 0x0F];
        }
        return new String(out);
    }

    public static byte[] decodeHexStr(String hex) {
        if (hex == null) {
            return null;
        }
        int len = hex.length();
        if ((len & 1) != 0) {
            throw new IllegalArgumentException("Hex string length must be even: " + len);
        }
        byte[] out = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            int hi = Character.digit(hex.charAt(i), 16);
            int lo = Character.digit(hex.charAt(i + 1), 16);
            if (hi < 0 || lo < 0) {
                throw new IllegalArgumentException("Invalid hex char at " + i);
            }
            out[i / 2] = (byte) ((hi << 4) | lo);
        }
        return out;
    }
}
