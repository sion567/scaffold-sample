package com.scaffold.common.core.crypto;

import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECPoint;
import java.util.Arrays;

import org.bouncycastle.asn1.ASN1BitString;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DEROctetString;

/**
 * SM2 JCA 密钥 ↔ 裸字节互转工具。
 * <p>
 * 本工程国密门面以裸字节为边界（公钥 65B {@code 04||X||Y}，私钥 32B d）；
 * BC keystore / BC 生成的密钥对均实现标准 {@link ECPublicKey}/{@link ECPrivateKey}，
 * 优先走 {@code getW()}/{@code getS()}，非标准实现回退到 X.509 SPKI / PKCS#8 DER 解析。
 * 原 zdhr-crypto Sm2Keys 的同语义替代。
 */
public final class Sm2Keys {

    private Sm2Keys() {}

    /** 提取非压缩公钥点 {@code 04||X||Y}（65 字节）。 */
    public static byte[] publicBytes(PublicKey publicKey) {
        if (publicKey == null) {
            throw new IllegalStateException("公钥为 null");
        }
        if (publicKey instanceof ECPublicKey ecKey) {
            return publicBytes(ecKey.getW());
        }
        // 回退：解析 X.509 SPKI，subjectPublicKey BIT STRING 直接承载裸点字节
        try (ASN1InputStream in = new ASN1InputStream(publicKey.getEncoded())) {
            ASN1Sequence spki = ASN1Sequence.getInstance(in.readObject());
            byte[] point = ASN1BitString.getInstance(spki.getObjectAt(1)).getBytes();
            if (point.length == 65 && point[0] == 0x04) {
                return point;
            }
            throw new IllegalStateException("无法识别的 SM2 公钥编码: len=" + point.length);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("SM2 公钥提取失败", e);
        }
    }

    /** 提取私钥标量 d（32 字节大端）。 */
    public static byte[] privateBytes(PrivateKey privateKey) {
        if (privateKey == null) {
            throw new IllegalStateException("私钥为 null");
        }
        if (privateKey instanceof ECPrivateKey ecKey) {
            return bigIntTo32(ecKey.getS());
        }
        // 回退：PKCS#8 → SEC1 ECPrivateKey SEQUENCE { version, privateKey OCTET, [params] }
        try (ASN1InputStream in = new ASN1InputStream(privateKey.getEncoded())) {
            ASN1Sequence pki = ASN1Sequence.getInstance(in.readObject());
            byte[] sec1 = DEROctetString.getInstance(pki.getObjectAt(2)).getOctets();
            try (ASN1InputStream sec1In = new ASN1InputStream(sec1)) {
                ASN1Sequence sec1Seq = ASN1Sequence.getInstance(sec1In.readObject());
                byte[] d = DEROctetString.getInstance(sec1Seq.getObjectAt(1)).getOctets();
                if (d.length != 32) {
                    throw new IllegalStateException("SM2 私钥长度非法: " + d.length);
                }
                return d;
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("SM2 私钥提取失败", e);
        }
    }

    /** 从标准 EC 点提取 65B 非压缩编码。 */
    public static byte[] publicBytes(ECPoint point) {
        if (point == null || ECPoint.POINT_INFINITY.equals(point)) {
            throw new IllegalStateException("SM2 公钥点为无穷远点");
        }
        byte[] out = new byte[65];
        out[0] = 0x04;
        System.arraycopy(bigIntTo32(point.getAffineX()), 0, out, 1, 32);
        System.arraycopy(bigIntTo32(point.getAffineY()), 0, out, 33, 32);
        return out;
    }

    /** BigInteger → 32 字节大端（不足左补零，多余符号字节截断）。 */
    public static byte[] bigIntTo32(BigInteger v) {
        byte[] src = v.toByteArray();
        byte[] out = new byte[32];
        if (src.length == 33) {
            System.arraycopy(src, 1, out, 0, 32);
        } else {
            System.arraycopy(src, Math.max(0, src.length - 32), out,
                    Math.max(0, 32 - src.length), Math.min(32, src.length));
        }
        return out;
    }

    /** 65B {@code 04||X||Y} → 标准 {@link ECPoint}。 */
    public static ECPoint toEcPoint(byte[] publicKey65) {
        if (publicKey65 == null || publicKey65.length != 65 || publicKey65[0] != 0x04) {
            throw new IllegalStateException("SM2 公钥须为 65 字节非压缩点 04||X||Y");
        }
        return new ECPoint(new BigInteger(1, Arrays.copyOfRange(publicKey65, 1, 33)),
                new BigInteger(1, Arrays.copyOfRange(publicKey65, 33, 65)));
    }
}
