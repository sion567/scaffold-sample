package com.scaffold.common.gm.keystore;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.scaffold.common.core.crypto.Hexs;
import com.scaffold.common.core.crypto.Sm2Engine;
import com.scaffold.common.core.crypto.Sm2Keys;

/**
 * 国密身份（keystore 密钥持有者，BouncyCastle 实现）。
 * <p>
 * 原 zdhr-crypto GmIdentity 的同语义替代：keystore 配置（{@code gm.keystore.*}）、
 * alias 约定（{@code sm2-privateKey-<alias>} / {@code sm2-publicKey-<alias>}，大小写不敏感）、
 * mtime 热轮换与 {@code decryptSm2} 的 04 前缀补全 + auto-try 解密行为全部保持一致。
 * <p>
 * keystore 格式默认 <b>BCFKS</b>（BC 专有格式，{@code gm.keystore.format} 可配）：
 * PKCS12 的私钥袋封装存在跨 provider 兼容问题（Kona 写出的 PBES2 变体 BC 解不开，
 * 报 pad block corrupted），统一 BCFKS 后读写均为同一 provider，不再依赖交换格式兼容性；
 * 存量 PKCS12 库用 {@link GmKeystoreConvertTool} 一次性转换，或显式
 * {@code gm.keystore.format=PKCS12}（走 BC 读，仅限过渡）。
 * <p>
 * 密文/签名格式契约见 {@link Sm2Engine}（raw C1C3C2 / 签名 DER / hex 大写）。
 */
public class GmIdentity {
    private static final Logger log = LoggerFactory.getLogger(GmIdentity.class);
    private static final Logger auditLog = LoggerFactory.getLogger(GmIdentity.class.getName() + ".audit");

    private static final BouncyCastleProvider BC_PROVIDER = new BouncyCastleProvider();

    private final String keystorePath;
    private final String storePass;
    private final String keyPass;
    private final String keystoreFormat;
    private final String jceProvider;
    private final String configuredAlias;
    private final boolean failFast;
    private final long reloadIntervalSeconds;

    /** 轮询调度线程（自管，不依赖消费方 @EnableScheduling；reload-interval<=0 时不创建） */
    private ScheduledExecutorService reloadScheduler;

    /** Immutable snapshot: KeyStore + alias as an atomic unit */
    private record Snapshot(KeyStore ks, String alias, long mtime) {}

    private volatile Snapshot snapshot;
    private volatile long lastModified = -1;
    private final Object lock = new Object();

    public GmIdentity(String keystorePath, String storePass, String keyPass, String keystoreFormat,
                      String jceProvider, String configuredAlias, boolean failFast, long reloadIntervalSeconds) {
        this.keystorePath = keystorePath;
        this.storePass = storePass;
        this.keyPass = keyPass;
        this.keystoreFormat = keystoreFormat == null || keystoreFormat.isBlank() ? "BCFKS" : keystoreFormat;
        this.jceProvider = jceProvider == null || jceProvider.isBlank() ? "BC" : jceProvider;
        this.configuredAlias = configuredAlias;
        this.failFast = failFast;
        this.reloadIntervalSeconds = reloadIntervalSeconds;
    }

    /** 启动期装配入口（自动配置在创建 bean 后调用） */
    public void init() {
        List<String> missing = new ArrayList<>();
        if (keystorePath == null || keystorePath.isBlank()) {
            missing.add("gm.keystore.path");
        }
        if (storePass == null) {
            missing.add("gm.keystore.store-pass");
        }
        if (keyPass == null) {
            missing.add("gm.keystore.key-pass");
        }
        if (configuredAlias == null || configuredAlias.isBlank()) {
            missing.add("gm.keystore.active-alias");
        }
        if (!missing.isEmpty()) {
            throw new IllegalStateException("GmIdentity 缺少必填配置: " + String.join(", ", missing));
        }
        startReloadScheduler();
        reloadIfChanged();
    }

