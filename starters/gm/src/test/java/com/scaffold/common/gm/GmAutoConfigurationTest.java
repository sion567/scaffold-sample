package com.scaffold.common.gm;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.scaffold.common.gm.field.FileSymmetricKeyProvider;
import com.scaffold.common.gm.field.GmFieldCrypto;
import com.scaffold.common.gm.keystore.GmIdentity;

/**
 * GmAutoConfiguration 条件装配测试：总开关 scaffold.gm.enabled、
 * gm.keystore.path / gm.sm4.root-key-dir 按需创建对应 bean。
 *
 * @author scaffold
 */
class GmAutoConfigurationTest {

    private static final String KEY_STORE = "src/test/resources/keystore/gm.bcfks";

    @TempDir
    Path dir;

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(GmAutoConfiguration.class);

    private String sm4Dir() throws Exception {
        Files.write(dir.resolve("sm4-root-v1"), "0123456789abcdef".getBytes(StandardCharsets.UTF_8));
        return dir.toString();
    }

    @Test
    @DisplayName("keystore + sm4 配置齐备：三个 bean 全装配")
    void fullWiring() throws Exception {
        runner.withPropertyValues(
                "gm.keystore.path=" + KEY_STORE,
                "gm.keystore.store-pass=StorePass123",
                "gm.keystore.key-pass=KeyPass123",
                "gm.keystore.active-alias=202607-h1",
                "gm.sm4.root-key-dir=" + sm4Dir(),
                "gm.sm4.root-key-version=v1").run(context -> {
                    assertThat(context).hasSingleBean(GmIdentity.class);
                    assertThat(context).hasSingleBean(FileSymmetricKeyProvider.class);
                    assertThat(context).hasSingleBean(GmFieldCrypto.class);
                });
    }

    @Test
    @DisplayName("缺 keystore/sm4 配置：对应 bean 不创建（配置可拔）")
    void partialWiring() throws Exception {
        runner.withPropertyValues("gm.sm4.root-key-dir=" + sm4Dir(),
                "gm.sm4.root-key-version=v1").run(context -> {
                    assertThat(context).doesNotHaveBean(GmIdentity.class);
                    assertThat(context).hasSingleBean(FileSymmetricKeyProvider.class);
                    assertThat(context).hasSingleBean(GmFieldCrypto.class);
                });
        runner.run(context -> {
            assertThat(context).doesNotHaveBean(GmIdentity.class);
            assertThat(context).doesNotHaveBean(FileSymmetricKeyProvider.class);
            assertThat(context).doesNotHaveBean(GmFieldCrypto.class);
        });
    }

    @Test
    @DisplayName("scaffold.gm.enabled=false：总开关关闭，全部不装配")
    void masterSwitchOff() throws Exception {
        runner.withPropertyValues(
                "scaffold.gm.enabled=false",
                "gm.keystore.path=" + KEY_STORE,
                "gm.sm4.root-key-dir=" + sm4Dir()).run(context -> {
                    assertThat(context).doesNotHaveBean(GmIdentity.class);
                    assertThat(context).doesNotHaveBean(FileSymmetricKeyProvider.class);
                    assertThat(context).doesNotHaveBean(GmFieldCrypto.class);
                });
    }
}
