package com.scaffold.common.core.utils.crypto;

import com.scaffold.common.core.crypto.HmacSm3;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 加密字段等值查询的哈希索引工具。
 * phone_idx / idcard_idx = hex( HMAC-SM3(gm.idx.salt, value) )
 * <p>
 * - HMAC-SM3 是带密钥单向哈希：确定性（同值同 idx）、不可逆
 * - salt 独立于 SM4 root key，不随密钥轮换（否则存量 idx 全失效）
 * - 仅用于精确匹配（=），不用于排序/范围
 */
@Component
public class FieldIndexUtils {

    private final String salt;

    public FieldIndexUtils(@Value("${gm.idx.salt}") String salt) {
        this.salt = salt;
    }

    public String phoneIdx(String phone) {
        return index(phone);
    }

    public String idCardIdx(String idCard) {
        return index(idCard);
    }

    /** 通用字段确定性索引（P0 person 域：key_mapping.ext_key 等任意敏感键值，与专用方法同盐） */
    public String fieldIdx(String value) {
        return index(value);
    }

    private String index(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return HmacSm3.hmac(salt, value);
    }
}