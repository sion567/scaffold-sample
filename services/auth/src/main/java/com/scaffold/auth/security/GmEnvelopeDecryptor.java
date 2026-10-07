package com.scaffold.auth.security;

import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.apache.dubbo.rpc.Invocation;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaffold.auth.form.GmEnvelopeBody;
import com.scaffold.common.core.crypto.Hexs;
import com.scaffold.common.core.crypto.Sm2Engine;
import com.scaffold.common.gm.keystore.GmIdentity;
import com.scaffold.common.security.utils.SecurityUtils;

/**
 * 国密信封解密核心（协议 V1.2，供 Dubbo provider 过滤器 GmEnvelopeDecryptFilter 调用）。
 * <p>
 * 请求协议：前端每次请求随机生成 SM4 密钥加密业务 JSON（encData，SM4-ECB-PKCS7 hex），
 * 用服务端 SM2 公钥加密该 SM4 密钥（encKey，C1C3C2，04 前缀 hex），连同
 * timestamp/nonce/signature 作为信封字段随请求体提交；本类对识别出的信封参数完成：
 * <ol>
 *   <li>防重放：timestamp 窗口校验（40001）+ nonce 唯一性（40002）</li>
 *   <li>SM2 会话验签（40003/40005/40006）：attachment 携带 token 的请求必须携带签名，
 *       用该会话签发的公钥（gm:sign:pub:{token}）做 SM3withSM2 验签；
 *       无 token 的登录前流量仅信封+防重放，不验签。
 *       依赖 triple REST 将 Authorization 头映射为 invocation attachment；
 *       若运行时映射缺失则自动退化为仅防重放（日志可见）</li>
 *   <li>信封解密（40004）：SM2 解 encKey → SM4 解 encData，明文 JSON 反序列化为原参数类型，
 *       由过滤器替换调用参数——业务方法拿到的是标准明文对象，对加解密完全无感</li>
 * </ol>
 * 未携带 encKey/encData 的信封参数原样透传（明文兼容）；非信封参数不做任何处理。
 */
