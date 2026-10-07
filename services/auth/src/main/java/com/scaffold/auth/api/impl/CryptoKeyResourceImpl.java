package com.scaffold.auth.api.impl;

import com.scaffold.auth.security.SessionSignKeyStore;
import com.scaffold.common.core.utils.StringUtils;
import com.scaffold.common.gm.keystore.GmIdentity;
import com.scaffold.common.security.utils.SecurityUtils;
import org.apache.dubbo.config.annotation.DubboService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;



import java.util.HashMap;
import java.util.Map;

/**
 * 国密密钥下发实现。
 * <p>
 * gm-key：仅下发 SM2 公钥（04 前缀 hex）与 keyId（keystore 活动别名），
 * 加密私钥与 SM4 密钥严禁下发给前端。
 * gm-sign-key：会话签名密钥对签发——登录态下后端现生成 SM2 密钥对，
 * 私钥经 TLS 下发给该会话（前端仅内存持有），公钥绑定 token 存 Redis 供验签；
 * 登出/过期即销毁。该端点是唯一的"私钥出后端"通道，且必须已登录。
 * <p>
 */
@DubboService
public class CryptoKeyResourceImpl implements com.scaffold.auth.api.CryptoKeyResource {
    private static final Logger log = LoggerFactory.getLogger(CryptoKeyResourceImpl.class);

    private final GmIdentity gmIdentity;

    private final SessionSignKeyStore signKeyStore;

    public CryptoKeyResourceImpl(GmIdentity gmIdentity, SessionSignKeyStore signKeyStore) {
        this.gmIdentity = gmIdentity;
        this.signKeyStore = signKeyStore;
    }

    @Override
    public ResponseEntity<Map<String, String>> getPublicKey() {
        Map<String, String> data = new HashMap<>();
        data.put("keyId", gmIdentity.getActiveAlias());
        data.put("publicKey", gmIdentity.getActivePublicKeyHex());

        String etag = "W/\"" + gmIdentity.getActiveAlias() + "\"";
        return ResponseEntity.ok()
                .header(HttpHeaders.ETAG, etag)
                .header(HttpHeaders.CACHE_CONTROL, "max-age=300")
                .body(data);
    }

    @Override
    public ResponseEntity<Map<String, String>> getSignKey(String authorization) {
        String token = authorization == null ? null : SecurityUtils.replaceTokenPrefix(authorization);
        if (StringUtils.isEmpty(token)) {
            return ResponseEntity.status(401)
                    .header(HttpHeaders.CACHE_CONTROL, "no-store")
                    .body(Map.of("msg", "未登录，无法签发会话签名密钥"));
        }
        String privateKeyHex = signKeyStore.issue(token);
        log.info("[国密] 会话签名密钥已签发, token={}...", token.substring(0, Math.min(8, token.length())));
        Map<String, String> data = new HashMap<>();
        data.put("privateKey", privateKeyHex);
        // 私钥严禁被中间层缓存
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(data);
    }




}
