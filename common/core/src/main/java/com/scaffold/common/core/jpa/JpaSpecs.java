package com.scaffold.common.core.jpa;

import java.util.Collection;
import java.util.Date;
import java.util.Map;

import org.springframework.data.jpa.domain.Specification;

import com.scaffold.common.core.utils.DateRanges;

/**
 * Specification 构造工具：把 mapper XML 时代 {@code <if test="...">} 的动态条件
 * 收敛为一行式组合，空值条件自动跳过（与原 XML 动态 where 语义一致）。
 * <p>
 * 用法（{@code .and()} 组合，null 条件被 Specification.and 自动忽略）：
 * {@code JpaSpecs.eqIf("status", user.getStatus()).and(JpaSpecs.likeIf("userName", ...))}
 * ——字段名为实体属性名（camelCase），列名转换交给 Hibernate 命名策略。
 */
public final class JpaSpecs {

    private JpaSpecs() {}

    /** 等值条件（value == null 时跳过） */
    public static Specification<Object> eqIf(String field, Object value) {
        return (value == null) ? alwaysTrue()
                : (root, query, cb) -> cb.equal(root.get(field), value);
    }

    /** 等值条件（字符串空串也跳过，对齐 XML 里 {@code != ''} 判断） */
    public static Specification<Object> eqIfNotBlank(String field, String value) {
        return (value == null || value.isEmpty()) ? alwaysTrue()
                : (root, query, cb) -> cb.equal(root.get(field), value);
    }

    /**
     * 包含模糊匹配（value 空白跳过；通配符转义；双侧 lower 大小写不敏感——
     * 对齐原 mapper XML 里 {@code lower(col) like lower(concat('%', #{x}, '%'))} 的既有语义）
     */
    public static Specification<Object> likeIf(String field, String value) {
        if (value == null || value.isBlank()) {
            return alwaysTrue();
        }
        String escaped = escapeLike(value.trim());
        return (root, query, cb) ->
                cb.like(cb.lower(root.get(field)), "%" + escaped.toLowerCase() + "%", '\\');
    }

    /** 不等条件（value == null 时跳过） */
    public static Specification<Object> neIf(String field, Object value) {
        return value == null ? alwaysTrue() : (root, query, cb) -> cb.notEqual(root.get(field), value);
    }

    /** 大于条件（value == null 时跳过） */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Specification<Object> gtIf(String field, Comparable value) {
        return value == null ? alwaysTrue() : (root, query, cb) -> cb.greaterThan(root.get(field), value);
    }

    /** 小于条件（value == null 时跳过） */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Specification<Object> ltIf(String field, Comparable value) {
        return value == null ? alwaysTrue() : (root, query, cb) -> cb.lessThan(root.get(field), value);
    }

    /** IN 条件（集合空跳过——对齐 XML {@code <if test="list != null and list.size() > 0">}） */
    public static Specification<Object> inIf(String field, Collection<?> values) {
        return (values == null || values.isEmpty()) ? alwaysTrue()
                : (root, query, cb) -> root.get(field).in(values);
    }

    /**
     * params.beginTime/endTime 日期区间条件（解析语义见 {@link com.scaffold.common.core.utils.DateRanges}），
     * 对齐 XML 里 {@code create_time >= #{params.beginTime}} 的动态过滤。
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Specification<Object> dateRangeIf(String field, Map<String, Object> params) {
        Date[] range = DateRanges.range(params);
        return betweenIf(field, range[0], range[1]);
    }

    /** 闭区间条件（任一端为 null 退化为单边比较；两端都 null 跳过） */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Specification<Object> betweenIf(String field, Comparable low, Comparable high) {
        if (low == null && high == null) {
            return alwaysTrue();
        }
        if (low == null) {
            return (root, query, cb) -> cb.lessThanOrEqualTo(root.get(field), (Comparable) high);
        }
        if (high == null) {
            return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get(field), (Comparable) low);
        }
        return (root, query, cb) -> cb.between(root.get(field), (Comparable) low, (Comparable) high);
    }

    /** 恒真条件（空条件占位，链式 .and() 安全） */
    public static Specification<Object> alwaysTrue() {
        return (root, query, cb) -> cb.conjunction();
    }

    /** like 通配符转义（\ % _） */
    private static String escapeLike(String value) {
        StringBuilder sb = new StringBuilder(value.length() + 8);
        for (char c : value.toCharArray()) {
            if (c == '\\' || c == '%' || c == '_') {
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.toString();
    }
}
