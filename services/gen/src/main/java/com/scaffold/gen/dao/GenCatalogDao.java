package com.scaffold.gen.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.scaffold.common.core.jpa.DatabaseDialects;
import com.scaffold.gen.domain.GenTable;
import com.scaffold.gen.domain.GenTableColumn;

/**
 * 数据库目录（逆向工程）查询。
 *
 * <p>information_schema / user_tables / pg_class / sys.tables / syscat.tables 等元数据
 * 目录 SQL 无法用 ORM 表达，本 DAO 用 {@link JdbcTemplate} 承载，SQL 文本自原
 * GenTableMapper.xml / GenTableColumnMapper.xml 按 6 方言分支原样搬运（迁移不重写语义），
 * 方言由 {@link DatabaseDialects#resolve(DataSource)} 按数据源产品名解析后分发——
 * 替代原 MyBatis databaseId（_databaseId）机制。所有取值一律走绑定参数，无拼接注入面。</p>
 *
 * @author ct
 */
@Repository
public class GenCatalogDao
{
    private final JdbcTemplate jdbcTemplate;

    private final DataSource dataSource;

    public GenCatalogDao(JdbcTemplate jdbcTemplate, DataSource dataSource)
    {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
    }

    /** 当前方言（低频操作，借用池化连接读取产品名） */
    private String dialect()
    {
        return DatabaseDialects.resolve(dataSource);
    }

    // ==================== 逆向查询：数据库表列表 ====================

    private static final String TABLES_MYSQL =
            "select table_name, table_comment, create_time, update_time from information_schema.tables"
            + " where table_schema = (select database())"
            + " AND table_name NOT LIKE 'qrtz\\_%' AND table_name NOT LIKE 'gen\\_%'"
            + " AND table_name NOT IN (select table_name from gen_table)";

    private static final String TABLES_ORACLE =
            "select ut.table_name, coalesce(utc.comments, '') as table_comment, ut.created as create_time, ut.last_ddl_time as update_time"
            + " from user_tables ut"
            + " left join user_tab_comments utc on utc.table_name = ut.table_name"
            + " where ut.table_name not like 'qrtz\\_%' escape '\\'"
            + " and ut.table_name not like 'gen\\_%' escape '\\'"
            + " and ut.table_name not in (select table_name from gen_table)";

    private static final String TABLES_POSTGRESQL =
            "select c.relname as table_name, coalesce(obj_description(c.oid, 'pg_class'), '') as table_comment,"
            + " null as create_time, null as update_time"
            + " from pg_class c"
            + " join pg_namespace n on n.oid = c.relnamespace"
            + " where n.nspname = current_schema()"
            + " and c.relkind in ('r', 'p')"
            + " and c.relname not like 'qrtz\\_%' and c.relname not like 'gen\\_%'"
            + " and c.relname not in (select table_name from gen_table)";

    private static final String TABLES_SQLSERVER =
            "select t.name as table_name, coalesce(cast(ep.value as nvarchar(500)), '') as table_comment,"
            + " t.create_date as create_time, t.modify_date as update_time"
            + " from sys.tables t"
            + " left join sys.extended_properties ep on ep.major_id = t.object_id and ep.minor_id = 0 and ep.name = 'MS_Description'"
            + " where t.name not like 'qrtz\\_%' escape '\\' and t.name not like 'gen\\_%' escape '\\'"
            + " and t.name not in (select table_name from gen_table)";

    private static final String TABLES_DB2 =
            "select t.tabname as table_name, coalesce(t.remarks, '') as table_comment, t.create_time, t.alter_time as update_time"
            + " from syscat.tables t"
            + " where t.type = 'T' and t.tabschema = current schema"
            + " and t.tabname not like 'qrtz\\_%' escape '\\' and t.tabname not like 'gen\\_%' escape '\\'"
            + " and t.tabname not in (select table_name from gen_table)";

