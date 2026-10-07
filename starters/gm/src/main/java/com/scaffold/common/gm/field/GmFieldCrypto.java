package com.scaffold.common.gm.field;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.modes.GCMBlockCipher;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.KeyParameter;

import com.scaffold.common.core.crypto.FieldCrypto;
import com.scaffold.common.core.crypto.FieldCryptoHolder;
import com.scaffold.common.core.crypto.Hexs;

import jakarta.annotation.PostConstruct;

/**
 * SM4 版本化字段加密（等保密钥轮换，BouncyCastle SM4-GCM 实现）。
 * <p>
 * 原 zdhr-crypto GmFieldCrypto 的同语义替代，密文格式逐字节兼容（存量密文照解）：
 * <ul>
 *   <li>DEK 模式（key-role=DEK，现状字节格式不变）：
 *       {@code $SM4$<version>$GCM$<nonce-hex>$<cipher-b64>$<tag-hex>}</li>
 *   <li>ENV 信封模式（key-role=KEK，随机 DEK 加密数据、root key 作 KEK 包 DEK）：
 *       {@code $SM4$<version>$ENV$SM4$<wrappedDEK-b64>$<nonce-hex>$<cipher-b64>$<tag-hex>}；
 *       当前只实现 SM4 材质，遇到其他材质解密明确抛异常</li>
 * </ul>
 * 密文头 {@code $SM4$<version>$<MODE>$...} 作为数据 GCM AAD 参与认证，
 * 防篡改 version/模式重定向密钥选择。启动时把自身注入 {@link FieldCryptoHolder}。
 */
public class GmFieldCrypto implements FieldCrypto {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String PREFIX = "$SM4$";
    private static final String MODE_GCM = "GCM";
    private static final String MODE_ENV = "ENV";
    private static final String KEK_SM4 = "SM4";   // KEK 材质：SM4（软对称）/ SM2 / KMS（预留）
    private static final int KEY_ROLE_DEK = 0;
    private static final int KEY_ROLE_KEK = 1;
    private static final int DEK_LEN = 16;
    private static final int NONCE_LEN = 12;
    private static final int TAG_LEN = 16;

    private final FileSymmetricKeyProvider keyProvider;
    private final int keyRole;

    public GmFieldCrypto(FileSymmetricKeyProvider keyProvider, String keyRole) {
        this.keyProvider = keyProvider;
        if ("KEK".equalsIgnoreCase(keyRole)) {
            this.keyRole = KEY_ROLE_KEK;
        } else if ("DEK".equalsIgnoreCase(keyRole)) {
            this.keyRole = KEY_ROLE_DEK;
        } else {
            throw new IllegalArgumentException("gm.sm4.key-role must be DEK or KEK, got: " + keyRole);
        }
    }

    @PostConstruct
    void init() {
        FieldCryptoHolder.set(this);
    }

    @Override
    public String encrypt(String plain) {
        if (plain == null || plain.isEmpty()) {
            return plain;
        }
        String v = keyProvider.getActiveVersion();   // 单次快照，避免 TOCTOU
        return keyRole == KEY_ROLE_KEK ? encryptEnv(plain, v) : encryptDek(plain, v);
    }

    /** DEK 模式：root key 直接加密数据（现状字节格式不变） */
    private String encryptDek(String plain, String v) {
        byte[] key = keyProvider.getKey(v);          // 复用快照版本取 key
        String header = PREFIX + v + "$" + MODE_GCM + "$";  // header 作 GCM AAD，防篡改 version/模式
        byte[] nonce = random(NONCE_LEN);
        byte[] cipherWithTag = gcm(key, nonce, header.getBytes(StandardCharsets.UTF_8),
                plain.getBytes(StandardCharsets.UTF_8), true);
        return header + Hexs.encodeHexStr(nonce) + "$"
                + Base64.getEncoder().encodeToString(cipher(cipherWithTag)) + "$"
                + Hexs.encodeHexStr(tag(cipherWithTag));
    }

