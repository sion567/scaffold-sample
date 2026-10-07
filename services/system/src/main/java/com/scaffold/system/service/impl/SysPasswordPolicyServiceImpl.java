package com.scaffold.system.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.alibaba.fastjson2.JSON;
import com.scaffold.common.core.text.Convert;
import com.scaffold.common.core.utils.StringUtils;
import com.scaffold.system.service.ISysConfigService;
import com.scaffold.system.service.ISysPasswordPolicyService;
import org.passay.DefaultPasswordValidator;
import org.passay.PasswordData;
import org.passay.PasswordValidator;
import org.passay.ValidationResult;
import org.passay.data.EnglishCharacterData;
import org.passay.data.EnglishSequenceData;
import org.passay.resolver.MessageResolver;
import org.passay.resolver.PropertiesMessageResolver;
import org.passay.rule.CharacterCharacteristicsRule;
import org.passay.rule.CharacterRule;
import org.passay.rule.IllegalSequenceRule;
import org.passay.rule.LengthRule;
import org.passay.rule.RepeatCharactersRule;
import org.passay.rule.Rule;
import org.passay.rule.UsernameRule;
import org.passay.rule.WhitespaceRule;

/**
 * 密码策略校验 服务层实现
 * <p>
 * 通过 sys.account.password.* 配置动态构建 Passay 规则：
 * <ul>
 *   <li>minLength：密码最小长度（LengthRule）</li>
 *   <li>complexity：大小写字母 + 数字 + 特殊字符四类全包含（CharacterCharacteristicsRule）</li>
 *   <li>customRules：JSON 数组自定义启用规则，支持枚举：<br>
 *       WHITESPACE（禁止空格）、ALPHABETICAL_SEQUENCE（禁止连续字母）、
 *       NUMERICAL_SEQUENCE（禁止连续数字）、QWERTY_SEQUENCE（禁止键盘连续序列）、
 *       USERNAME（禁止包含用户名）、REPEAT_CHARACTERS（禁止重复字符）</li>
 * </ul>
 *
 * @author ct
 */
@Service
public class SysPasswordPolicyServiceImpl implements ISysPasswordPolicyService
{
    private static final Logger log = LoggerFactory.getLogger(SysPasswordPolicyServiceImpl.class);

    /** 密码最小长度配置键 */
    private static final String CONFIG_MIN_LENGTH = "sys.account.password.minLength";

    /** 密码复杂度配置键 */
    private static final String CONFIG_COMPLEXITY = "sys.account.password.complexity";

    /** 自定义规则配置键（JSON 数组） */
    private static final String CONFIG_CUSTOM_RULES = "sys.account.password.customRules";

    /** 密码最大长度（字符数，Passay LengthRule 上限；UTF-8 字节上限 72 由 validate 单独检查） */
    private static final int MAX_PASSWORD_LENGTH = 72;

    /** 密码 UTF-8 字节上限（沿用原 BCrypt 72 字节约束作为策略上限；SM3 本身无长度硬限制） */
    private static final int MAX_PASSWORD_BYTES = 72;

    /** 自定义规则枚举常量 */
    private static final String RULE_WHITESPACE = "WHITESPACE";
    private static final String RULE_ALPHABETICAL_SEQUENCE = "ALPHABETICAL_SEQUENCE";
    private static final String RULE_NUMERICAL_SEQUENCE = "NUMERICAL_SEQUENCE";
    private static final String RULE_QWERTY_SEQUENCE = "QWERTY_SEQUENCE";
    private static final String RULE_USERNAME = "USERNAME";
    private static final String RULE_REPEAT_CHARACTERS = "REPEAT_CHARACTERS";

    /** 中文错误消息解析器（classpath:passay-messages.properties） */
    private final MessageResolver messageResolver = createMessageResolver();
    private final ISysConfigService configService;

    public SysPasswordPolicyServiceImpl(ISysConfigService configService)
    {
        this.configService = configService;
    }