    /** 查询数据库表列表（按当前方言分发，动态条件对齐原 <if> 分支） */
    public List<GenTable> selectDbTableList(GenTable genTable)
    {
        return queryTables(dialect(), genTable);
    }

    /** 方言分发实现（包内可见，供方言分支单测直接指定方言） */
    List<GenTable> queryTables(String d, GenTable genTable)
    {
        if (d == null || DatabaseDialects.H2.equals(d))
        {
            return selectTablesViaMetadata(genTable);
        }
        List<Object> args = new ArrayList<>();
        String sql = buildTableListSql(d, genTable, args);
        return jdbcTemplate.query(sql, GenCatalogDao::mapTable, args.toArray());
    }

    /** 表列表 SQL 构建（包内可见：方言分支单测断言 SQL 文本；条件参数按序填入 args） */
    String buildTableListSql(String d, GenTable genTable, List<Object> args)
    {
        StringBuilder sql = new StringBuilder();

        switch (d == null ? "" : d)
        {
            case DatabaseDialects.MYSQL:
            {
                sql.append(TABLES_MYSQL);
                if (notBlank(genTable.getTableName()))
                {
                    sql.append(" AND lower(table_name) like lower(concat(concat('%', ?), '%'))");
                    args.add(genTable.getTableName());
                }
                if (notBlank(genTable.getTableComment()))
                {
                    sql.append(" AND lower(table_comment) like lower(concat(concat('%', ?), '%'))");
                    args.add(genTable.getTableComment());
                }
                if (notBlank(beginTime(genTable)))
                {
                    sql.append(" AND DATE_FORMAT(create_time, '%Y%m%d') >= ?");
                    args.add(beginTime(genTable));
                }
                if (notBlank(endTime(genTable)))
                {
                    sql.append(" AND DATE_FORMAT(create_time, '%Y%m%d') <= ?");
                    args.add(endTime(genTable));
                }
                sql.append(" order by create_time desc");
                break;
            }
            case DatabaseDialects.ORACLE:
            {
                sql.append(TABLES_ORACLE);
                if (notBlank(genTable.getTableName()))
                {
                    sql.append(" AND lower(ut.table_name) like lower(concat(concat('%', ?), '%'))");
                    args.add(genTable.getTableName());
                }
                if (notBlank(genTable.getTableComment()))
                {
                    sql.append(" AND lower(coalesce(utc.comments, '')) like lower(concat(concat('%', ?), '%'))");
                    args.add(genTable.getTableComment());
                }
                if (notBlank(beginTime(genTable)))
                {
                    sql.append(" AND TO_CHAR(ut.created, 'YYYYMMDD') >= ?");
                    args.add(beginTime(genTable));
                }
                if (notBlank(endTime(genTable)))
                {
                    sql.append(" AND TO_CHAR(ut.created, 'YYYYMMDD') <= ?");
                    args.add(endTime(genTable));
                }
                sql.append(" order by ut.created desc");
                break;
            }
            case DatabaseDialects.POSTGRESQL:
            {
                sql.append(TABLES_POSTGRESQL);
                if (notBlank(genTable.getTableName()))
                {
                    sql.append(" AND lower(c.relname) like lower(concat(concat('%', ?), '%'))");
                    args.add(genTable.getTableName());
                }
                if (notBlank(genTable.getTableComment()))
                {
                    sql.append(" AND lower(coalesce(obj_description(c.oid, 'pg_class'), '')) like lower(concat(concat('%', ?), '%'))");
                    args.add(genTable.getTableComment());
                }
                sql.append(" order by c.relname");
                break;
            }
            case DatabaseDialects.SQLSERVER:
            {
                sql.append(TABLES_SQLSERVER);
                if (notBlank(genTable.getTableName()))
                {
                    sql.append(" AND lower(t.name) like lower(concat(concat('%', ?), '%'))");
                    args.add(genTable.getTableName());
                }
                if (notBlank(genTable.getTableComment()))
                {
                    sql.append(" AND lower(coalesce(cast(ep.value as nvarchar(500)), '')) like lower(concat(concat('%', ?), '%'))");
                    args.add(genTable.getTableComment());
                }
                if (notBlank(beginTime(genTable)))
                {
                    sql.append(" AND CONVERT(varchar(8), t.create_date, 112) >= ?");
                    args.add(beginTime(genTable));
                }
                if (notBlank(endTime(genTable)))
                {
                    sql.append(" AND CONVERT(varchar(8), t.create_date, 112) <= ?");
                    args.add(endTime(genTable));
                }
                sql.append(" order by t.create_date desc");
                break;
            }
            case DatabaseDialects.DB2:
            {
                sql.append(TABLES_DB2);
                if (notBlank(genTable.getTableName()))
                {
                    sql.append(" AND lower(t.tabname) like lower(concat(concat('%', ?), '%'))");
                    args.add(genTable.getTableName());
                }
                if (notBlank(genTable.getTableComment()))
                {
                    sql.append(" AND lower(coalesce(t.remarks, '')) like lower(concat(concat('%', ?), '%'))");
                    args.add(genTable.getTableComment());
                }
                if (notBlank(beginTime(genTable)))
                {
                    sql.append(" AND VARCHAR_FORMAT(t.create_time, 'YYYYMMDD') >= ?");
                    args.add(beginTime(genTable));
                }
                if (notBlank(endTime(genTable)))
                {
                    sql.append(" AND VARCHAR_FORMAT(t.create_time, 'YYYYMMDD') <= ?");
                    args.add(endTime(genTable));
                }
                sql.append(" order by t.create_time desc");
                break;
            }
            default:
                // h2/未识别库走 JDBC 元数据兜底（queryTables 分发），不构建 SQL
                throw new IllegalStateException("方言 " + d + " 无目录 SQL（走元数据兜底）");
        }
        return sql.toString();
    }

