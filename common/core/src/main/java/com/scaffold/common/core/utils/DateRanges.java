package com.scaffold.common.core.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;

/**
 * 列表页日期区间参数解析（params.beginTime/endTime 字符串 → Date）。
 *
 * <p>承接原 common/mybatis DateRangeParamInterceptor 的解析语义：
 * {@code yyyy-MM-dd HH:mm:ss} 全量、{@code yyyy-MM-dd}（endTime 补到 23:59:59.999）、
 * {@code yyyyMMdd} 紧凑格式；非法值忽略（不参与过滤）。</p>
 *
 * @author ct
 */
public class DateRanges
{
    private static final DateTimeFormatter F_DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final DateTimeFormatter F_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final DateTimeFormatter F_COMPACT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private DateRanges()
    {
    }

    /** beginTime → Date（当天 00:00:00；解析失败返回 null） */
    public static Date parseBegin(Object value)
    {
        LocalDateTime parsed = parse(value, false);
        return parsed == null ? null : Date.from(parsed.atZone(java.time.ZoneId.systemDefault()).toInstant());
    }

    /** endTime → Date（仅日期时补到当天 23:59:59.999；解析失败返回 null） */
    public static Date parseEnd(Object value)
    {
        LocalDateTime parsed = parse(value, true);
        return parsed == null ? null : Date.from(parsed.atZone(java.time.ZoneId.systemDefault()).toInstant());
    }

    /** 从查询参数 map 取区间（beginTime/endTime），任一端有效即返回
     * @return [begin, end]，元素可为 null */
    public static Date[] range(Map<String, Object> params)
    {
        if (params == null)
        {
            return new Date[] { null, null };
        }
        return new Date[] { parseBegin(params.get("beginTime")), parseEnd(params.get("endTime")) };
    }

    private static LocalDateTime parse(Object value, boolean endOfDay)
    {
        if (!(value instanceof String))
        {
            return null;
        }
        String trimmed = ((String) value).trim();
        if (trimmed.isEmpty())
        {
            return null;
        }
        try
        {
            if (trimmed.length() > 10)
            {
                return LocalDateTime.parse(trimmed, F_DATETIME);
            }
            LocalDate date;
            if (trimmed.contains("-"))
            {
                date = LocalDate.parse(trimmed, F_DATE);
            }
            else
            {
                date = LocalDate.parse(trimmed, F_COMPACT);
            }
            return endOfDay ? date.atTime(23, 59, 59, 999_999_999) : date.atStartOfDay();
        }
        catch (Exception e)
        {
            return null;
        }
    }
}
