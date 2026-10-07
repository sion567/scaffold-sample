package com.scaffold.common.core.constant;

/**
 * Token的Key常量
 * （JWT 签名密钥已移出硬编码：由 JwtUtils 从系统属性 jwt.secret / 环境变量 JWT_SECRET 读取）
 * 
 * @author ct
 */
public class TokenConstants
{
    /**
     * 令牌前缀
     */
    public static final String PREFIX = "Bearer ";

}