    /** 按表名组查询数据库表（导入用） */
    public List<GenTable> selectDbTableListByNames(String[] tableNames)
    {
        return queryTablesByNames(dialect(), tableNames);
    }

    /** 方言分发实现（包内可见，供方言分支单测直接指定方言） */
    List<GenTable> queryTablesByNames(String d, String[] tableNames)
    {
        if (d == null || DatabaseDialects.H2.equals(d))
        {
            return selectTablesByNamesViaMetadata(tableNames);
        }
        return jdbcTemplate.query(buildTablesByNamesSql(d, tableNames.length),
                GenCatalogDao::mapTable, (Object[]) tableNames);
    }

    /** 按名组表查询 SQL 构建（包内可见；IN 占位与表数量一致） */
    String buildTablesByNamesSql(String d, int nameCount)
    {
        String placeholders = String.join(", ", java.util.Collections.nCopies(nameCount, "?"));
        String sql;
        switch (d == null ? "" : d)
        {
            case DatabaseDialects.MYSQL:
                sql = "select table_name, table_comment, create_time, update_time from information_schema.tables"
                        + " where table_name NOT LIKE 'qrtz\\_%' and table_name NOT LIKE 'gen\\_%' and table_schema = (select database())"
                        + " and table_name in (" + placeholders + ")";
                break;
            case DatabaseDialects.ORACLE:
                sql = "select ut.table_name, coalesce(utc.comments, '') as table_comment, ut.created as create_time, ut.last_ddl_time as update_time"
                        + " from user_tables ut"
                        + " left join user_tab_comments utc on utc.table_name = ut.table_name"
                        + " where ut.table_name not like 'qrtz\\_%' escape ''"
                        + " and ut.table_name not like 'gen\\_%' escape ''"
                        + " and ut.table_name in (" + placeholders + ")";
                break;
            case DatabaseDialects.POSTGRESQL:
                sql = "select c.relname as table_name, coalesce(obj_description(c.oid, 'pg_class'), '') as table_comment,"
                        + " null as create_time, null as update_time"
                        + " from pg_class c"
                        + " join pg_namespace n on n.oid = c.relnamespace"
                        + " where n.nspname = current_schema()"
                        + " and c.relkind in ('r', 'p')"
                        + " and c.relname not like 'qrtz\\_%' and c.relname not like 'gen\\_%'"
                        + " and c.relname in (" + placeholders + ")";
                break;
            case DatabaseDialects.SQLSERVER:
                sql = "select t.name as table_name, coalesce(cast(ep.value as nvarchar(500)), '') as table_comment,"
                        + " t.create_date as create_time, t.modify_date as update_time"
                        + " from sys.tables t"
                        + " left join sys.extended_properties ep on ep.major_id = t.object_id and ep.minor_id = 0 and ep.name = 'MS_Description'"
                        + " where t.name not like 'qrtz\\_%' escape '' and t.name not like 'gen\\_%' escape ''"
                        + " and t.name in (" + placeholders + ")";
                break;
            case DatabaseDialects.DB2:
                sql = "select t.tabname as table_name, coalesce(t.remarks, '') as table_comment, t.create_time, t.alter_time as update_time"
                        + " from syscat.tables t"
                        + " where t.type = 'T' and t.tabschema = current schema"
                        + " and t.tabname not like 'qrtz\\_%' escape '' and t.tabname not like 'gen\\_%' escape ''"
                        + " and t.tabname in (" + placeholders + ")";
                break;
            default:
                throw new IllegalStateException("方言 " + d + " 无目录 SQL（走元数据兜底）");
        }
        return sql;
    }

