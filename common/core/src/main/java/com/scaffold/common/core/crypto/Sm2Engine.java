package com.scaffold.common.core.crypto;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Arrays;

import org.bouncycastle.asn1.ASN1BitString;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.gm.GMNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.SM2Engine.Mode;
import org.bouncycastle.crypto.generators.ECKeyPairGenerator;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECKeyGenerationParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.crypto.signers.SM2Signer;
import org.bouncycastle.crypto.signers.StandardDSAEncoding;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPrivateKeySpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.bouncycastle.math.ec.ECPoint;

/**
 * SM2 加解密/签名门面（BouncyCastle 轻量级 API 实现）。
 * <p>
 * 原 zdhr-crypto Sm2Engine 的同语义替代，输出格式逐字节兼容（存量密文/签名照用）：
 * <ul>
 *   <li>密文：raw C1C3C2（默认），hex 大写；解密 auto-try 兼容自定义 ASN1_DER
 *       （{x, y, BIT STRING c2, OCTET STRING c3}，历史格式）与旧序列 C1C2C3；</li>
 *   <li>签名：SM3withSM2（默认 userID {@code 1234567812345678}），DER 编码，hex 大写；
 *       验签兼容 sm-crypto 默认输出的 64 字节裸 r||s；</li>
 *   <li>密钥：裸字节表示（公钥 65B {@code 04||X||Y}，私钥 32B d），见 {@link Sm2Keys}。</li>
 * </ul>
 * 走 BC 轻量级 API，不向 JVM 注册全局 Provider（避免劫持 RSA 等标准算法查找）。
 */
public class Sm2Engine {

    /** 线程安全；国密各门面共用（zdhr CryptoRandom 的等价物） */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final X9ECParameters X9 = GMNamedCurves.getByName("sm2p256v1");
    private static final ECDomainParameters DOMAIN =
            new ECDomainParameters(X9.getCurve(), X9.getG(), X9.getN(), X9.getH());

    private final byte[] publicKeyBytes;
    private final byte[] privateKeyBytes;

    public Sm2Engine(byte[] privateKeyBytes, byte[] publicKeyBytes) {
        if (publicKeyBytes == null) {
            throw new IllegalArgumentException("publicKeyBytes cannot be null");
        }
        if (publicKeyBytes.length != 65 || publicKeyBytes[0] != 0x04) {
            throw new IllegalArgumentException("SM2 公钥须为 65 字节非压缩点 04||X||Y");
        }
        this.publicKeyBytes = publicKeyBytes.clone();
        // 兼容历史调用：允许带符号位/缺前导零的表示，归一化为 32 字节
        this.privateKeyBytes = privateKeyBytes == null
                ? null : Sm2Keys.bigIntTo32(new BigInteger(1, privateKeyBytes));
    }

    /** SM2 公钥加密（raw C1C3C2，hex 大写）。 */
    public String encryptHex(String plain) {
        byte[] in = plain == null ? new byte[0] : plain.getBytes(StandardCharsets.UTF_8);
        try {
            org.bouncycastle.crypto.engines.SM2Engine engine =
                    new org.bouncycastle.crypto.engines.SM2Engine(Mode.C1C3C2);
            engine.init(true, new ParametersWithRandom(publicParams(), SECURE_RANDOM));
            return Hexs.encodeHexStr(process(engine, in));
        } catch (Exception e) {
            throw new IllegalStateException("SM2 加密失败(C1C3C2)", e);
        }
    }

