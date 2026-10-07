package com.scaffold.auth.api;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import com.scaffold.auth.form.LoginBody;
import com.scaffold.auth.form.RegisterBody;
import com.scaffold.auth.form.UnLockBody;
import com.scaffold.common.core.domain.R;

/**
 * token 认证服务（Triple REST 对外接口）
 *
 * @author scaffold
 */
public interface TokenResource
{
    /**
     * 用户登录
     */
    @PostMapping("login")
    R<?> login(@RequestBody LoginBody form);

    /**
     * 双因子验证码获取（可选合规增强，是否强制由 scaffold.auth.two-factor 配置决定）
     * 开发环境返回验证码便于联调；生产环境经短信通道发送，不返回
     */
    @PostMapping("sendOtp")
    R<?> sendOtp(@RequestBody LoginBody form);

    /**
     * 用户退出
     */
    @DeleteMapping("logout")
    R<?> logout(@RequestHeader("Authorization") String authorization);

    /**
     * 刷新令牌有效期
     */
    @PostMapping("refresh")
    R<?> refresh(@RequestHeader("Authorization") String authorization);

    /**
     * 用户注册
     */
    @PostMapping("register")
    R<?> register(@RequestBody RegisterBody registerBody);

    /**
     * 解锁屏幕
     */
    @PostMapping("unlockscreen")
    R<?> unlockScreen(@RequestBody UnLockBody unLockBody);
}