    /** 按表名查询字段元数据（同步/导入用） */
    public List<GenTableColumn> selectDbTableColumnsByName(String tableName)
    {
        return queryColumns(dialect(), tableName);
    }

    /** 方言分发实现（包内可见，供方言分支单测直接指定方言） */
    List<GenTableColumn> queryColumns(String d, String tableName)
    {
        if (d == null || DatabaseDialects.H2.equals(d))
        {
            return selectColumnsViaMetadata(tableName);
        }
        return jdbcTemplate.query(buildColumnsSql(d), GenCatalogDao::mapColumn, tableName);
    }

    /** 字段元数据 SQL 构建（包内可见：方言分支单测断言 SQL 文本） */
    String buildColumnsSql(String d)
    {
        String sql;
        switch (d == null ? "" : d)
        {
            case DatabaseDialects.MYSQL:
                sql = "select column_name, (case when (is_nullable = 'NO' && column_key != 'PRI') then '1' else null end) as is_required,"
                        + " (case when column_key = 'PRI' then '1' else '0' end) as is_pk, ordinal_position as sort, column_comment,"
                        + " (case when extra = 'auto_increment' then '1' else '0' end) as is_increment, column_type"
                        + " from information_schema.columns where table_schema = (select database()) and table_name = (?)"
                        + " order by ordinal_position";
                break;
            case DatabaseDialects.ORACLE:
                sql = COLUMNS_ORACLE;
                break;
            case DatabaseDialects.POSTGRESQL:
                sql = COLUMNS_POSTGRESQL;
                break;
            case DatabaseDialects.SQLSERVER:
                sql = COLUMNS_SQLSERVER;
                break;
            case DatabaseDialects.DB2:
                sql = COLUMNS_DB2;
                break;
            default:
                throw new IllegalStateException("方言 " + d + " 无目录 SQL（走元数据兜底）");
        }
        return sql;
    }

    // ==================== JDBC 元数据兜底（h2 / 未识别库） ====================

    /** 排除的前缀（与各方言分支的 NOT LIKE 'qrtz_%' / 'gen_%' 一致） */
    private static final String[] EXCLUDED_PREFIXES = {"qrtz_", "gen_"};