    @Override
    public List<String> validate(String rawPassword, String username)
    {
        if (StringUtils.isEmpty(rawPassword))
        {
            return Collections.singletonList("密码不能为空");
        }
        // 密码字节上限沿用原 BCrypt 72 字节策略约束，多字节字符（如中文）按 UTF-8 字节数校验
        if (rawPassword.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES)
        {
            return Collections.singletonList("密码过长，UTF-8 编码不能超过 " + MAX_PASSWORD_BYTES + " 字节");
        }
        int minLength = Convert.toInt(configService.selectConfigByKey(CONFIG_MIN_LENGTH), 8);
        boolean complexity = Convert.toBool(configService.selectConfigByKey(CONFIG_COMPLEXITY), true);
        List<Rule> rules = buildRules(minLength, complexity);
        PasswordValidator validator = new DefaultPasswordValidator(messageResolver, rules);
        PasswordData passwordData = new PasswordData(StringUtils.isEmpty(username) ? "" : username, rawPassword);
        ValidationResult result = validator.validate(passwordData);
        return result.isValid() ? Collections.emptyList() : result.getMessages();
    }

    /**
     * 根据配置构建 Passay 规则列表
     */
    private List<Rule> buildRules(int minLength, boolean complexity)
    {
        List<Rule> rules = new ArrayList<>();
        int min = Math.max(minLength, 0);
        rules.add(new LengthRule(min, Math.max(MAX_PASSWORD_LENGTH, min)));
        if (complexity)
        {
            CharacterRule upper = new CharacterRule(EnglishCharacterData.UpperCase, 1);
            CharacterRule lower = new CharacterRule(EnglishCharacterData.LowerCase, 1);
            CharacterRule digit = new CharacterRule(EnglishCharacterData.Digit, 1);
            CharacterRule special = new CharacterRule(EnglishCharacterData.Special, 1);
            // 大小写字母 + 数字 + 特殊字符 四类全部满足
            rules.add(new CharacterCharacteristicsRule(true, false, 4, upper, lower, digit, special));
        }
        for (String ruleCode : getCustomRules())
        {
            switch (ruleCode)
            {
                case RULE_WHITESPACE:
                    rules.add(new WhitespaceRule());
                    break;
                case RULE_ALPHABETICAL_SEQUENCE:
                    rules.add(new IllegalSequenceRule(EnglishSequenceData.Alphabetical, 5));
                    break;
                case RULE_NUMERICAL_SEQUENCE:
                    rules.add(new IllegalSequenceRule(EnglishSequenceData.Numerical, 5));
                    break;
                case RULE_QWERTY_SEQUENCE:
                    rules.add(new IllegalSequenceRule(EnglishSequenceData.USQwerty, 5));
                    break;
                case RULE_USERNAME:
                    rules.add(new UsernameRule());
                    break;
                case RULE_REPEAT_CHARACTERS:
                    rules.add(new RepeatCharactersRule());
                    break;
                default:
                    log.warn("密码自定义规则未识别，已忽略: {}", ruleCode);
            }
        }
        return rules;
    }

    /**
     * 读取 sys.account.password.customRules（JSON 数组）并解析为规则编码列表
     */
    private List<String> getCustomRules()
    {
        String configValue = configService.selectConfigByKey(CONFIG_CUSTOM_RULES);
        if (StringUtils.isEmpty(configValue))
        {
            return Collections.emptyList();
        }
        try
        {
            return JSON.parseArray(configValue, String.class);
        }
        catch (Exception e)
        {
            log.warn("sys.account.password.customRules 配置格式错误，已忽略自定义规则: {}", configValue, e);
            return Collections.emptyList();
        }
    }

    private static MessageResolver createMessageResolver()
    {
        Properties props = new Properties();
        try (InputStream in = SysPasswordPolicyServiceImpl.class.getResourceAsStream("/passay-messages.properties"))
        {
            if (in != null)
            {
                props.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            }
            else
            {
                log.warn("未找到 passay-messages.properties，密码策略错误消息将使用默认英文");
            }
        }
        catch (IOException e)
        {
            log.warn("加载 passay-messages.properties 失败，密码策略错误消息将使用默认英文", e);
        }
        return new PropertiesMessageResolver(props, Locale.CHINA);
    }
}
