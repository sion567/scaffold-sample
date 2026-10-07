package com.scaffold.common.gm.field;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * FileSymmetricKeyProvider 测试：16B root key 读取、mtime 缓存重载、运行期切版本与非法 key 拒绝。
 *
 * @author scaffold
 */
class FileSymmetricKeyProviderTest {

    private static final byte[] KEY_V1 = "0123456789abcdef".getBytes();
    private static final byte[] KEY_V2 = "fedcba9876543210".getBytes();

    @TempDir
    Path dir;

    private FileSymmetricKeyProvider newProvider(String version) throws Exception {
        writeKey("v1", KEY_V1);
        writeKey("v2", KEY_V2);
        FileSymmetricKeyProvider provider = new FileSymmetricKeyProvider(dir.toString(), version, 0);
        provider.init();
        return provider;
    }

    private void writeKey(String version, byte[] key) throws Exception {
        Files.write(dir.resolve("sm4-root-" + version), key);
    }

    @Test
    @DisplayName("init 缺配置快速失败")
    void initRequiresConfig() {
        assertThrows(IllegalStateException.class,
                () -> new FileSymmetricKeyProvider("", "v1", 0).init());
        assertThrows(IllegalStateException.class,
                () -> new FileSymmetricKeyProvider(dir.toString(), "", 0).init());
    }

    @Test
    @DisplayName("getKey：读取 16B 密钥；文件缺失/长度非法拒绝")
    void keyAccess() throws Exception {
        FileSymmetricKeyProvider provider = newProvider("v1");
        assertArrayEquals(KEY_V1, provider.getKey("v1"));
        assertThrows(IllegalStateException.class, () -> provider.getKey("missing"));
        Files.write(dir.resolve("sm4-root-bad"), "short".getBytes());
        assertThrows(IllegalStateException.class, () -> provider.getKey("bad"));
    }

    @Test
    @DisplayName("reload(newVersion)：运行期切换活跃版本，缺文件拒绝")
    void versionSwitch() throws Exception {
        FileSymmetricKeyProvider provider = newProvider("v1");
        assertEquals("v1", provider.getActiveVersion());
        provider.reload("v2");
        assertEquals("v2", provider.getActiveVersion());
        assertThrows(IllegalStateException.class, () -> provider.reload("nope"));
        assertEquals("v2", provider.getActiveVersion(), "失败的切换不得改变活跃版本");
    }

    @Test
    @DisplayName("mtime 轮询：文件被替换后 reload 拾取新密钥")
    void reloadPicksUpReplacedFile() throws Exception {
        FileSymmetricKeyProvider provider = newProvider("v1");
        provider.reload();
        byte[] replaced = Arrays.copyOf(KEY_V1, 16);
        replaced[0] = 'X';
        // mtime 粒度可能不足 1ms：改写内容并确保 mtime 变化
        Path file = dir.resolve("sm4-root-v1");
        Thread.sleep(20);
        Files.write(file, replaced);
        provider.reload();
        assertArrayEquals(replaced, provider.getKey("v1"));
    }

    @Nested
    @DisplayName("与 GmFieldCrypto 集成")
    class WithFieldCrypto {
        @Test
        @DisplayName("切版本后新加密走新版本，旧版本密文照解")
        void versionedDecrypt() throws Exception {
            FileSymmetricKeyProvider provider = newProvider("v1");
            GmFieldCrypto crypto = new GmFieldCrypto(provider, "DEK");
            crypto.init();

            String cipherV1 = crypto.encrypt("存量敏感数据");
            assertTrue(crypto.isVersioned(cipherV1));
            assertTrue(cipherV1.startsWith("$SM4$v1$GCM$"));

            provider.reload("v2");
            String cipherV2 = crypto.encrypt("新版本数据");
            assertTrue(cipherV2.startsWith("$SM4$v2$GCM$"));
            assertEquals("存量敏感数据", crypto.decrypt(cipherV1), "旧版本密文仍可解");
            assertEquals("新版本数据", crypto.decrypt(cipherV2));
            assertNotEquals(cipherV1, cipherV2);
        }
    }
}