public class GmEnvelopeDecryptor {

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private static final Logger log = LoggerFactory.getLogger(GmEnvelopeDecryptor.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 协议错误码（与前端 request.ts 的 40006/40003 重放逻辑联动） */
    public static final int CODE_EXPIRED = 40001;
    public static final int CODE_REPLAY = 40002;
    public static final int CODE_BAD_SIGNATURE = 40003;
    public static final int CODE_DECRYPT_FAILED = 40004;
    public static final int CODE_MISSING_PARAMS = 40005;
    /** 会话签名钥未初始化/已过期（前端收到后自动重新签发并重放请求） */
    public static final int CODE_SIGN_NOT_INIT = 40006;

    private final GmEnvelopeProperties props;
    private final GmIdentity gmIdentity;
    private final NonceCache nonceCache;
    private final SessionSignKeyStore signKeyStore;

    public GmEnvelopeDecryptor(GmEnvelopeProperties props, GmIdentity gmIdentity,
                               NonceCache nonceCache, SessionSignKeyStore signKeyStore) {
        this.props = props;
        this.gmIdentity = gmIdentity;
        this.nonceCache = nonceCache;
        this.signKeyStore = signKeyStore;
    }

    /**
     * 遍历调用参数，对继承 GmEnvelopeBody 的参数执行防重放、验签与信封解密。
     *
     * @return 替换后的参数数组；没有任何参数被替换时返回 null（调用方无需 setArguments）
     * @throws EnvelopeRejectException 协议校验失败（code 40001~40006）
     */
    public Object[] decryptArguments(Invocation invocation) {
        Object[] args = invocation.getArguments();
        if (args == null || args.length == 0) {
            return null;
        }
        Object[] replaced = null;
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof GmEnvelopeBody body) {
                GmEnvelopeBody decrypted = decryptBody(body, invocation);
                if (decrypted != args[i]) {
                    if (replaced == null) {
                        replaced = args.clone();
                    }
                    replaced[i] = decrypted;
                }
            }
        }
        return replaced;
    }

    /** 解密单个信封参数；未携带 encKey/encData 时原样返回（明文兼容透传） */
    private GmEnvelopeBody decryptBody(GmEnvelopeBody body, Invocation invocation) {
        String timestamp = body.getTimestamp();
        String nonce = body.getNonce();
        if (isBlank(timestamp) || isBlank(nonce)) {
            reject(CODE_MISSING_PARAMS, "缺少必要安全参数");
        }

        // 1. 防重放（40001 / 40002）
        long ts;
        try {
            ts = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            reject(CODE_MISSING_PARAMS, "timestamp 格式错误");
            return body; // reject 必抛，此处仅为编译可达
        }
        if (Math.abs(System.currentTimeMillis() - ts) > props.getTimestampWindow().toMillis()) {
            reject(CODE_EXPIRED, "请求已过期");
        }
        if (!nonceCache.tryRegister("envelope", nonce)) {
            reject(CODE_REPLAY, "请求重复，请勿重复提交");
        }

        // 2. SM2 会话验签：attachment 携带 token 的请求必须验签；登录前流量（无 token）跳过
        verifySignature(body, invocation);

        // 3. 信封解密（40004）：SM2 解 encKey → SM4-ECB 解 encData → 明文绑定到原参数类型
        String encKey = body.getEncKey();
        String encData = body.getEncData();
        if (isBlank(encKey) || isBlank(encData)) {
            return body;
        }
        String sm4KeyHex;
        try {
            sm4KeyHex = gmIdentity.decryptSm2(encKey);
        } catch (Exception e) {
            log.warn("[信封] SM2 解密异常 method={}: {}", invocation.getMethodName(), e.getMessage());
            reject(CODE_DECRYPT_FAILED, "数据解析异常");
            return body;
        }
        if (sm4KeyHex == null || !sm4KeyHex.matches("[0-9a-fA-F]{32}")) {
            log.warn("[信封] 解出的 SM4 密钥格式非法 method={}", invocation.getMethodName());
            reject(CODE_DECRYPT_FAILED, "数据解析异常");
            return body;
        }
        try {
            byte[] plain = sm4EcbPkcs7Decrypt(Hexs.decodeHexStr(sm4KeyHex), Hexs.decodeHexStr(encData));
            return MAPPER.readValue(new String(plain, StandardCharsets.UTF_8), body.getClass());
        } catch (Exception e) {
            log.warn("[信封] SM4 解密/参数绑定失败 method={}: {}", invocation.getMethodName(), e.getMessage());
            reject(CODE_DECRYPT_FAILED, "数据解析异常");
            return body;
        }
    }

    /**
     * 会话验签：签名串 = {encKey, encData, timestamp, nonce} ASCII 升序拼串
     * （与前端 gmEnvelope.ts 的 canonical 一致，业务字段不参与签名）。
     */
    private void verifySignature(GmEnvelopeBody body, Invocation invocation) {
        String token = attachmentToken(invocation);
        if (token == null || token.isBlank()) {
            return;
        }
        String sessionPubHex = signKeyStore.publicKeyOf(token);
        if (sessionPubHex == null || sessionPubHex.isBlank()) {
            reject(CODE_SIGN_NOT_INIT, "签名会话未初始化，请重新获取签名密钥");
        }
        if (isBlank(body.getSignature())) {
            reject(CODE_MISSING_PARAMS, "缺少签名参数");
        }
        Map<String, String> fields = new HashMap<>();
        fields.put(EnvelopeCanonical.TIMESTAMP, body.getTimestamp());
        fields.put(EnvelopeCanonical.NONCE, body.getNonce());
        fields.put("encKey", body.getEncKey());
        fields.put("encData", body.getEncData());
        String canonical = EnvelopeCanonical.build(fields);
        boolean ok;
        try {
            Sm2Engine verifyEngine = new Sm2Engine(null, Hexs.decodeHexStr(sessionPubHex));
            ok = verifyEngine.verify(canonical, body.getSignature());
        } catch (Exception e) {
            log.warn("[信封] 验签异常 method={}: {}", invocation.getMethodName(), e.getMessage());
            ok = false;
        }
        if (!ok) {
            log.warn("[信封] 验签失败 method={}", invocation.getMethodName());
            reject(CODE_BAD_SIGNATURE, "签名验证失败");
        }
    }

    /** 从 attachment 中大小写不敏感地取 Authorization 并剥离 Bearer 前缀；无则返回 null */
    private String attachmentToken(Invocation invocation) {
        Map<String, String> attachments = invocation.getAttachments();
        if (attachments == null) {
            return null;
        }
        for (Map.Entry<String, String> e : attachments.entrySet()) {
            if ("authorization".equalsIgnoreCase(e.getKey()) && e.getValue() != null) {
                return SecurityUtils.replaceTokenPrefix(e.getValue());
            }
        }
        return null;
    }

    private byte[] sm4EcbPkcs7Decrypt(byte[] key, byte[] data) throws Exception {
        Cipher cipher = Cipher.getInstance("SM4/ECB/PKCS7Padding", BouncyCastleProvider.PROVIDER_NAME);
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "SM4"));
        return cipher.doFinal(data);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static void reject(int code, String message) {
        throw new EnvelopeRejectException(code, message);
    }

    /** 协议校验失败；Filter 壳捕获后以 R.fail(code, msg) 短路返回（HTTP 200 报文，联动前端重放逻辑） */
    public static class EnvelopeRejectException extends RuntimeException {

        private final int code;

        public EnvelopeRejectException(int code, String message) {
            super(message);
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }
}
