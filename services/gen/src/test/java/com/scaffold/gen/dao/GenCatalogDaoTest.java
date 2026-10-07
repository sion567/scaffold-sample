package com.scaffold.gen.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.h2.jdbcx.JdbcDataSource;
import org.h2.tools.RunScript;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import com.scaffold.gen.domain.GenTable;
import com.scaffold.gen.domain.GenTableColumn;

/**
 * gen 逆向目录查询测试（{@link GenCatalogDao}，替代原 GenTableDialectMapperTest）。
 *
 * <p>两个层面：</p>
 * <ol>
 *   <li>方言分支选择：按方言字符串直查 SQL 构建方法，断言命中对应方言的目录表/函数
 *       （oracle→user_tables、pg→pg_class、sqlserver→sys.tables、db2→syscat、
 *       mysql→DATE_FORMAT、H2/未识别→ANSI 兜底）；</li>
 *   <li>H2 真实执行：内存库跑测试 DDL + 建业务表后走 ANSI 兜底分支，逆向出的
 *       表/字段元数据与建表一致。</li>
 * </ol>
 *
 * <p>目录/系统表类语句无法在 H2 上执行（无 user_tables/pg_class/sys.* 元数据），
 * 各方言 SQL 的执行正确性在接入真实库时回归（docs/multi-database-guide.md §6）。</p>
 *
 * @author ct
 */
class GenCatalogDaoTest
{
    private static JdbcDataSource h2;

