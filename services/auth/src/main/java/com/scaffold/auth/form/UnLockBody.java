package com.scaffold.auth.form;

/**
 * 系统解锁对象（继承信封基类：前端以国密信封提交，GmEnvelopeDecryptFilter 解密后到达）
 *
 * @author scaffold
 */
public class UnLockBody extends GmEnvelopeBody
{
    /**
     * 用户密码（SM2 加密密文）
     */
    private String password;

    /**
     * SM2 客户端标识（登录页 /auth/sm2PublicKey 下发）
     */
    private String clientId;

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    public String getClientId()
    {
        return clientId;
    }

    public void setClientId(String clientId)
    {
        this.clientId = clientId;
    }
}