    private void startReloadScheduler() {
        if (reloadIntervalSeconds > 0 && reloadScheduler == null) {
            reloadScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "gm-keystore-reload");
                t.setDaemon(true);
                return t;
            });
            reloadScheduler.scheduleWithFixedDelay(this::reloadQuietly,
                    reloadIntervalSeconds, reloadIntervalSeconds, TimeUnit.SECONDS);
        }
    }

    private void reloadQuietly() {
        try {
            reload();
        } catch (Exception e) {
            auditLog.error("GmIdentity scheduled reload failed: {}", e.getMessage());
        }
    }

    @jakarta.annotation.PreDestroy
    void shutdownReloadScheduler() {
        if (reloadScheduler != null) {
            reloadScheduler.shutdownNow();
        }
    }

    public PublicKey getSm2PublicKey() {
        Snapshot snap = snapshot;
        if (snap == null) {
            throw new IllegalStateException("KeyStore not loaded");
        }
        try {
            X509Certificate cert = (X509Certificate) findEntry(snap.ks, "sm2-publicKey-" + snap.alias, true);
            if (cert == null) {
                throw new IllegalStateException("Certificate not found for alias: " + snap.alias);
            }
            return cert.getPublicKey();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to get public key", e);
        }
    }

    public PrivateKey getSm2PrivateKey() {
        Snapshot snap = snapshot;
        if (snap == null) {
            throw new IllegalStateException("KeyStore not loaded");
        }
        return getSm2PrivateKey(snap.alias);
    }

    public PrivateKey getSm2PrivateKey(String alias) {
        Snapshot snap = snapshot;
        if (snap == null) {
            throw new IllegalStateException("KeyStore not loaded");
        }
        try {
            String full = findAlias(snap.ks, "sm2-privateKey-" + alias);
            if (full == null) {
                throw new IllegalStateException("Private key not found for alias: " + alias);
            }
            PrivateKey key = (PrivateKey) snap.ks.getKey(full, keyPass.toCharArray());
            if (key == null) {
                throw new IllegalStateException("Private key is null for alias: " + alias);
            }
            return key;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to get private key for alias: " + alias, e);
        }
    }

    /** 用当前活跃 alias 的 SM2 私钥解密（自动补 04 前缀，auto-try DER/C1C3C2/C1C2C3）。 */
    public String decryptSm2(String cipher) {
        return decryptSm2(cipher, configuredAlias);
    }

    /** 用指定 alias 的 SM2 私钥解密（供协议栈按历史 alias 解旧密文）。 */
    public String decryptSm2(String cipher, String alias) {
        if (cipher == null || cipher.isEmpty()) {
            throw new IllegalArgumentException("SM2 cipher is empty");
        }
        byte[] privBytes = Sm2Keys.privateBytes(getSm2PrivateKey(alias));
        Snapshot snap = snapshot;
        if (snap == null) {
            throw new IllegalStateException("KeyStore not loaded");
        }
        byte[] pubBytes;
        try {
            X509Certificate cert = (X509Certificate) findEntry(snap.ks, "sm2-publicKey-" + alias, true);
            if (cert == null) {
                throw new IllegalStateException("Certificate not found for alias: " + alias);
            }
            pubBytes = Sm2Keys.publicBytes(cert.getPublicKey());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to get public key for alias: " + alias, e);
        }
        Sm2Engine engine = new Sm2Engine(privBytes, pubBytes);
        String text = cipher;
        // 自动补 04 前缀（sm-crypto 默认输出不带；自定义 DER 首字节 0x30 除外）
        boolean looksLikeDer = text.length() >= 2 && text.startsWith("30");
        if (!text.startsWith("04") && !looksLikeDer) {
            text = "04" + text;
        }
        return engine.decryptStr(text);
    }

    public String getActiveAlias() {
        Snapshot snap = snapshot;
        return snap == null ? null : snap.alias;
    }

    /** 所有 SM2 私钥 alias（active 优先），用于多钥依次解密 */
    public List<String> getAliases() {
        Snapshot snap = snapshot;
        if (snap == null) {
            throw new IllegalStateException("KeyStore not loaded");
        }
        List<String> result = new ArrayList<>();
        String prefix = "sm2-privateKey-";
        try {
            Enumeration<String> aliases = snap.ks.aliases();
            while (aliases.hasMoreElements()) {
                String a = aliases.nextElement();
                // JDK 系 PKCS12 会把 alias 归一化为小写，前缀匹配须大小写不敏感
                if (a.toLowerCase().startsWith(prefix.toLowerCase())) {
                    result.add(a.substring(prefix.length()));
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to enumerate aliases", e);
        }
        result.remove(snap.alias);
        result.add(0, snap.alias);
        return result;
    }

    /** 当前活跃 SM2 公钥 hex（04 前缀，130 字符大写） */
    public String getActivePublicKeyHex() {
        return Hexs.encodeHexStr(Sm2Keys.publicBytes(getSm2PublicKey()));
    }

    /** 使用当前活跃的 SM2 私钥签名（SM3withSM2，DER hex 大写） */
    public String sign(String data) {
        if (data == null) {
            throw new IllegalArgumentException("data 不能为 null");
        }
        Sm2Engine engine = new Sm2Engine(Sm2Keys.privateBytes(getSm2PrivateKey()),
                Sm2Keys.publicBytes(getSm2PublicKey()));
        return engine.sign(data);
    }

    /** 使用当前活跃的 SM2 公钥验签 */
    public boolean verify(String data, String signature) {
        if (data == null || signature == null) {
            throw new IllegalArgumentException("data 和 signature 不能为 null");
        }
        Sm2Engine engine = new Sm2Engine(Sm2Keys.privateBytes(getSm2PrivateKey()),
                Sm2Keys.publicBytes(getSm2PublicKey()));
        return engine.verify(data, signature);
    }

    public void reload() {
        reloadIfChanged();
    }

    /** 运行期切换活跃 alias（keystore 不变时也可直接换别名），需对应私钥+证书存在 */
    public void reload(String newAlias) {
        if (newAlias == null || newAlias.isBlank()) {
            reload();
            return;
        }
        Snapshot snap = snapshot;
        if (snap != null && newAlias.equals(snap.alias)) {
            reloadIfChanged(newAlias);
            return;
        }
        if (!Files.exists(Path.of(keystorePath))) {
            if (failFast) {
                throw new IllegalStateException("Keystore not found: " + keystorePath);
            }
            log.error("Keystore not found: {}. Will retry on next reload.", keystorePath);
            return;
        }
        synchronized (lock) {
            Snapshot latest = snapshot;
            if (latest != null && newAlias.equals(latest.alias)) {
                return;
            }
            long mtime;
            try {
                mtime = Files.getLastModifiedTime(Path.of(keystorePath)).toMillis();
            } catch (IOException e) {
                if (failFast) {
                    throw new IllegalStateException("Failed to get mtime: " + keystorePath, e);
                }
                log.error("Failed to get mtime: {}", keystorePath);
                return;
            }
            doReload(mtime, newAlias);
        }
    }

    private void reloadIfChanged() {
        reloadIfChanged(configuredAlias);
    }

    private void reloadIfChanged(String alias) {
        Path ksPath = Path.of(keystorePath);
        if (!Files.exists(ksPath)) {
            if (failFast) {
                throw new IllegalStateException("Keystore not found: " + ksPath);
            }
            log.error("Keystore not found: {}. Will retry on next reload.", ksPath);
            return;
        }
        long outerMtime;
        try {
            outerMtime = Files.getLastModifiedTime(ksPath).toMillis();
        } catch (Exception e) {
            if (failFast) {
                throw new IllegalStateException("Failed to get mtime: " + ksPath, e);
            }
            log.error("Failed to get mtime: {}", ksPath);
            return;
        }
        if (outerMtime != lastModified) {
            synchronized (lock) {
                long mtime;
                try {
                    mtime = Files.getLastModifiedTime(ksPath).toMillis();
                } catch (IOException e) {
                    if (failFast) {
                        throw new IllegalStateException("Failed to get mtime: " + ksPath, e);
                    }
                    log.error("Failed to get mtime: {}", ksPath);
                    return;
                }
                if (mtime != lastModified) {
                    doReload(mtime, alias);
                }
            }
        }
    }

    private void doReload(long mtime, String alias) {
        KeyStore ks;
        try {
            ks = loadKeyStore();
        } catch (Exception e) {
            auditLog.error("GmIdentity reload failed (keystore load): {}", e.getMessage());
            if (failFast) {
                throw new IllegalStateException("Failed to load keystore: " + keystorePath, e);
            }
            return;
        }
        boolean hasPrivate;
        boolean hasPublic;
        try {
            hasPrivate = findAlias(ks, "sm2-privateKey-" + alias) != null;
            hasPublic = findAlias(ks, "sm2-publicKey-" + alias) != null;
        } catch (Exception e) {
            auditLog.error("GmIdentity reload failed (containsAlias): {}", e.getMessage());
            throw new IllegalStateException("Failed to check alias existence", e);
        }
        if (!hasPrivate || !hasPublic) {
            auditLog.warn("GmIdentity reload skipped: alias '{}' not found, retaining previous snapshot", alias);
            return;
        }
        snapshot = new Snapshot(ks, alias, mtime);
        lastModified = mtime;
        log.info("GmIdentity reloaded, activeAlias={}, mtime={}", alias, lastModified);
        auditLog.info("GmIdentity reloaded, activeAlias={}", alias);
    }

    /**
     * 大小写不敏感 alias 查找（JDK 系 PKCS12 会把 alias 归一化为小写，BC 写入的条目保持原样）。
     *
     * @return 实际存在的 alias；不存在返回 null
     */
    private static String findAlias(KeyStore ks, String alias) throws Exception {
        String wanted = alias.toLowerCase();
        for (Enumeration<String> e = ks.aliases(); e.hasMoreElements(); ) {
            String a = e.nextElement();
            if (a.toLowerCase().equals(wanted)) {
                return a;
            }
        }
        return null;
    }

    /** 大小写不敏感取条目：isCertificate=true 取证书，否则取私钥。 */
    private static Object findEntry(KeyStore ks, String alias, boolean isCertificate) throws Exception {
        String actual = findAlias(ks, alias);
        if (actual == null) {
            return null;
        }
        return isCertificate ? ks.getCertificate(actual) : ks.getKey(actual, null);
    }

    private KeyStore loadKeyStore() throws Exception {
        // BCFKS 为 BC 专有格式，固定走 BC；其他格式（PKCS12 等过渡场景）按 jce-provider 解析
        KeyStore ks;
        if ("BCFKS".equalsIgnoreCase(keystoreFormat) || "BC".equalsIgnoreCase(jceProvider)) {
            ks = KeyStore.getInstance(keystoreFormat, BC_PROVIDER);
        } else {
            ks = KeyStore.getInstance(keystoreFormat, jceProvider);
        }
        try (InputStream is = Files.newInputStream(Path.of(keystorePath))) {
            ks.load(is, storePass.toCharArray());
        }
        return ks;
    }
}
