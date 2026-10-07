package com.scaffold.auth.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 国密信封配置（scaffold.auth.gm-envelope.*，信封解密 + 防重放 + 会话验签）。
 * <p>
 * 由 Dubbo provider 过滤器 GmEnvelopeDecryptFilter（SPI）执行：识别继承
 * GmEnvelopeBody 的接口参数并解密换参，因此不再需要 URL 路径匹配配置。
 * 总开关关闭（默认）时不创建解密器，过滤器对所有调用直接放行。
 * 协议：前端每次请求随机生成 SM4 密钥加密业务数据（encData），
 * 再用服务端 SM2 公钥加密该 SM4 密钥（encKey），连同 timestamp/nonce/signature 一起提交；
 * 服务端验签、防重放后解开信封，业务方法拿到的即为明文。
 */
@ConfigurationProperties(prefix = "scaffold.auth.gm-envelope")
public class GmEnvelopeProperties {

    /** 总开关，true 时才装配信封解密器（默认关闭；依赖国密 keystore 与前端信封协议配合）。 */
    private boolean enabled = false;

    /** timestamp 允许的时间窗口，须与 nonce 缓存 TTL 一致（规范建议 3-5 分钟）。 */
    private Duration timestampWindow = Duration.ofMinutes(5);

    /** 会话签名密钥 TTL；过期后前端收到 40006 自动重新签发，须与登录会话有效期同量级。 */
    private Duration signKeyTtl = Duration.ofMinutes(30);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Duration getTimestampWindow() {
        return timestampWindow;
    }

    public void setTimestampWindow(Duration timestampWindow) {
        this.timestampWindow = timestampWindow;
    }

    public Duration getSignKeyTtl() {
        return signKeyTtl;
    }

    public void setSignKeyTtl(Duration signKeyTtl) {
        this.signKeyTtl = signKeyTtl;
    }
}