    private List<GenTable> selectTablesViaMetadata(GenTable genTable)
    {
        java.util.Set<String> imported = new java.util.HashSet<>(
                jdbcTemplate.queryForList("select table_name from gen_table", String.class));
        List<GenTable> result = new ArrayList<>();
        try (java.sql.Connection con = dataSource.getConnection())
        {
            try (java.sql.ResultSet rs = con.getMetaData().getTables(
                    null, con.getSchema(), "%", new String[] {"TABLE"}))
            {
                while (rs.next())
                {
                    String name = rs.getString("TABLE_NAME");
                    if (isExcluded(name) || imported.contains(name))
                    {
                        continue;
                    }
                    String comment = rs.getString("REMARKS");
                    if (notBlank(genTable.getTableName())
                            && !name.toLowerCase().contains(genTable.getTableName().toLowerCase()))
                    {
                        continue;
                    }
                    if (notBlank(genTable.getTableComment())
                            && (comment == null
                                    || !comment.toLowerCase().contains(genTable.getTableComment().toLowerCase())))
                    {
                        continue;
                    }
                    GenTable t = new GenTable();
                    t.setTableName(name);
                    t.setTableComment(comment);
                    result.add(t);
                }
            }
        }
        catch (java.sql.SQLException e)
        {
            throw new com.scaffold.common.core.exception.ServiceException("读取数据库表元数据失败：" + e.getMessage());
        }
        result.sort(java.util.Comparator.comparing(GenTable::getTableName));
        return result;
    }

    private List<GenTable> selectTablesByNamesViaMetadata(String[] tableNames)
    {
        java.util.Set<String> wanted = new java.util.HashSet<>(java.util.Arrays.asList(tableNames));
        List<GenTable> result = new ArrayList<>();
        try (java.sql.Connection con = dataSource.getConnection())
        {
            try (java.sql.ResultSet rs = con.getMetaData().getTables(
                    null, con.getSchema(), "%", new String[] {"TABLE"}))
            {
                while (rs.next())
                {
                    String name = rs.getString("TABLE_NAME");
                    if (isExcluded(name) || !wanted.contains(name))
                    {
                        continue;
                    }
                    GenTable t = new GenTable();
                    t.setTableName(name);
                    t.setTableComment(rs.getString("REMARKS"));
                    result.add(t);
                }
            }
        }
        catch (java.sql.SQLException e)
        {
            throw new com.scaffold.common.core.exception.ServiceException("读取数据库表元数据失败：" + e.getMessage());
        }
        return result;
    }

    private List<GenTableColumn> selectColumnsViaMetadata(String tableName)
    {
        List<GenTableColumn> result = new ArrayList<>();
        try (java.sql.Connection con = dataSource.getConnection())
        {
            java.util.Set<String> pkColumns = new java.util.HashSet<>();
            try (java.sql.ResultSet rs = con.getMetaData().getPrimaryKeys(null, con.getSchema(), tableName))
            {
                while (rs.next())
                {
                    pkColumns.add(rs.getString("COLUMN_NAME"));
                }
            }
            int sort = 1;
            try (java.sql.ResultSet rs = con.getMetaData().getColumns(null, con.getSchema(), tableName, "%"))
            {
                while (rs.next())
                {
                    GenTableColumn c = new GenTableColumn();
                    c.setColumnName(rs.getString("COLUMN_NAME"));
                    boolean required = "NO".equalsIgnoreCase(rs.getString("IS_NULLABLE"));
                    c.setIsRequired(required && !pkColumns.contains(c.getColumnName()) ? "1" : null);
                    c.setIsPk(pkColumns.contains(c.getColumnName()) ? "1" : "0");
                    c.setSort(sort++);
                    c.setColumnComment(rs.getString("REMARKS"));
                    c.setIsIncrement("YES".equalsIgnoreCase(rs.getString("IS_AUTOINCREMENT")) ? "1" : "0");
                    c.setColumnType(toColumnType(rs));
                    result.add(c);
                }
            }
        }
        catch (java.sql.SQLException e)
        {
            throw new com.scaffold.common.core.exception.ServiceException("读取数据库字段元数据失败：" + e.getMessage());
        }
        return result;
    }

