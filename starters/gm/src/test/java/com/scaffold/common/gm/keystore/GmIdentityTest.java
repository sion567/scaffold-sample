package com.scaffold.common.gm.keystore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.scaffold.common.core.crypto.Hexs;
import com.scaffold.common.core.crypto.Sm2Engine;
import com.scaffold.common.core.crypto.Sm2Keys;

/**
 * GmIdentity 测试：真实 keystore（仓库 dev 密钥，alias 202607-h1）加载、
 * 公钥下发格式（04 前缀 130 hex）、sm-crypto 形态（无 04 前缀）信封解密、签名验签与别名切换。
 * 主路径走 BCFKS（默认格式，format 传 null 验证缺省行为）；PKCS12 仅保留存量库读取回归。
 *
 * @author scaffold
 */
class GmIdentityTest {

    private static final String STORE_PASS = "StorePass123";
    private static final String KEY_PASS = "KeyPass123";
    private static final String ALIAS = "202607-h1";

    private static String bcfksPath;
    private static String p12Path;

    @TempDir
    Path tempDir;

    @BeforeAll
    static void locateKeystore() {
        bcfksPath = Path.of("src", "test", "resources", "keystore", "gm.bcfks").toString();
        p12Path = Path.of("src", "test", "resources", "keystore", "gm.p12").toString();
    }

    private GmIdentity newIdentity() {
        GmIdentity identity = new GmIdentity(bcfksPath, STORE_PASS, KEY_PASS, null, "BC",
                ALIAS, true, 0L);
        identity.init();
        return identity;
    }

    @Test
    @DisplayName("init：加载 BCFKS keystore（format 缺省），活动别名与公钥格式正确")
    void loadsKeystore() {
        GmIdentity identity = newIdentity();
        assertEquals(ALIAS, identity.getActiveAlias());
        String pubHex = identity.getActivePublicKeyHex();
        assertEquals(130, pubHex.length(), "65B 非压缩点 = 130 hex 字符");
        assertTrue(pubHex.startsWith("04"));
        assertTrue(identity.getAliases().contains(ALIAS));
    }

    @Test
    @DisplayName("存量 PKCS12 库：显式 format=PKCS12 仍可读（过渡兼容）")
    void loadsLegacyPkcs12() {
        GmIdentity identity = new GmIdentity(p12Path, STORE_PASS, KEY_PASS, "PKCS12", "BC",
                ALIAS, true, 0L);
        identity.init();
        assertEquals(ALIAS, identity.getActiveAlias());
        assertEquals(identity.getActivePublicKeyHex(), newIdentity().getActivePublicKeyHex(),
                "PKCS12 与 BCFKS 是同一对密钥材料");
    }

    @Test
    @DisplayName("缺必填配置快速失败；错误口令/错误别名拒绝")
    void configAndCredentialGuards() {
        assertThrows(IllegalStateException.class,
                () -> new GmIdentity("", STORE_PASS, KEY_PASS, null, "BC", ALIAS, true, 0L).init());
        assertThrows(IllegalStateException.class,
                () -> new GmIdentity(bcfksPath, null, KEY_PASS, null, "BC", ALIAS, true, 0L).init());
        assertThrows(IllegalStateException.class,
                () -> new GmIdentity(bcfksPath, "wrong-pass", KEY_PASS, null, "BC", ALIAS, true, 0L).init());
        // 别名不存在：init 不中断（保留空快照），取钥时失败
        GmIdentity noAlias = new GmIdentity(bcfksPath, STORE_PASS, KEY_PASS, null, "BC", "no-such-alias", true, 0L);
        noAlias.init();
        assertThrows(IllegalStateException.class, noAlias::getSm2PrivateKey);
    }

    @Test
    @DisplayName("decryptSm2：标准/无 04 前缀（sm-crypto 形态）密文均可解")
    void decryptSm2Forms() {
        GmIdentity identity = newIdentity();
        String pubHex = identity.getActivePublicKeyHex();
        Sm2Engine encryptor = new Sm2Engine(null, Hexs.decodeHexStr(pubHex));

        String with04 = encryptor.encryptHex("envelope-sm4-key");
        assertEquals("envelope-sm4-key", identity.decryptSm2(with04));

        String without04 = with04.substring(2);
        assertNotEquals("04", without04.substring(0, 2));
        assertEquals("envelope-sm4-key", identity.decryptSm2(without04), "sm-crypto 默认无 04 前缀");
    }

    @Test
    @DisplayName("decryptSm2：密钥不符/密文被篡改时解密失败")
    void decryptWrongKeyFails() {
        GmIdentity identity = newIdentity();
        java.security.KeyPair other = Sm2Engine.generateKeyPair();
        Sm2Engine encryptor = new Sm2Engine(Sm2Keys.privateBytes(other.getPrivate()),
                Sm2Keys.publicBytes(other.getPublic()));
        assertThrows(Exception.class, () -> identity.decryptSm2(encryptor.encryptHex("data")));
    }

    @Test
    @DisplayName("sign/verify 回环：keystore 私钥签名可用自身公钥验证")
    void signVerifyRoundTrip() {
        GmIdentity identity = newIdentity();
        String sig = identity.sign("canonical-data");
        assertNotNull(sig);
        assertTrue(identity.verify("canonical-data", sig));
        assertTrue(new Sm2Engine(null, Hexs.decodeHexStr(identity.getActivePublicKeyHex()))
                .verify("canonical-data", sig), "公钥引擎交叉验证");
        assertFalse(identity.verify("tampered", sig));
    }

    @Test
    @DisplayName("reload(newAlias)：切到不存在别名时告警并保留原快照（与原 zdhr 语义一致）")
    void aliasSwitchGuard() {
        GmIdentity identity = newIdentity();
        identity.reload("no-such-alias");
        assertEquals(ALIAS, identity.getActiveAlias(), "失败切换不得改变活跃别名");
    }
}
