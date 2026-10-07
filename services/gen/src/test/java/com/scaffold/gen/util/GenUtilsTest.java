package com.scaffold.gen.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.scaffold.common.core.constant.GenConstants;
import com.scaffold.gen.domain.GenTable;
import com.scaffold.gen.domain.GenTableColumn;

/**
 * {@link GenUtils#initColumnField(GenTableColumn, GenTable)} 测试：
 * 列类型 → Java 类型/控件类型推导与 insert/edit/list/query 标记的核心规则。
 *
 * <p>用例按规则逐条断言（杀变异体：类型分支被改/标记被删即红）。</p>
 *
 * @author ct
 */
class GenUtilsTest
{
    private GenTable table()
    {
        GenTable table = new GenTable();
        table.setTableId(9L);
        table.setCreateBy("ct");
        return table;
    }

    private GenTableColumn col(String name, String columnType)
    {
        GenTableColumn c = new GenTableColumn();
        c.setColumnName(name);
        c.setColumnType(columnType);
        GenUtils.initColumnField(c, table());
        return c;
    }

    @Test
    @DisplayName("通用初始化：tableId/createBy 透传、javaField 驼峰、默认 String + EQ")
    void common_fieldsInitialized()
    {
        GenTableColumn c = col("user_age", "int(3)");
        assertEquals(9L, c.getTableId().longValue());
        assertEquals("ct", c.getCreateBy());
        assertEquals("userAge", c.getJavaField());
        assertEquals(GenConstants.QUERY_EQ, c.getQueryType());
        assertEquals(GenConstants.REQUIRE, c.getIsInsert());
    }

    @Test
    @DisplayName("字符串：短 varchar → input；超 500 → textarea")
    void string_columns()
    {
        assertEquals(GenConstants.HTML_INPUT, col("remark", "varchar(64)").getHtmlType());
        assertEquals(GenConstants.HTML_TEXTAREA, col("remark", "varchar(1000)").getHtmlType());
        assertEquals(GenConstants.TYPE_STRING, col("remark", "varchar(64)").getJavaType());
        // text 归 COLUMNTYPE_TEXT → textarea
        assertEquals(GenConstants.HTML_TEXTAREA, col("content_text", "text").getHtmlType());
    }

    @Test
    @DisplayName("时间：date/timestamp/datetime → Date + datetime 控件")
    void time_columns()
    {
        for (String t : new String[] {"date", "timestamp", "datetime"})
        {
            GenTableColumn c = col("create_time", t);
            assertEquals(GenConstants.TYPE_DATE, c.getJavaType(), t);
            assertEquals(GenConstants.HTML_DATETIME, c.getHtmlType(), t);
        }
    }

    @Test
    @DisplayName("数字：带标度 → BigDecimal；短整型 → Integer；其他 → Long")
    void number_columns()
    {
        assertEquals(GenConstants.TYPE_BIGDECIMAL, col("amount", "decimal(10,2)").getJavaType());
        assertEquals(GenConstants.TYPE_INTEGER, col("sort_no", "int(3)").getJavaType());
        assertEquals(GenConstants.TYPE_LONG, col("big_id", "bigint(20)").getJavaType());
        // 无精度裸类型 → Long（str == null 分支）
        assertEquals(GenConstants.TYPE_LONG, col("big_id", "bigint").getJavaType());
    }

    @Test
    @DisplayName("主键：isPk=1，且不参与 edit/list/query")
    void pk_column_flags()
    {
        GenTableColumn c = new GenTableColumn();
        c.setColumnName("user_id");
        c.setColumnType("bigint(20)");
        c.setIsPk("1");
        GenUtils.initColumnField(c, table());
        assertEquals(GenConstants.REQUIRE, c.getIsInsert());
        assertNull(c.getIsEdit());
        assertNull(c.getIsList());
        assertNull(c.getIsQuery());
    }

    @Test
    @DisplayName("排除列：create_by 等不参与 edit/list/query")
    void excluded_columns()
    {
        GenTableColumn c = col("create_by", "varchar(64)");
        assertNull(c.getIsEdit());
        assertNull(c.getIsList());
        assertNull(c.getIsQuery());
    }

    @Test
    @DisplayName("控件特化：name→LIKE 查询；status→radio；type/sex→select；image/file/content→上传/富文本")
    void htmlType_specializations()
    {
        GenTableColumn name = col("user_name", "varchar(30)");
        assertEquals(GenConstants.QUERY_LIKE, name.getQueryType());
        assertEquals(GenConstants.HTML_RADIO, col("del_status", "char(1)").getHtmlType());
        assertEquals(GenConstants.HTML_SELECT, col("biz_type", "varchar(32)").getHtmlType());
        assertEquals(GenConstants.HTML_SELECT, col("user_sex", "char(1)").getHtmlType());
        assertEquals(GenConstants.HTML_IMAGE_UPLOAD, col("avatar_image", "varchar(255)").getHtmlType());
        assertEquals(GenConstants.HTML_FILE_UPLOAD, col("attach_file", "varchar(255)").getHtmlType());
        assertEquals(GenConstants.HTML_EDITOR, col("rich_content", "varchar(2000)").getHtmlType());
    }
}
