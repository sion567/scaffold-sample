package com.scaffold.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 图形验证码校验配置（scaffold.auth.captcha.*）。
 * <p>
 * 验证码图片由网关生成下发（GET /captchaImage，网关侧配置 security.captcha.*），
 * 登录/注册请求经国密信封解密后在本服务校验 code/uuid，因此「校验开关」在 auth 侧。
 * 两处开关须保持一致：网关开而本服务关则验证码形同虚设；网关关而本服务开
 * 则前端无码可填、登录被拒。
 *
 * @author scaffold
 */
@Component
@ConfigurationProperties(prefix = "scaffold.auth.captcha")
public class CaptchaProperties
{
    /** 验证码校验开关：true 时登录/注册必须携带有效 code/uuid */
    private boolean enabled = true;

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }
}
