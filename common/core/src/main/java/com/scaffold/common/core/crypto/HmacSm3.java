package com.scaffold.common.core.crypto;

import java.nio.charset.StandardCharsets;

import org.bouncycastle.crypto.digests.SM3Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;

/**
 * HMAC-SM3 消息认证码（BouncyCastle 实现），返回大写 hex。
 * <p>
 * 原 zdhr-crypto HmacSm3 的同语义替代：key/data 均按 UTF-8 取字节，
 * 网关身份头签名（GatewaySm3Signature）与加密字段索引（FieldIndexUtils）共用。
 */
public final class HmacSm3 {

    private HmacSm3() {}

    public static String hmac(String secretKey, String data) {
        byte[] secretBytes = secretKey == null ? new byte[0] : secretKey.getBytes(StandardCharsets.UTF_8);
        byte[] dataBytes = data == null ? new byte[0] : data.getBytes(StandardCharsets.UTF_8);
        HMac mac = new HMac(new SM3Digest());
        mac.init(new KeyParameter(secretBytes));
        mac.update(dataBytes, 0, dataBytes.length);
        byte[] out = new byte[mac.getMacSize()];
        mac.doFinal(out, 0);
        return Hexs.encodeHexStr(out);
    }
}
