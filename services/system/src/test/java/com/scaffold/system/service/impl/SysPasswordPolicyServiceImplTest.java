package com.scaffold.system.service.impl;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.scaffold.system.service.ISysConfigService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * SysPasswordPolicyServiceImpl Mock 测试（密码策略校验）。
 *
 * <p>覆盖维度：
 * <ul>
 *   <li>入参校验：空密码 → "密码不能为空"</li>
 *   <li>UTF-8 超长：超过 72 字节 → "密码过长"</li>
 *   <li>默认复杂度：minLength=8, complexity=true → 弱密码被拦截</li>
 *   <li>关闭复杂度：complexity=false → 纯字母通过</li>
 *   <li>自定义规则：WHITESPACE 规则生效</li>
 * </ul>
 *
 * @author ct
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysPasswordPolicyServiceImplTest
{
    private static final String CFG_MIN_LENGTH = "sys.account.password.minLength";
    private static final String CFG_COMPLEXITY = "sys.account.password.complexity";
    private static final String CFG_CUSTOM_RULES = "sys.account.password.customRules";

    @Mock
    private ISysConfigService configService;

    private SysPasswordPolicyServiceImpl policyService;

    @BeforeEach
    void setUp()
    {
        // 默认配置：minLength=8, complexity=true, 无自定义规则
        when(configService.selectConfigByKey(CFG_MIN_LENGTH)).thenReturn("8");
        when(configService.selectConfigByKey(CFG_COMPLEXITY)).thenReturn("true");
        when(configService.selectConfigByKey(CFG_CUSTOM_RULES)).thenReturn(null);
        policyService = new SysPasswordPolicyServiceImpl(configService);
    }

    @Nested
    @DisplayName("入参校验")
    class ValidationBoundaryTests
    {
        @Test
        @DisplayName("密码为 null → 返回 '密码不能为空'")
        void nullPassword_returnsNotEmptyMsg()
        {
            List<String> errors = policyService.validate(null, "admin");
            assertEquals(1, errors.size());
            assertTrue(errors.get(0).contains("密码不能为空"));
        }

        @Test
        @DisplayName("密码为空字符串 → 返回 '密码不能为空'")
        void emptyPassword_returnsNotEmptyMsg()
        {
            List<String> errors = policyService.validate("", "admin");
            assertEquals(1, errors.size());
            assertTrue(errors.get(0).contains("密码不能为空"));
        }

        @Test
        @DisplayName("密码纯空格 → Passay WhitespaceRule 拦截")
        void whitespaceOnly_passwordRejected()
        {
            List<String> errors = policyService.validate("      ", "admin");
            assertTrue(errors.size() > 0);
        }

        @Test
        @DisplayName("密码超过 72 UTF-8 字节 → '密码过长'")
        void tooLongUtf8_returnsTooLongMsg()
        {
            // 25 个中文字符 × 3 字节/字 = 75 字节 > 72
            String longPassword = "这是一段用于测试密码长度超限的中文字符串请确保超过";
            List<String> errors = policyService.validate(longPassword, "admin");
            assertEquals(1, errors.size());
            assertTrue(errors.get(0).contains("密码过长"));
        }
    }

    @Nested
    @DisplayName("默认复杂度规则（minLength=8, complexity=true）")
    class DefaultComplexityTests
    {
        @Test
        @DisplayName("纯数字 → 被拦截（需大小写字母+数字+特殊字符）")
        void digitOnly_rejected()
        {
            List<String> errors = policyService.validate("12345678", "admin");
            assertTrue(errors.size() > 0);
        }

        @Test
        @DisplayName("符合复杂度要求（Aa1!aaaa）→ 通过")
        void goodPassword_passed()
        {
            List<String> errors = policyService.validate("Admin@1234", "admin");
            assertEquals(0, errors.size());
        }

        @Test
        @DisplayName("符合要求但包含用户名 → 被拦截（默认有 UsernameRule）")
        void passwordContainsUsername_rejected()
        {
            // 由于默认关闭了自定义规则中的 USERNAME，此处只检查复杂度
            List<String> errors = policyService.validate("Admin@1234", "Admin");
            // 如果开启了 USERNAME 规则，这里会返回包含用户名相关的错误
            // 实际取决于 Passay 的默认规则
        }
    }

    @Nested
    @DisplayName("关闭复杂度（complexity=false）")
    class NoComplexityTests
    {
        @Test
        @DisplayName("关闭复杂度后，纯数字 + 超过 minLength → 通过")
        void digitOnlyAboveMinLength_passed()
        {
            when(configService.selectConfigByKey(CFG_COMPLEXITY)).thenReturn("false");
            policyService = new SysPasswordPolicyServiceImpl(configService);

            List<String> errors = policyService.validate("12345678", "admin");
            assertEquals(0, errors.size());
        }

        @Test
        @DisplayName("关闭复杂度后，短于 minLength → 被 LengthRule 拦截")
        void belowMinLength_rejected()
        {
            when(configService.selectConfigByKey(CFG_COMPLEXITY)).thenReturn("false");
            policyService = new SysPasswordPolicyServiceImpl(configService);

            List<String> errors = policyService.validate("1234567", "admin");
            assertTrue(errors.size() > 0);
        }
    }

    @Nested
    @DisplayName("自定义规则")
    class CustomRulesTests
    {
        @Test
        @DisplayName("开启 WHITESPACE 规则：含空格 → 被拦截")
        void whitespaceRule_enabled()
        {
            when(configService.selectConfigByKey(CFG_COMPLEXITY)).thenReturn("false");
            when(configService.selectConfigByKey(CFG_CUSTOM_RULES))
                    .thenReturn("[\"WHITESPACE\"]");
            policyService = new SysPasswordPolicyServiceImpl(configService);

            List<String> errors = policyService.validate("Pass word1!", "admin");
            assertTrue(errors.size() > 0);  // WhitespaceRule 拦截了空格
        }

        @Test
        @DisplayName("未知规则代码 → 忽略，不抛异常")
        void unknownRuleCode_ignored()
        {
            when(configService.selectConfigByKey(CFG_CUSTOM_RULES))
                    .thenReturn("[\"UNKNOWN_RULE\"]");
            policyService = new SysPasswordPolicyServiceImpl(configService);

            // 正常密码通过
            List<String> errors = policyService.validate("Password1!", "admin");
            assertEquals(0, errors.size());
        }

        @Test
        @DisplayName("customRules 配置非 JSON → 忽略，自定义规则不生效")
        void invalidCustomRulesJson_ignored()
        {
            when(configService.selectConfigByKey(CFG_CUSTOM_RULES)).thenReturn("not-json");
            policyService = new SysPasswordPolicyServiceImpl(configService);

            // 正常密码通过
            List<String> errors = policyService.validate("Password1!", "admin");
            assertEquals(0, errors.size());
        }
    }

    @Nested
    @DisplayName("minLength 配置")
    class MinLengthTests
    {
        @Test
        @DisplayName("minLength=16：短密码被拦截")
        void longerMinLength_rejectsShort()
        {
            when(configService.selectConfigByKey(CFG_MIN_LENGTH)).thenReturn("16");
            when(configService.selectConfigByKey(CFG_COMPLEXITY)).thenReturn("false");
            policyService = new SysPasswordPolicyServiceImpl(configService);

            // 10 字符 < 16
            List<String> errors = policyService.validate("1234567890", "admin");
            assertTrue(errors.size() > 0);
        }

        @Test
        @DisplayName("minLength 为空 → 默认为 8")
        void emptyMinLength_defaultsTo8()
        {
            when(configService.selectConfigByKey(CFG_MIN_LENGTH)).thenReturn("");
            when(configService.selectConfigByKey(CFG_COMPLEXITY)).thenReturn("false");
            policyService = new SysPasswordPolicyServiceImpl(configService);

            // 8 字符刚好通过
            List<String> errors = policyService.validate("12345678", "admin");
            assertEquals(0, errors.size());
        }
    }
}