    /**
     * SM2 私钥解密（hex 密文）：auto-try 按 自定义 DER → C1C3C2 → C1C2C3 顺序尝试，
     * 与原 zdhr 实现一致，存量三种形态密文均可解。
     */
    public String decryptStr(String cipherHex) {
        if (privateKeyBytes == null) {
            throw new IllegalStateException("SM2 私钥未初始化，无法解密");
        }
        if (cipherHex == null || cipherHex.isEmpty()) {
            throw new IllegalArgumentException("SM2 解密密文为空");
        }
        Exception last = null;
        for (int attempt = 0; attempt < 3; attempt++) {
            // DER 首字节必须是 0x30，否则直接跳过节省异常开销
            if (attempt == 0) {
                byte[] head;
                try {
                    head = Hexs.decodeHexStr(cipherHex.length() >= 2 ? cipherHex.substring(0, 2) : cipherHex);
                } catch (Exception e) {
                    continue;
                }
                if (head.length == 0 || head[0] != 0x30) {
                    continue;
                }
            }
            try {
                byte[] cipherBytes = Hexs.decodeHexStr(cipherHex);
                byte[] rawC1C3C2 = switch (attempt) {
                    case 0 -> customDerToRaw(cipherBytes);
                    case 2 -> reorderC1C2C3ToC1C3C2(cipherBytes);
                    default -> cipherBytes;
                };
                byte[] plainBytes = decryptRaw(rawC1C3C2);
                return new String(plainBytes, StandardCharsets.UTF_8);
            } catch (Exception e) {
                last = e;
            }
        }
        throw new IllegalStateException("无法识别 SM2 密文模式(DER/C1C3C2/C1C2C3 均失败)", last);
    }

    /** SM3withSM2 签名（默认 userID，DER 编码，hex 大写）。 */
    public String sign(String data) {
        if (privateKeyBytes == null) {
            throw new IllegalStateException("SM2 私钥未初始化，无法签名");
        }
        byte[] in = data == null ? new byte[0] : data.getBytes(StandardCharsets.UTF_8);
        try {
            SM2Signer signer = new SM2Signer();
            signer.init(true, privateParams());
            signer.update(in, 0, in.length);
            return Hexs.encodeHexStr(signer.generateSignature());
        } catch (Exception e) {
            throw new IllegalStateException("SM2 签名失败", e);
        }
    }

    /** SM3withSM2 验签：DER 或 64 字节裸 r||s（sm-crypto 默认输出）均可。 */
    public boolean verify(String data, String sigHex) {
        try {
            byte[] in = data == null ? new byte[0] : data.getBytes(StandardCharsets.UTF_8);
            byte[] sig = normalizeSignature(Hexs.decodeHexStr(sigHex));
            SM2Signer signer = new SM2Signer();
            signer.init(false, publicParams());
            signer.update(in, 0, in.length);
            return signer.verifySignature(sig);
        } catch (Exception e) {
            return false;
        }
    }

    /** 当前活跃公钥 hex（04 前缀，130 字符大写）。 */
    public String getPublicKeyHex() {
        return Hexs.encodeHexStr(publicKeyBytes);
    }

    /** 生成 SM2 密钥对（曲线 sm2p256v1，BC BCEC 键：实现标准 ECPublicKey/ECPrivateKey 接口）。 */
    public static KeyPair generateKeyPair() {
        ECParameterSpec spec = ECNamedCurveTable.getParameterSpec("sm2p256v1");
        ECKeyPairGenerator generator = new ECKeyPairGenerator();
        generator.init(new ECKeyGenerationParameters(DOMAIN, SECURE_RANDOM));
        org.bouncycastle.crypto.AsymmetricCipherKeyPair kp = generator.generateKeyPair();
        BCECPrivateKey privKey = new BCECPrivateKey("EC",
                new ECPrivateKeySpec(((ECPrivateKeyParameters) kp.getPrivate()).getD(), spec),
                org.bouncycastle.jce.provider.BouncyCastleProvider.CONFIGURATION);
        BCECPublicKey pubKey = new BCECPublicKey("EC",
                new ECPublicKeySpec(((ECPublicKeyParameters) kp.getPublic()).getQ(), spec),
                org.bouncycastle.jce.provider.BouncyCastleProvider.CONFIGURATION);
        return new KeyPair(pubKey, privKey);
    }

    /** JCA PublicKey → 裸公钥字节（65B）。 */
    public static byte[] publicBytes(PublicKey publicKey) {
        return Sm2Keys.publicBytes(publicKey);
    }

    /** JCA PrivateKey → 裸私钥字节（32B d）。 */
    public static byte[] privateBytes(PrivateKey privateKey) {
        return Sm2Keys.privateBytes(privateKey);
    }

    // ==================== 内部实现 ====================

    private ECPublicKeyParameters publicParams() {
        return new ECPublicKeyParameters(X9.getCurve().decodePoint(publicKeyBytes), DOMAIN);
    }

    private ECPrivateKeyParameters privateParams() {
        return new ECPrivateKeyParameters(new BigInteger(1, privateKeyBytes), DOMAIN);
    }