    private static boolean isExcluded(String tableName)
    {
        if (tableName == null)
        {
            return true;
        }
        String lower = tableName.toLowerCase();
        for (String prefix : EXCLUDED_PREFIXES)
        {
            if (lower.startsWith(prefix))
            {
                return true;
            }
        }
        return false;
    }

    /** 列类型串重建（对齐各方言分支的 column_type 口径：varchar(n)/char(n)/decimal(p,s)/bigint/datetime/text...） */
    private static String toColumnType(java.sql.ResultSet rs) throws java.sql.SQLException
    {
        String typeName = rs.getString("TYPE_NAME").toLowerCase();
        int size = rs.getInt("COLUMN_SIZE");
        int digits = rs.getInt("DECIMAL_DIGITS");
        switch (typeName)
        {
            case "character varying":
            case "varchar":
                return "varchar(" + size + ")";
            case "character":
            case "char":
                return "char(" + size + ")";
            case "numeric":
            case "decimal":
                return "decimal(" + size + "," + digits + ")";
            case "integer":
                return "int";
            case "smallint":
                return "smallint";
            case "tinyint":
                return "tinyint";
            case "bigint":
                return "bigint";
            case "real":
                return "float";
            case "double precision":
            case "double":
                return "double";
            case "timestamp":
            case "datetime":
                return "datetime";
            case "clob":
            case "character large object":
                return "text";
            case "blob":
            case "binary varying":
                return "blob";
            default:
                return typeName;
        }
    }

    // ==================== 字段元数据 SQL（各库目录查询，原样搬运） ====================

    private static final String COLUMNS_ORACLE =
            "select c.column_name,"
            + " (case when c.nullable = 'N' and pk.column_name is null then '1' else null end) as is_required,"
            + " (case when pk.column_name is not null then '1' else '0' end) as is_pk,"
            + " c.column_id as sort,"
            + " coalesce(cc.comments, '') as column_comment,"
            + " (case when c.identity_column = 'YES' then '1' else '0' end) as is_increment,"
            + " lower(c.data_type) ||"
            + " (case"
            + " when c.data_type in ('CHAR', 'NCHAR', 'VARCHAR2', 'NVARCHAR2', 'RAW') then '(' || c.data_length || ')'"
            + " when c.data_type = 'NUMBER' and c.data_precision is not null then"
            + " '(' || c.data_precision || (case when c.data_scale > 0 then ',' || c.data_scale else '' end) || ')'"
            + " else ''"
            + " end) as column_type"
            + " from user_tab_columns c"
            + " left join user_col_comments cc on cc.table_name = c.table_name and cc.column_name = c.column_name"
            + " left join (select cons.table_name, cols.column_name"
            + " from user_constraints cons"
            + " join user_cons_columns cols on cols.constraint_name = cons.constraint_name"
            + " where cons.constraint_type = 'P') pk"
            + " on pk.table_name = c.table_name and pk.column_name = c.column_name"
            + " where c.table_name = upper(?)"
            + " order by c.column_id";