    /** ENV 模式：随机 DEK 加密数据，KEK（root key，SM4 材质）包 DEK；dataAAD 含 wrappedDEK 防跨密文换 DEK */
    private String encryptEnv(String plain, String v) {
        byte[] kek = keyProvider.getKey(v);
        byte[] dek = random(DEK_LEN);
        try {
            // 1) 包 DEK：wrappedDEK = wrapNonce(12) || encDEK(16) || wrapTag(16) → b64
            byte[] wrapNonce = random(NONCE_LEN);
            byte[] wrapped = gcm(kek, wrapNonce, null, dek, true);
            byte[] wrappedDEK = new byte[NONCE_LEN + wrapped.length];
            System.arraycopy(wrapNonce, 0, wrappedDEK, 0, NONCE_LEN);
            System.arraycopy(wrapped, 0, wrappedDEK, NONCE_LEN, wrapped.length);
            String wrappedB64 = Base64.getEncoder().encodeToString(wrappedDEK);

            // 2) 数据加密：dataAAD 含 KEK 材质 + wrappedDEK（防跨密文换 DEK / 防材质降级）
            String header = PREFIX + v + "$" + MODE_ENV + "$" + KEK_SM4 + "$" + wrappedB64 + "$";
            byte[] dataNonce = random(NONCE_LEN);
            byte[] cipherWithTag = gcm(dek, dataNonce, header.getBytes(StandardCharsets.UTF_8),
                    plain.getBytes(StandardCharsets.UTF_8), true);
            return header + Hexs.encodeHexStr(dataNonce) + "$"
                    + Base64.getEncoder().encodeToString(cipher(cipherWithTag)) + "$"
                    + Hexs.encodeHexStr(tag(cipherWithTag));
        } finally {
            Arrays.fill(dek, (byte) 0);
        }
    }

    @Override
    public String decrypt(String cipher) {
        return decrypt(cipher, true);
    }

    @Override
    public String decrypt(String cipher, boolean throwOnFail) {
        if (cipher == null || cipher.isEmpty()) {
            return cipher;
        }
        if (!isVersioned(cipher)) {
            if (throwOnFail) {
                throw new IllegalStateException("Not a versioned SM4 ciphertext");
            }
            return cipher;
        }
        try {
            String[] p = cipher.split("\\$");
            if (p.length < 4 || !"SM4".equals(p[1]) || p[2].isBlank()) {
                throw new IllegalStateException("Malformed SM4 ciphertext header");
            }
            String version = p[2];
            if (MODE_ENV.equals(p[3])) {
                return decryptEnv(p, version);
            }
            if (MODE_GCM.equals(p[3])) {
                return decryptDek(p, version);
            }
            throw new IllegalStateException("Malformed SM4 ciphertext header");
        } catch (Exception e) {
            if (throwOnFail) {
                throw e instanceof RuntimeException re ? re : new IllegalStateException("SM4 decrypt failed", e);
            }
            return cipher;
        }
    }

    private String decryptDek(String[] p, String version) {
        if (p.length < 7 || p[5].isEmpty() || p[6].isEmpty()) {
            throw new IllegalStateException("Malformed SM4 ciphertext header");
        }
        String header = PREFIX + version + "$" + MODE_GCM + "$";  // 与加密时一致的 AAD
        byte[] nonce = Hexs.decodeHexStr(p[4]);
        byte[] cipherBytes = Base64.getDecoder().decode(p[5]);
        byte[] tagBytes = Hexs.decodeHexStr(p[6]);
        byte[] key = keyProvider.getKey(version);
        byte[] plain = gcm(key, nonce, header.getBytes(StandardCharsets.UTF_8),
                concat(cipherBytes, tagBytes), false);
        return new String(plain, StandardCharsets.UTF_8);
    }

