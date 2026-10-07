package com.scaffold.auth.form;

/**
 * 用户登录对象（继承信封基类：前端以国密信封提交，GmEnvelopeDecryptFilter 解密后到达）
 *
 * @author scaffold
 */
public class LoginBody extends GmEnvelopeBody
{
    /**
     * 用户名
     */
    private String username;

    /**
     * 用户密码
     */
    private String password;

    /**
     * 双因子验证码（可选合规增强，命中 scaffold.auth.two-factor 配置的账号必填，短信/OTP）
     */
    private String secondAuthCode;

    /**
     * SM2 客户端标识（登录前由 /auth/sm2PublicKey 下发，
     * 后端据此取一次性私钥解密 SM2 加密的密码）
     */
    private String clientId;

    /**
     * 图形验证码（scaffold.auth.captcha.enabled=true 时必填；
     * 网关 /captchaImage 下发，经信封加密到达本服务，解密后校验）
     */
    private String code;

    /**
     * 验证码唯一标识（与 code 配对）
     */
    private String uuid;

    public String getClientId()
    {
        return clientId;
    }

    public void setClientId(String clientId)
    {
        this.clientId = clientId;
    }

    public String getCode()
    {
        return code;
    }

    public void setCode(String code)
    {
        this.code = code;
    }

    public String getUuid()
    {
        return uuid;
    }

    public void setUuid(String uuid)
    {
        this.uuid = uuid;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    public String getSecondAuthCode()
    {
        return secondAuthCode;
    }

    public void setSecondAuthCode(String secondAuthCode)
    {
        this.secondAuthCode = secondAuthCode;
    }
}