    @BeforeAll
    static void initH2() throws Exception
    {
        h2 = new JdbcDataSource();
        h2.setURL("jdbc:h2:mem:gen_catalog_test;MODE=Oracle;DB_CLOSE_DELAY=-1");
        h2.setUser("sa");
        h2.setPassword("");
        try (InputStream in = GenCatalogDaoTest.class.getClassLoader()
                .getResourceAsStream("sql/gen/gen_dialect_h2.sql"))
        {
            assertNotNull(in, "DDL 脚本不存在: sql/gen/gen_dialect_h2.sql");
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
                 var connection = h2.getConnection())
            {
                RunScript.execute(connection, reader);
            }
        }
    }

    // ===== 方言分支选择（SQL 构建断言，不执行） =====

    @Test
    @DisplayName("selectDbTableList：5 方言分支各自命中；h2/未识别库走 JDBC 元数据兜底")
    void tableList_branchSelection()
    {
        GenTable param = new GenTable();
        param.setTableName("x");
        param.getParams().put("beginTime", "20240101");
        param.getParams().put("endTime", "20240201");
        GenCatalogDao dao = new GenCatalogDao(new JdbcTemplate(h2), h2);

        assertTrue(dao.buildTableListSql("oracle", param, new ArrayList<>()).contains("user_tables"));
        assertTrue(dao.buildTableListSql("postgresql", param, new ArrayList<>()).contains("pg_class"));
        assertTrue(dao.buildTableListSql("sqlserver", param, new ArrayList<>()).contains("from sys.tables"));
        assertTrue(dao.buildTableListSql("db2", param, new ArrayList<>()).contains("syscat.tables"));
        assertTrue(dao.buildTableListSql("mysql", param, new ArrayList<>()).contains("DATE_FORMAT"));
        // h2/未识别库：buildTableListSql 不提供 SQL，queryTables 分发到 JDBC 元数据兜底
        // （原 ANSI 分支在 H2 2.x 不可执行），执行级验证见 reverseEngineer_onH2
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> dao.buildTableListSql("h2", param, new ArrayList<>()));
    }

    @Test
    @DisplayName("selectDbTableList：动态条件对齐原 <if>（like/时间过滤按需拼接，参数按序绑定）")
    void tableList_dynamicConditions()
    {
        GenCatalogDao dao = new GenCatalogDao(new JdbcTemplate(), h2);
        List<Object> args = new ArrayList<>();

        String bare = dao.buildTableListSql("mysql", new GenTable(), args);
        assertFalse(bare.contains("like lower('%' || ? || '%')"));
        assertTrue(args.isEmpty());

        GenTable param = new GenTable();
        param.setTableName("x");
        param.getParams().put("beginTime", "20240101");
        String one = dao.buildTableListSql("mysql", param, args);
        assertTrue(one.contains("like lower(concat(concat('%', ?), '%'))"));
        assertTrue(one.contains("DATE_FORMAT(create_time, '%Y%m%d') >= ?"));
        assertEquals(2, args.size());
        assertEquals("x", args.get(0));
        assertEquals("20240101", args.get(1));
    }

    @Test
    @DisplayName("selectDbTableColumnsByName：6 方言分支各自命中")
    void columns_branchSelection()
    {
        String[] ids = {"oracle", "postgresql", "sqlserver", "db2", "mysql"};
        String[] expects = {"user_tab_columns", "pg_attribute", "from sys.columns", "syscat.columns",
                "is_nullable = 'NO'"};
        GenCatalogDao dao = new GenCatalogDao(new JdbcTemplate(h2), h2);
        for (int i = 0; i < ids.length; i++)
        {
            assertTrue(dao.buildColumnsSql(ids[i]).contains(expects[i]),
                    "dialect=" + ids[i] + " 应命中 " + expects[i]);
        }
        // h2 走 JDBC 元数据兜底，执行级验证见 reverseEngineer_onH2
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> dao.buildColumnsSql("h2"));
    }

    @Test
    @DisplayName("按表名组查询 SQL：IN 占位与表数量一致")
    void tablesByNames_placeholders()
    {
        GenCatalogDao dao = new GenCatalogDao(new JdbcTemplate(), h2);
        String sql = dao.buildTablesByNamesSql("db2", 3);
        assertEquals(3, sql.split("\\?").length - 1);
    }

    // ===== H2 真实执行（ANSI 兜底分支） =====

    @Test
    @DisplayName("H2 真实执行：逆向出的表与字段元数据和建表一致")
    void reverseEngineer_onH2()
    {
        JdbcTemplate jdbc = new JdbcTemplate(h2);
        jdbc.execute("CREATE TABLE reverse_probe_tbl ("
                + "id BIGINT NOT NULL PRIMARY KEY,"
                + "user_name VARCHAR2(50) NOT NULL,"
                + "created_at TIMESTAMP)");
        jdbc.execute("INSERT INTO gen_table(table_id, table_name, table_comment, class_name, package_name,"
                + " module_name, business_name, function_name, function_author, gen_type, gen_path)"
                + " VALUES (900, 'reverse_probe_tbl', '逆向探针表', 'ReverseProbeTbl', 'com.demo',"
                + " 'demo', 'probe', '探针', 'ct', '0', '/')");
        try
        {
            GenCatalogDao dao = new GenCatalogDao(jdbc, h2);
            GenTable query = new GenTable();
            query.setTableName("reverse_probe");
            List<GenTable> tables = dao.selectDbTableList(query);
            assertEquals(1, tables.size());
            assertEquals("REVERSE_PROBE_TBL", tables.get(0).getTableName());
            // 未识别库同理走兜底
            assertEquals(1, dao.queryTables(null, query).size());

            List<GenTableColumn> columns = dao.selectDbTableColumnsByName("REVERSE_PROBE_TBL");
            assertEquals(3, columns.size());
            GenTableColumn pk = columns.stream().filter(c -> "1".equals(c.getIsPk())).findFirst().orElseThrow();
            assertEquals("ID", pk.getColumnName());
            GenTableColumn name = columns.stream().filter(c -> "USER_NAME".equals(c.getColumnName())).findFirst().orElseThrow();
            assertEquals("1", name.getIsRequired());
            assertTrue(columns.get(0).getSort() <= columns.get(columns.size() - 1).getSort());

            List<GenTable> byNames = dao.selectDbTableListByNames(new String[] {"REVERSE_PROBE_TBL"});
            assertEquals(1, byNames.size());
        }
        finally
        {
            jdbc.execute("DELETE FROM gen_table WHERE table_id = 900");
            jdbc.execute("DROP TABLE reverse_probe_tbl");
        }
    }
}