    private static byte[] process(org.bouncycastle.crypto.engines.SM2Engine engine, byte[] input)
            throws InvalidCipherTextException {
        return engine.processBlock(input, 0, input.length);
    }

    private byte[] decryptRaw(byte[] rawC1C3C2) throws InvalidCipherTextException {
        org.bouncycastle.crypto.engines.SM2Engine engine =
                new org.bouncycastle.crypto.engines.SM2Engine(Mode.C1C3C2);
        engine.init(false, privateParams());
        return process(engine, rawC1C3C2);
    }

    /** 签名归一化：64 字节裸 r||s → DER；DER 原样返回。 */
    private static byte[] normalizeSignature(byte[] sig) throws IOException {
        if (sig == null || sig.length != 64) {
            return sig;
        }
        return StandardDSAEncoding.INSTANCE.encode(DOMAIN.getN(),
                new BigInteger(1, Arrays.copyOfRange(sig, 0, 32)),
                new BigInteger(1, Arrays.copyOfRange(sig, 32, 64)));
    }

    /**
     * 自定义 DER → raw C1C3C2（历史格式：
     * SEQUENCE { INTEGER x, INTEGER y, BIT STRING c2, OCTET STRING c3 }）。
     */
    private static byte[] customDerToRaw(byte[] der) throws IOException {
        try (ASN1InputStream in = new ASN1InputStream(der)) {
            ASN1Sequence seq = ASN1Sequence.getInstance(in.readObject());
            if (seq.size() != 4) {
                throw new IllegalArgumentException("SM2 DER 解密: SEQUENCE 长度应为 4, 实际 " + seq.size());
            }
            BigInteger x = ASN1Integer.getInstance(seq.getObjectAt(0)).getValue();
            BigInteger y = ASN1Integer.getInstance(seq.getObjectAt(1)).getValue();
            byte[] c2 = ASN1BitString.getInstance(seq.getObjectAt(2)).getBytes();
            byte[] c3 = DEROctetString.getInstance(seq.getObjectAt(3)).getOctets();
            byte[] raw = new byte[65 + c3.length + c2.length];
            raw[0] = 0x04;
            System.arraycopy(Sm2Keys.bigIntTo32(x), 0, raw, 1, 32);
            System.arraycopy(Sm2Keys.bigIntTo32(y), 0, raw, 33, 32);
            System.arraycopy(c3, 0, raw, 65, 32);
            System.arraycopy(c2, 0, raw, 97, c2.length);
            return raw;
        }
    }

    /** 旧序列 C1C2C3 → C1C3C2（仅解密兼容存量密文）。 */
    private static byte[] reorderC1C2C3ToC1C3C2(byte[] c1c2c3) {
        if (c1c2c3.length < 98) {
            throw new IllegalArgumentException("SM2 C1C2C3 密文长度非法: " + c1c2c3.length);
        }
        byte[] raw = new byte[c1c2c3.length];
        System.arraycopy(c1c2c3, 0, raw, 0, 65); // C1
        int c2Len = c1c2c3.length - 97;
        System.arraycopy(c1c2c3, 65, raw, 97, c2Len); // C2 → 尾部
        System.arraycopy(c1c2c3, 65 + c2Len, raw, 65, 32); // C3 → 中部
        return raw;
    }

    /** 仅供测试/gm 模块构造自定义 DER 密文（历史格式兼容验证）。 */
    static byte[] rawToCustomDer(byte[] rawC1C3C2) throws IOException {
        byte[] c2 = Arrays.copyOfRange(rawC1C3C2, 97, rawC1C3C2.length);
        byte[] c3 = Arrays.copyOfRange(rawC1C3C2, 65, 97);
        ASN1EncodableVector v = new ASN1EncodableVector();
        v.add(new ASN1Integer(new BigInteger(1, Arrays.copyOfRange(rawC1C3C2, 1, 33))));
        v.add(new ASN1Integer(new BigInteger(1, Arrays.copyOfRange(rawC1C3C2, 33, 65))));
        v.add(new org.bouncycastle.asn1.DERBitString(c2));
        v.add(new DEROctetString(c3));
        return new DERSequence(v).getEncoded("DER");
    }
}
