package com.scaffold.system.domain.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * String 属性 ↔ INT 列 转换（sys_menu.is_frame/is_cache 等 RuoYi 遗留的
 * "数字列存字符串" 映射：0/1 与 "0"/"1" 互转，空串/非数字安全回落 null）。
 *
 * @author scaffold
 */
@Converter
public class IntegerStringConverter implements AttributeConverter<String, Integer> {

    @Override
    public Integer convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(attribute.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("非数字值无法写入 INT 列: " + attribute, e);
        }
    }

    @Override
    public String convertToEntityAttribute(Integer dbData) {
        return dbData == null ? null : String.valueOf(dbData);
    }
}
