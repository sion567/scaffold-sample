package com.scaffold.auth.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 双因子认证配置（scaffold.auth.two-factor.*，可选的合规增强项，默认关闭）。
 * <p>
 * 开启后，登录用户的角色命中 {@link #requiredRoles}（精确匹配）或
 * {@link #requiredRolePrefixes}（前缀匹配）时，登录必须额外通过 OTP 校验；
 * 未命中的账号不受影响。具体命中范围由部署方按需配置，代码不内置任何账号/角色。
 *
 * @author scaffold
 */
@Component
@ConfigurationProperties(prefix = "scaffold.auth.two-factor")
public class TwoFactorProperties
{
    /** 总开关：false（默认）时所有账号登录均不需要双因子 */
    private boolean enabled = false;

    /** 需要双因子的角色列表（与用户角色精确匹配），默认仅 admin */
    private List<String> requiredRoles = new ArrayList<>(List.of("admin"));

    /** 需要双因子的角色前缀列表（与用户角色前缀匹配），默认为空 */
    private List<String> requiredRolePrefixes = new ArrayList<>();

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public List<String> getRequiredRoles()
    {
        return requiredRoles;
    }

    public void setRequiredRoles(List<String> requiredRoles)
    {
        this.requiredRoles = requiredRoles;
    }

    public List<String> getRequiredRolePrefixes()
    {
        return requiredRolePrefixes;
    }

    public void setRequiredRolePrefixes(List<String> requiredRolePrefixes)
    {
        this.requiredRolePrefixes = requiredRolePrefixes;
    }
}