    private static final String COLUMNS_POSTGRESQL =
            "select a.attname as column_name,"
            + " (case when a.attnotnull and pk.attnum is null then '1' else null end) as is_required,"
            + " (case when pk.attnum is not null then '1' else '0' end) as is_pk,"
            + " a.attnum as sort,"
            + " coalesce(col_description(a.attrelid, a.attnum), '') as column_comment,"
            + " (case when exists (select 1 from pg_attrdef d"
            + " where d.adrelid = a.attrelid and d.adnum = a.attnum"
            + " and pg_get_expr(d.adbin, d.adrelid) like 'nextval%')"
            + " then '1' else '0' end) as is_increment,"
            + " (case t.typname"
            + " when 'varchar' then 'varchar(' || (a.atttypmod - 4) || ')'"
            + " when 'bpchar' then 'char(' || (a.atttypmod - 4) || ')'"
            + " when 'int8' then 'bigint'"
            + " when 'int4' then 'int'"
            + " when 'int2' then 'smallint'"
            + " when 'float4' then 'float'"
            + " when 'float8' then 'double'"
            + " when 'numeric' then 'decimal(' || ((a.atttypmod - 4) >> 16) || ',' || mod((a.atttypmod - 4), 65536) || ')'"
            + " when 'date' then 'date'"
            + " when 'timestamp' then 'datetime'"
            + " when 'timestamptz' then 'datetime'"
            + " when 'text' then 'text'"
            + " when 'bytea' then 'blob'"
            + " else t.typname"
            + " end) as column_type"
            + " from pg_attribute a"
            + " join pg_class cl on cl.oid = a.attrelid"
            + " join pg_namespace n on n.oid = cl.relnamespace"
            + " join pg_type t on t.oid = a.atttypid"
            + " left join (select con.conrelid, k.attnum"
            + " from pg_constraint con"
            + " join pg_attribute k on k.attrelid = con.conrelid and k.attnum = any (con.conkey)"
            + " where con.contype = 'p') pk"
            + " on pk.conrelid = a.attrelid and pk.attnum = a.attnum"
            + " where n.nspname = current_schema() and cl.relname = ?"
            + " and a.attnum > 0 and not a.attisdropped"
            + " order by a.attnum";

    private static final String COLUMNS_SQLSERVER =
            "select c.name as column_name,"
            + " (case when c.is_nullable = 0 and pk.column_id is null then '1' else null end) as is_required,"
            + " (case when pk.column_id is not null then '1' else '0' end) as is_pk,"
            + " c.column_id as sort,"
            + " coalesce(cast(ep.value as nvarchar(500)), '') as column_comment,"
            + " (case when c.is_identity = 1 then '1' else '0' end) as is_increment,"
            + " (case lower(t.name)"
            + " when 'varchar' then 'varchar(' + case when c.max_length = -1 then 'max' else cast(c.max_length as varchar(10)) end + ')'"
            + " when 'nvarchar' then 'nvarchar(' + case when c.max_length = -1 then 'max' else cast(c.max_length / 2 as varchar(10)) end + ')'"
            + " when 'char' then 'char(' + cast(c.max_length as varchar(10)) + ')'"
            + " when 'nchar' then 'nchar(' + cast(c.max_length / 2 as varchar(10)) + ')'"
            + " when 'decimal' then 'decimal(' + cast(c.precision as varchar(10)) + ',' + cast(c.scale as varchar(10)) + ')'"
            + " when 'numeric' then 'decimal(' + cast(c.precision as varchar(10)) + ',' + cast(c.scale as varchar(10)) + ')'"
            + " when 'bigint' then 'bigint'"
            + " when 'int' then 'int'"
            + " when 'smallint' then 'smallint'"
            + " when 'tinyint' then 'tinyint'"
            + " when 'float' then 'double'"
            + " when 'real' then 'float'"
            + " when 'datetime' then 'datetime'"
            + " when 'datetime2' then 'datetime'"
            + " when 'text' then 'text'"
            + " when 'ntext' then 'text'"
            + " else lower(t.name)"
            + " end) as column_type"
            + " from sys.columns c"
            + " join sys.types t on t.user_type_id = c.user_type_id"
            + " left join sys.extended_properties ep on ep.major_id = c.object_id and ep.minor_id = c.column_id and ep.name = 'MS_Description'"
            + " left join (select ic.object_id, ic.column_id"
            + " from sys.indexes i"
            + " join sys.index_columns ic on ic.object_id = i.object_id and ic.index_id = i.index_id"
            + " where i.is_primary_key = 1) pk"
            + " on pk.object_id = c.object_id and pk.column_id = c.column_id"
            + " where c.object_id = object_id(?)"
            + " order by c.column_id";

