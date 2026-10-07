package com.scaffold.auth.form;

/**
 * 国密信封请求基类（协议 V1.2）。
 * <p>
 * 继承本类的 DTO 表示「该接口接受信封加密请求」：前端把业务 JSON 整体用随机 SM4
 * 密钥加密为 encData（SM4-ECB-PKCS7 hex），SM4 密钥用服务端 SM2 公钥加密为 encKey
 * （C1C3C2，04 前缀 hex），连同 timestamp/nonce/signature 一起随请求体提交。
 * Dubbo provider 过滤器 GmEnvelopeDecryptFilter 识别出本类型参数后完成防重放、
 * 会话验签与解密，并把参数替换为解密后的业务对象，业务方法对加解密无感。
 * <p>
 * 信封字段必须声明在 DTO 上（而不是从请求体里动态读取）：triple REST 在进入
 * Filter 链之前就完成了 JSON 反序列化，未声明的字段会被丢弃。
 *
 * @author scaffold
 */
public class GmEnvelopeBody
{
    /** SM2 加密后的临时 SM4 密钥（C1C3C2，04 前缀 hex） */
    private String encKey;

    /** SM4-ECB-PKCS7 加密的业务 JSON（hex） */
    private String encData;

    /** 请求时间戳（毫秒），允许窗口见 scaffold.auth.gm-envelope.timestamp-window */
    private String timestamp;

    /** 一次性随机数（防重放，Redis SETNX 判重） */
    private String nonce;

    /** SM2 会话签名（{encKey, encData, timestamp, nonce} ASCII 升序拼串） */
    private String signature;

    public String getEncKey()
    {
        return encKey;
    }

    public void setEncKey(String encKey)
    {
        this.encKey = encKey;
    }

    public String getEncData()
    {
        return encData;
    }

    public void setEncData(String encData)
    {
        this.encData = encData;
    }

    public String getTimestamp()
    {
        return timestamp;
    }

    public void setTimestamp(String timestamp)
    {
        this.timestamp = timestamp;
    }

    public String getNonce()
    {
        return nonce;
    }

    public void setNonce(String nonce)
    {
        this.nonce = nonce;
    }

    public String getSignature()
    {
        return signature;
    }

    public void setSignature(String signature)
    {
        this.signature = signature;
    }
}
