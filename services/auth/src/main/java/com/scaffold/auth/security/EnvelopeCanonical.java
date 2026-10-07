package com.scaffold.auth.security;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

/**
 * 签名串拼装（与 zdhr 示例 SignatureStringBuilder 保持一致）：
 * 按参数名 ASCII 升序排列，拼成 key1=value1&key2=value2，空值不参与。
 * <p>
 * 验签摘要由 Sm2Engine 内部的 SM3withSM2 完成（含 ZA 预处理），
 * 因此直接对拼装结果做 Sm2Engine.verify 即可，无需手动 SM3。
 */
public final class EnvelopeCanonical {

    public static final String TIMESTAMP = "timestamp";
    public static final String NONCE = "nonce";
    public static final String SIGNATURE = "signature";

    private EnvelopeCanonical() {
    }

    /**
     * @param params 业务参数 + timestamp + nonce；signature 自动剔除
     * @return 待签名字符串
     */
    public static String build(Map<String, String> params) {
        TreeMap<String, String> sorted = new TreeMap<>();
        params.forEach((k, v) -> {
            if (SIGNATURE.equals(k) || v == null || v.isEmpty()) {
                return;
            }
            sorted.put(k, v);
        });
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : sorted.entrySet()) {
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(e.getKey()).append('=').append(e.getValue());
        }
        return sb.toString();
    }
}
