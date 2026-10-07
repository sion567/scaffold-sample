package com.scaffold.system.service;

import java.util.List;

/**
 * 密码策略校验 服务层
 * <p>
 * 基于 Passay 2.0.0 构建规则，规则来源：
 * <ul>
 *   <li>sys.account.password.minLength：密码最小长度（默认 8）</li>
 *   <li>sys.account.password.complexity：是否要求大小写字母 + 数字 + 特殊字符（默认 true）</li>
 *   <li>sys.account.password.customRules：自定义启用规则（JSON 数组，枚举见实现类）</li>
 * </ul>
 *
 * @author ct
 */
public interface ISysPasswordPolicyService
{
    /**
     * 校验密码是否符合当前密码策略
     *
     * @param rawPassword 明文密码
     * @param username 用户名（用于"禁止包含用户名"规则）
     * @return 不满足时返回错误消息列表（空列表表示通过）
     */
    public List<String> validate(String rawPassword, String username);
}
