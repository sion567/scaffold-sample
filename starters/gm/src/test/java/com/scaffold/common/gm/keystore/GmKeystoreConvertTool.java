package com.scaffold.common.gm.keystore;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.Provider;
import java.security.Security;
import java.security.interfaces.ECPrivateKey;
import java.util.Enumeration;

import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPrivateKeySpec;

/**
 * GM keystore 一次性转换工具（手动运行，非单元测试）。
 * <p>
 * 背景：keystore 私钥袋封装存在跨 provider 兼容问题——Kona 写出的 PKCS12
 * （PBES2/PBKDF2/AES-256-CBC 变体）BC 与 openssl 均无法解开（pad block corrupted）。
 * 本工具读出 SM2 私钥标量 d 与证书链，以 BC 重写为目标格式——密钥材料与别名不变，
 * 仅更换封装，原信封密文不受影响。目标格式默认 <b>BCFKS</b>（GmIdentity 当前默认，
 * BC 专有格式，彻底规避交换格式兼容问题），也可显式指定 PKCS12（BC 标准 PBES2 写出）。
 * <p>
 * 用途：① 仓库 dev keystore（keystore/gm.p12 → gm/gm.bcfks）；② 生产 Kona 写出的
 * gm.p12 在切换到 starter-gm 前做同样转换。
 * <p>
 * 运行（BC 直读的源库只需 bcprov jar；Kona 写出的源库 classpath 另需
 * kona-crypto/kona-pkix，读取失败时自动回落 Kona）：
 * <pre>
 * java -cp 'bcprov-jdk18on.jar[:kona-crypto.jar:kona-pkix.jar]:scaffold-spring-boot-starter-gm-tests.jar' \
 *      com.scaffold.common.gm.keystore.GmKeystoreConvertTool &lt;in&gt; &lt;out&gt; &lt;storePass&gt; &lt;keyPass&gt; &lt;alias&gt; [outFormat=BCFKS]
 * </pre>
 */
public final class GmKeystoreConvertTool {

    private GmKeystoreConvertTool() {}

    public static void main(String[] args) throws Exception {
        if (args.length < 5 || args.length > 6) {
            System.err.println("usage: <in> <out> <storePass> <keyPass> <alias> [outFormat=BCFKS]");
            System.exit(2);
        }
        Path in = Path.of(args[0]);
        Path out = Path.of(args[1]);
        String storePass = args[2];
        String keyPass = args[3];
        String alias = args[4];
        String outFormat = args.length > 5 ? args[5] : "BCFKS";

        BouncyCastleProvider bc = new BouncyCastleProvider();
        if (Security.getProvider("BC") == null) {
            Security.addProvider(bc);
        }

        // 1) 读旧库：先 BC（BC 直写的库/已转换过的库），失败回落 Kona（Kona 写出的 PBES2 变体）
        KeyStore old = loadSource(in, storePass, bc);
        System.out.println("== source aliases ==");
        for (Enumeration<String> e = old.aliases(); e.hasMoreElements(); ) {
            System.out.println("  " + e.nextElement());
        }
        ECPrivateKey priv = (ECPrivateKey) old.getKey("sm2-privateKey-" + alias, keyPass.toCharArray());
        java.security.cert.Certificate cert = old.getCertificate("sm2-publicKey-" + alias);
        if (priv == null || cert == null) {
            throw new IllegalStateException("alias " + alias + " 私钥或证书缺失");
        }
        System.out.println("d.bitLength=" + priv.getS().bitLength()
                + " cert.subject=" + ((java.security.cert.X509Certificate) cert).getSubjectX500Principal());

        // 2) BC 重写：BCECPrivateKey + BC 证书
        ECParameterSpec spec = ECNamedCurveTable.getParameterSpec("sm2p256v1");
        java.security.PrivateKey bcPriv = new org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey(
                "EC", new ECPrivateKeySpec(priv.getS(), spec), BouncyCastleProvider.CONFIGURATION);
        java.security.cert.CertificateFactory cf = java.security.cert.CertificateFactory.getInstance("X.509", "BC");
        java.security.cert.Certificate bcCert = cf.generateCertificate(
                new ByteArrayInputStream(cert.getEncoded()));

        KeyStore fresh = KeyStore.getInstance(outFormat, bc);
        fresh.load(null, null);
        fresh.setKeyEntry("sm2-privateKey-" + alias, bcPriv, keyPass.toCharArray(),
                new java.security.cert.Certificate[]{bcCert});
        fresh.setCertificateEntry("sm2-publicKey-" + alias, bcCert);
        try (OutputStream os = Files.newOutputStream(out)) {
            fresh.store(os, storePass.toCharArray());
        }

        // 3) BC 回读验证
        KeyStore verify = KeyStore.getInstance(outFormat, bc);
        try (InputStream is = Files.newInputStream(out)) {
            verify.load(is, storePass.toCharArray());
        }
        java.security.Key roundTrip = verify.getKey("sm2-privateKey-" + alias, keyPass.toCharArray());
        if (roundTrip == null) {
            throw new IllegalStateException("BC 回读失败");
        }
        System.out.println("OK: " + out + " (" + outFormat + "，BC 可读，密钥材料不变)");
    }

    /** BC 直读源库；失败时注册 Kona provider 后重试（Kona 写出的 PKCS12 仅 Kona 能解）。 */
    private static KeyStore loadSource(Path in, String storePass, BouncyCastleProvider bc) throws Exception {
        try (InputStream is = Files.newInputStream(in)) {
            KeyStore ks = KeyStore.getInstance("PKCS12", bc);
            ks.load(is, storePass.toCharArray());
            System.out.println("source read via BC/PKCS12");
            return ks;
        } catch (Exception bcFailure) {
            System.out.println("BC/PKCS12 读取失败（" + bcFailure.getMessage() + "），回落 Kona/PKCS12");
            register("com.tencent.kona.crypto.KonaCryptoProvider", "KonaCrypto");
            register("com.tencent.kona.pkix.KonaPKIXProvider", "KonaPKIX");
            try (InputStream is = Files.newInputStream(in)) {
                KeyStore ks = KeyStore.getInstance("PKCS12", "KonaPKIX");
                ks.load(is, storePass.toCharArray());
                System.out.println("source read via KonaPKIX/PKCS12");
                return ks;
            }
        }
    }

    private static void register(String className, String name) {
        if (Security.getProvider(name) != null) {
            return;
        }
        try {
            Provider provider = (Provider) Class.forName(className).getDeclaredConstructor().newInstance();
            Security.addProvider(provider);
        } catch (ClassNotFoundException | LinkageError e) {
            throw new IllegalStateException("缺少 provider 类 " + className + "（检查 classpath）", e);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("provider 注册失败: " + className, e);
        }
    }
}