    private static final String COLUMNS_DB2 =
            "select c.colname as column_name,"
            + " (case when c.nulls = 'N' and pk.colname is null then '1' else null end) as is_required,"
            + " (case when pk.colname is not null then '1' else '0' end) as is_pk,"
            + " c.colno + 1 as sort,"
            + " coalesce(c.remarks, '') as column_comment,"
            + " (case when c.identity = 'Y' then '1' else '0' end) as is_increment,"
            + " (case lower(rtrim(c.typename))"
            + " when 'varchar' then 'varchar(' || cast(c.length as varchar(10)) || ')'"
            + " when 'vargraphic' then 'varchar(' || cast(c.length as varchar(10)) || ')'"
            + " when 'char' then 'char(' || cast(c.length as varchar(10)) || ')'"
            + " when 'decimal' then 'decimal(' || cast(c.length as varchar(10)) || ',' || cast(c.scale as varchar(10)) || ')'"
            + " when 'integer' then 'int'"
            + " when 'bigint' then 'bigint'"
            + " when 'smallint' then 'smallint'"
            + " when 'real' then 'float'"
            + " when 'double' then 'double'"
            + " when 'date' then 'date'"
            + " when 'time' then 'time'"
            + " when 'timestamp' then 'datetime'"
            + " when 'clob' then 'text'"
            + " else lower(rtrim(c.typename))"
            + " end) as column_type"
            + " from syscat.columns c"
            + " left join (select k.tabschema, k.tabname, k.colname"
            + " from syscat.keycoluse k"
            + " join syscat.tabconst tc on tc.tabschema = k.tabschema and tc.tabname = k.tabname"
            + " where tc.type = 'P' and k.constname = tc.constname) pk"
            + " on pk.tabschema = c.tabschema and pk.tabname = c.tabname and pk.colname = c.colname"
            + " where c.tabschema = current schema and c.tabname = upper(?)"
            + " order by c.colno";

    // ==================== 行映射（列名即原 resultMap 的 column） ====================

    private static GenTable mapTable(ResultSet rs, int rowNum) throws SQLException
    {
        GenTable table = new GenTable();
        table.setTableName(rs.getString("table_name"));
        table.setTableComment(rs.getString("table_comment"));
        try
        {
            table.setCreateTime(rs.getTimestamp("create_time"));
            table.setUpdateTime(rs.getTimestamp("update_time"));
        }
        catch (SQLException ignored)
        {
            // pg_catalog 分支无时间列（原 SQL select null），保持 null
        }
        return table;
    }

    private static GenTableColumn mapColumn(ResultSet rs, int rowNum) throws SQLException
    {
        GenTableColumn column = new GenTableColumn();
        column.setColumnName(rs.getString("column_name"));
        column.setIsRequired(rs.getString("is_required"));
        column.setIsPk(rs.getString("is_pk"));
        column.setSort(rs.getInt("sort"));
        column.setColumnComment(rs.getString("column_comment"));
        column.setIsIncrement(rs.getString("is_increment"));
        column.setColumnType(rs.getString("column_type"));
        return column;
    }

    private static String beginTime(GenTable t)
    {
        Object v = t.getParams() == null ? null : t.getParams().get("beginTime");
        return v == null ? null : String.valueOf(v);
    }

    private static String endTime(GenTable t)
    {
        Object v = t.getParams() == null ? null : t.getParams().get("endTime");
        return v == null ? null : String.valueOf(v);
    }

    private static boolean notBlank(String s)
    {
        return s != null && !s.isBlank();
    }
}
