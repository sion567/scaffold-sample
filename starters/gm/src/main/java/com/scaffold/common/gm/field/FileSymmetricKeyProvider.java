package com.scaffold.common.gm.field;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 文件版 SM4 对称密钥提供者：从 {@code sm4-root-<version>} 文件读 16 字节密钥。
 * <p>
 * 配置: {@code gm.sm4.root-key-dir}（目录）、{@code gm.sm4.root-key-version}（新加密用版本）、
 * {@code gm.sm4.reload-interval}（秒，>0 启用定时轮询，默认 300）。
 * root key 文件: {@code <root-key-dir>/sm4-root-<version>}，16 字节，600 权限。
 * <p>
 * 轮询语义: 每 tick 检查活跃版本 key 文件的 mtime，未变化则跳过（不打日志）；
 * 文件被原子替换（mtime 变化）后重载并记录审计。运行期切版本用 {@link #reload(String)}。
 */
public class FileSymmetricKeyProvider {

    private static final Logger log = LoggerFactory.getLogger(FileSymmetricKeyProvider.class);
    private static final Logger auditLog = LoggerFactory.getLogger(FileSymmetricKeyProvider.class.getName() + ".audit");

    private final String rootKeyDir;
    private final String configuredVersion;
    private final long reloadIntervalSeconds;

    /** 当前活跃版本（新加密用）。构造器/init() 初始化；可被 reload(String) 运行期切换 */
    private volatile String activeVersion;

    /** 各版本 key 文件 mtime 缓存：轮询只在 mtime 变化时重载 */
    private final Map<String, Long> versionMtimes = new ConcurrentHashMap<>();
    /** 失败日志风暴防护：仅在"转失败/转成功"时各记一次 */
    private volatile boolean lastReloadFailed = false;

    private ScheduledExecutorService reloadScheduler;

    public FileSymmetricKeyProvider(String rootKeyDir, String configuredVersion, long reloadIntervalSeconds) {
        this.rootKeyDir = rootKeyDir;
        this.configuredVersion = configuredVersion;
        this.reloadIntervalSeconds = reloadIntervalSeconds;
        this.activeVersion = configuredVersion;
    }

    /** 启动期装配入口（自动配置在创建 bean 后调用） */
    public void init() {
        if (rootKeyDir == null || rootKeyDir.isBlank() || configuredVersion == null || configuredVersion.isBlank()) {
            throw new IllegalStateException("gm.sm4.root-key-dir and gm.sm4.root-key-version must be configured");
        }
        startReloadScheduler();
        reload();
        log.info("FileSymmetricKeyProvider initialized, version={}, dir={}", activeVersion, rootKeyDir);
    }

    private void startReloadScheduler() {
        if (reloadIntervalSeconds > 0 && reloadScheduler == null) {
            reloadScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "gm-sm4-reload");
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
            onReloadFailure("scheduled reload failed: " + e.getMessage());
        }
    }

    @jakarta.annotation.PreDestroy
    void shutdownReloadScheduler() {
        if (reloadScheduler != null) {
            reloadScheduler.shutdownNow();
        }
    }

    public String getActiveVersion() {
        return activeVersion;
    }

    public byte[] getKey(String version) {
        Path file = versionFile(version);
        if (!Files.exists(file)) {
            auditLog.error("SM4 root key file not found: {}", file);
            throw new IllegalStateException("SM4 root key file not found: " + file);
        }
        try {
            byte[] key = Files.readAllBytes(file);
            if (key.length != 16) {
                throw new IllegalStateException("SM4 root key must be 16 bytes: " + file);
            }
            return key;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read SM4 root key: " + file, e);
        }
    }

    /** 检查活跃版本 key 文件 mtime，变化则重载 */
    public void reload() {
        String v = activeVersion;
        Path file = versionFile(v);
        long mtime = mtimeOf(file);
        if (mtime < 0) {
            onReloadFailure("SM4 root key file not found: " + file);
            return;
        }
        Long cached = versionMtimes.get(v);
        if (cached != null && cached == mtime) {
            return;
        }
        try {
            byte[] key = Files.readAllBytes(file);
            if (key.length != 16) {
                onReloadFailure("SM4 root key must be 16 bytes: " + file);
                return;
            }
            versionMtimes.put(v, mtime);
            if (lastReloadFailed) {
                auditLog.info("FileSymmetricKeyProvider reload recovered, version={}", v);
                lastReloadFailed = false;
            }
            auditLog.info("FileSymmetricKeyProvider reloaded, activeVersion={}", v);
        } catch (IOException e) {
            onReloadFailure("Failed to read SM4 root key: " + file);
        }
    }

    /** 运行期切换活跃版本（新加密用），需 newVersion 的 key 文件存在且为 16 字节 */
    public void reload(String newVersion) {
        Path file = versionFile(newVersion);
        if (!Files.exists(file)) {
            auditLog.error("FileSymmetricKeyProvider reload({}) rejected: file missing {}", newVersion, file);
            throw new IllegalStateException("SM4 root key file not found: " + file);
        }
        byte[] key;
        try {
            key = Files.readAllBytes(file);
        } catch (IOException e) {
            auditLog.error("FileSymmetricKeyProvider reload({}) rejected: read failed {}", newVersion, file);
            throw new IllegalStateException("Failed to read SM4 root key: " + file, e);
        }
        if (key.length != 16) {
            auditLog.error("FileSymmetricKeyProvider reload({}) rejected: key must be 16 bytes {}", newVersion, file);
            throw new IllegalStateException("SM4 root key must be 16 bytes: " + file);
        }
        versionMtimes.put(newVersion, mtimeOf(file));
        this.activeVersion = newVersion;
        lastReloadFailed = false;
        auditLog.info("FileSymmetricKeyProvider reloaded, activeVersion={}", newVersion);
    }

    private long mtimeOf(Path file) {
        if (!Files.exists(file)) {
            return -1;
        }
        try {
            return Files.getLastModifiedTime(file).toMillis();
        } catch (IOException e) {
            return -1;
        }
    }

    private void onReloadFailure(String message) {
        if (!lastReloadFailed) {
            auditLog.error("FileSymmetricKeyProvider reload failed: {}", message);
            lastReloadFailed = true;
        }
    }

    private Path versionFile(String version) {
        return Path.of(rootKeyDir, "sm4-root-" + version);
    }
}