    private String decryptEnv(String[] p, String version) {
        if (p.length < 9 || p[4].isEmpty() || p[5].isEmpty() || p[6].isEmpty() || p[7].isEmpty() || p[8].isEmpty()) {
            throw new IllegalStateException("Malformed SM4 ciphertext header");
        }
        if (!KEK_SM4.equals(p[4])) {
            // KEK 材质 SM2 / KMS 预留：格式已定义，实现随对应 KeyProvider 落地
            throw new IllegalStateException("Unsupported KEK material: " + p[4]);
        }
        byte[] wrappedDEK = Base64.getDecoder().decode(p[5]);
        if (wrappedDEK.length != NONCE_LEN + DEK_LEN + TAG_LEN) {
            throw new IllegalStateException("Malformed wrappedDEK length");
        }
        byte[] wrapNonce = Arrays.copyOfRange(wrappedDEK, 0, NONCE_LEN);
        byte[] encDek = Arrays.copyOfRange(wrappedDEK, NONCE_LEN, NONCE_LEN + DEK_LEN);
        byte[] wrapTag = Arrays.copyOfRange(wrappedDEK, NONCE_LEN + DEK_LEN, wrappedDEK.length);
        byte[] kek = keyProvider.getKey(version);
        byte[] dek = gcm(kek, wrapNonce, null, concat(encDek, wrapTag), false);   // KEK 校验失败抛异常
        try {
            String header = PREFIX + version + "$" + MODE_ENV + "$" + KEK_SM4 + "$" + p[5] + "$";  // 与加密时一致的 dataAAD
            byte[] dataNonce = Hexs.decodeHexStr(p[6]);
            byte[] cipherBytes = Base64.getDecoder().decode(p[7]);
            byte[] tagBytes = Hexs.decodeHexStr(p[8]);
            byte[] plain = gcm(dek, dataNonce, header.getBytes(StandardCharsets.UTF_8),
                    concat(cipherBytes, tagBytes), false);
            return new String(plain, StandardCharsets.UTF_8);
        } finally {
            Arrays.fill(dek, (byte) 0);
        }
    }

    @Override
    public boolean isVersioned(String value) {
        return value != null && value.startsWith(PREFIX);
    }

    @Override
    public String getActiveVersion() {
        return keyProvider.getActiveVersion();
    }

    @Override
    public void reload() {
        keyProvider.reload();
    }

    @Override
    public void reload(String newVersion) {
        keyProvider.reload(newVersion);
    }

    private static byte[] random(int len) {
        byte[] out = new byte[len];
        SECURE_RANDOM.nextBytes(out);
        return out;
    }

    private static byte[] cipher(byte[] cipherWithTag) {
        return Arrays.copyOf(cipherWithTag, cipherWithTag.length - TAG_LEN);
    }

    private static byte[] tag(byte[] cipherWithTag) {
        return Arrays.copyOfRange(cipherWithTag, cipherWithTag.length - TAG_LEN, cipherWithTag.length);
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] out = new byte[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    /** SM4-GCM 单块运算：encrypt 输出 密文||tag，decrypt 输入 密文||tag */
    private static byte[] gcm(byte[] key, byte[] nonce, byte[] aad, byte[] in, boolean encrypt) {
        GCMBlockCipher gcm = new GCMBlockCipher(new org.bouncycastle.crypto.engines.SM4Engine());
        gcm.init(encrypt, new AEADParameters(new KeyParameter(key), TAG_LEN * 8, nonce, aad));
        byte[] out = new byte[gcm.getOutputSize(in.length)];
        int len = gcm.processBytes(in, 0, in.length, out, 0);
        try {
            len += gcm.doFinal(out, len);
        } catch (InvalidCipherTextException e) {
            throw new IllegalStateException(encrypt ? "SM4-GCM encrypt failed" : "GCM authentication failed - data tampered or wrong key", e);
        }
        return Arrays.copyOf(out, len);
    }
}
