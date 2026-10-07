package com.scaffold.auth.api.impl;

import org.springframework.web.bind.annotation.RequestHeader;

import com.scaffold.auth.service.TwoFactorService;
import com.scaffold.common.gm.keystore.GmIdentity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.dubbo.config.annotation.DubboService;
import com.scaffold.auth.api.TokenResource;
import com.scaffold.auth.form.LoginBody;
import com.scaffold.auth.form.RegisterBody;
import com.scaffold.auth.form.UnLockBody;
import com.scaffold.auth.service.SysLoginService;
import com.scaffold.auth.security.SessionSignKeyStore;
import com.scaffold.common.core.domain.R;
import com.scaffold.common.core.utils.JwtUtils;
import com.scaffold.common.core.utils.StringUtils;
import com.scaffold.common.security.auth.AuthUtil;
import com.scaffold.common.security.service.TokenService;
import com.scaffold.common.security.utils.SecurityUtils;
import com.scaffold.system.api.model.LoginUser;

/**
 * token 认证服务实现（Triple REST）
 *
 * @author scaffold
 */
@DubboService
public class TokenResourceImpl implements TokenResource
{
    private static final Logger log = LoggerFactory.getLogger(TokenResourceImpl.class);

    private final TokenService tokenService;
    private final SysLoginService sysLoginService;
    private final TwoFactorService twoFactorService;
    private final GmIdentity gmIdentity;
    private final SessionSignKeyStore signKeyStore;

    public TokenResourceImpl(TokenService tokenService, SysLoginService sysLoginService,
                             TwoFactorService twoFactorService, GmIdentity gmIdentity,
                             SessionSignKeyStore signKeyStore) {
        this.tokenService = tokenService;
        this.sysLoginService = sysLoginService;
        this.twoFactorService = twoFactorService;
        this.gmIdentity = gmIdentity;
        this.signKeyStore = signKeyStore;
    }

    @Override
    public R<?> login(LoginBody form)
    {
        String password = gmIdentity.decryptSm2(form.getPassword());
        // 用户登录（图形验证码/双因子开启时，分别校验 code/uuid 与 secondAuthCode）
        LoginUser userInfo = sysLoginService.login(form.getUsername(), password, form.getSecondAuthCode(),
                form.getCode(), form.getUuid());
        // 获取登录token
        return R.ok(tokenService.createToken(userInfo));
    }

    /**
     * 双因子验证码获取（可选合规增强，按 scaffold.auth.two-factor 配置生效）
     * 开发环境返回验证码便于联调；生产环境经短信通道发送，不返回
     */
    public R<?> sendOtp(LoginBody form)
    {
        if (StringUtils.isBlank(form.getUsername()))
        {
            return R.fail("用户名不能为空");
        }
        String code = twoFactorService.sendOtp(form.getUsername());
        return code != null ? R.ok(code) : R.ok();
    }

    @Override
    public R<?> logout(@RequestHeader("Authorization") String authorization)
    {
        String token = SecurityUtils.replaceTokenPrefix(authorization);
        if (StringUtils.isNotEmpty(token))
        {
            String username = null;
            try
            {
                // JWT 可能已过期（30 分钟有效期），过期视为已退出，不阻塞登出
                username = JwtUtils.getUserName(token);
            }
            catch (Exception e)
            {
                log.warn("登出时解析 token 异常（可能已过期）: {}", e.getMessage());
            }
            // 撤销会话签名密钥（规范 V1.2：登出即销毁）
            signKeyStore.revoke(token);
            // 删除用户缓存记录
            AuthUtil.logoutByToken(token);
            // 记录用户退出日志
            sysLoginService.logout(username);
        }
        return R.ok();
    }

    @Override
    public R<?> refresh(@RequestHeader("Authorization") String authorization)
    {
        String token = SecurityUtils.replaceTokenPrefix(authorization);
        LoginUser loginUser = tokenService.getLoginUser(token);
        if (StringUtils.isNotNull(loginUser))
        {
            // 刷新令牌有效期
            tokenService.refreshToken(loginUser);
        }
        return R.ok();
    }

    @Override
    public R<?> register(RegisterBody registerBody)
    {
        String password = gmIdentity.decryptSm2(registerBody.getPassword());
        // 用户注册（图形验证码开启时校验 code/uuid）
        sysLoginService.register(registerBody.getUsername(), password, registerBody.getCode(), registerBody.getUuid());
        return R.ok();
    }

    @Override
    public R<?> unlockScreen(UnLockBody unLockBody)
    {
        String password = gmIdentity.decryptSm2(unLockBody.getPassword());
        sysLoginService.unlock(password);
        return R.ok();
    }
}
