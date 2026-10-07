package com.scaffold.message.service;

import com.scaffold.common.core.exception.ServiceException;
import com.scaffold.common.core.utils.StringUtils;
import com.scaffold.message.domain.MsgInnerTemplate;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 模板渲染（§4.4：占位符 ${key} 正则逐键替换；缺参策略 ERROR/KEEP/BLANK）
 *
 * @author ct
 */
public final class TemplateRenderer {
  private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([a-zA-Z0-9_]+)\\}");

  private TemplateRenderer() {}

  /** 渲染模板：短信/邮件固定 ERROR 策略（缺参抛错），站内信按模板配置 */
  public static String render(
      String template, Map<String, String> params, String missingParamMode) {
    if (template == null) {
      return "";
    }
    String mode =
        StringUtils.isEmpty(missingParamMode) ? MsgInnerTemplate.MISSING_ERROR : missingParamMode;
    Matcher matcher = PLACEHOLDER.matcher(template);
    StringBuilder result = new StringBuilder();
    while (matcher.find()) {
      String key = matcher.group(1);
      String value = params == null ? null : params.get(key);
      if (value != null) {
        matcher.appendReplacement(result, Matcher.quoteReplacement(value));
      } else if (MsgInnerTemplate.MISSING_KEEP.equals(mode)) {
        matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(0)));
      } else if (MsgInnerTemplate.MISSING_BLANK.equals(mode)) {
        matcher.appendReplacement(result, "");
      } else {
        throw new ServiceException("模板变量[" + key + "]缺失");
      }
    }
    matcher.appendTail(result);
    return result.toString();
  }

  /** 从模板内容解析变量名清单（逗号分隔，录入时生成 PARAM_NAMES） */
  public static String parseParamNames(String... templates) {
    StringBuilder names = new StringBuilder();
    for (String tpl : templates) {
      if (StringUtils.isEmpty(tpl)) {
        continue;
      }
      Matcher matcher = PLACEHOLDER.matcher(tpl);
      while (matcher.find()) {
        String name = matcher.group(1);
        if (names.indexOf(name) < 0) {
          if (names.length() > 0) {
            names.append(',');
          }
          names.append(name);
        }
      }
    }
    return names.toString();
  }
}
