package com.scaffold.common.core.crypto;

/**
 * 静态 FieldCrypto 访问点（供 MyBatis TypeHandler 等非 Spring 实例化环境使用）。
 * <p>
 * Spring 侧由 starter-gm 的 GmFieldCrypto 在启动时注入；未注入时 get() 抛 IllegalStateException。
 */
public final class FieldCryptoHolder {
    private static volatile FieldCrypto instance;

    private FieldCryptoHolder() {}

    public static void set(FieldCrypto fieldCrypto) {
        instance = fieldCrypto;
    }

    public static FieldCrypto get() {
        FieldCrypto f = instance;
        if (f == null) {
            throw new IllegalStateException(
                    "FieldCrypto not initialized. Add gm.sm4.* config and enable the gm starter (scaffold-spring-boot-starter-gm).");
        }
        return f;
    }
}
