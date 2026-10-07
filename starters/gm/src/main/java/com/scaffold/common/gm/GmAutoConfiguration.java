package com.scaffold.common.gm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import com.scaffold.common.gm.field.FileSymmetricKeyProvider;
import com.scaffold.common.gm.field.GmFieldCrypto;
import com.scaffold.common.gm.keystore.GmIdentity;

/**
 * 国密自动装配（BouncyCastle 底座，替代原 zdhr-crypto @EnableSmCrypto + io.zdhr.crypto 组件扫描）。
 * <p>
 * 总开关 {@code scaffold.gm.enabled}（默认 true，引入即生效）；密钥按需装配：
 * <ul>
 *   <li>{@link GmIdentity}：配置 {@code gm.keystore.path} 时创建（auth 信封解密/密钥下发用）；
 *       keystore 格式默认 BCFKS（{@code gm.keystore.format}，PKCS12 仅作存量过渡）；</li>
 *   <li>{@link FileSymmetricKeyProvider} + {@link GmFieldCrypto}：配置 {@code gm.sm4.root-key-dir}
 *       时创建，GmFieldCrypto 启动即注入 FieldCryptoHolder（Sm4FieldCrypto 静态门面可用）。</li>
 * </ul>
 * 未配置对应密钥时相应 bean 不创建（关闭时 Sm4FieldCrypto 调用方会显式失败，
 * 而不是启动即挂——字段加密属可选合规能力）。
 */
@AutoConfiguration
@ConditionalOnProperty(name = "scaffold.gm.enabled", havingValue = "true", matchIfMissing = true)
public class GmAutoConfiguration {

    @Bean
    @ConditionalOnProperty("gm.keystore.path")
    public GmIdentity gmIdentity(
            @Value("${gm.keystore.path}") String keystorePath,
            @Value("${gm.keystore.store-pass}") String storePass,
            @Value("${gm.keystore.key-pass}") String keyPass,
            @Value("${gm.keystore.format:BCFKS}") String format,
            @Value("${gm.keystore.jce-provider:BC}") String jceProvider,
            @Value("${gm.keystore.active-alias}") String activeAlias,
            @Value("${gm.keystore.fail-fast:true}") boolean failFast,
            @Value("${gm.keystore.reload-interval:0}") long reloadInterval) {
        GmIdentity identity = new GmIdentity(keystorePath, storePass, keyPass, format, jceProvider,
                activeAlias, failFast, reloadInterval);
        identity.init();
        return identity;
    }

    @Bean
    @ConditionalOnProperty("gm.sm4.root-key-dir")
    public FileSymmetricKeyProvider fileSymmetricKeyProvider(
            @Value("${gm.sm4.root-key-dir}") String rootKeyDir,
            @Value("${gm.sm4.root-key-version:}") String rootKeyVersion,
            @Value("${gm.sm4.reload-interval:300}") long reloadInterval) {
        FileSymmetricKeyProvider provider = new FileSymmetricKeyProvider(rootKeyDir, rootKeyVersion, reloadInterval);
        provider.init();
        return provider;
    }

    @Bean
    @ConditionalOnBean(FileSymmetricKeyProvider.class)
    public GmFieldCrypto gmFieldCrypto(FileSymmetricKeyProvider fileSymmetricKeyProvider,
            @Value("${gm.sm4.key-role:DEK}") String keyRole) {
        return new GmFieldCrypto(fileSymmetricKeyProvider, keyRole);
    }
}
