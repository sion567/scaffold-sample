package com.scaffold.auth.api;

import jakarta.annotation.security.PermitAll;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

/**
 * 国密密钥下发接口。
 * <p>
 * 网关路由 /auth/** 并 StripPrefix=1，故 scaffold-auth 内部路径为根路径下的 /gm-key，
 * 前端通过 GET /auth/gm-key 访问（参考 {@link TokenResource} 同样的路径约定）。
 */
@RequestMapping("/")
public interface CryptoKeyResource {

    @GetMapping("gm-key")
    @PermitAll
    ResponseEntity<Map<String, String>> getPublicKey();

    /**
     * 会话签名密钥对签发（需登录态）。
     * <p>
     * 后端按会话生成 SM2 签名密钥对：私钥经 TLS 下发给该已登录会话（仅前端内存持有），
     * 公钥绑定 token 存 Redis 供验签；登出/过期即销毁——签名恒在，无构建期注入。
     * 该路径在信封过滤器白名单中（bootstrap 请求本身不套信封、不验签）。
     */
    @GetMapping("gm-sign-key")
    ResponseEntity<Map<String, String>> getSignKey(
            @RequestHeader(value = "Authorization", required = false) String authorization);
}
