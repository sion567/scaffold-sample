package com.scaffold.common.core.jpa;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.scaffold.common.core.exception.ServiceException;

/**
 * 数据库方言识别：按 JDBC {@link java.sql.DatabaseMetaData#getDatabaseProductName() getDatabaseProductName()}
 * 把具体数据库归一到方言家族 id，供需要按方言分支的逻辑（如 gen 逆向查询）做服务层分发。
 *
 * <p>9 种受支持数据库归并为 6 个方言（完整映射与配置见 docs/multi-database-guide.md）：
 * {@code mysql}、{@code oracle}（含达梦）、{@code postgresql}（含 openGauss、瀚高、金仓 PG 兼容模式）、
 * {@code sqlserver}、{@code db2}、{@code h2}（本地零安装）。
 * 匹配规则：对产品名做小写化后<strong>包含</strong>匹配，按登记顺序取首个命中；
 * 未识别返回 {@code null}，由调用方决定兜底行为（勿静默假设某个方言）。</p>
 *
 * <p>本映射即全局 {@code DatabaseIdProvider}（{@link DatabaseDialectsProvider}，由
 * common/mybatis 自动装配注册）的解析内核：configuration.databaseId 与动态 SQL 的
 * {@code _databaseId} 都取这里的方言 id。方言差异的两种标准写法（见
 * docs/multi-database-guide.md §5）：统一用 {@code <choose>} + {@code _databaseId}
 * 保单语句 id（gen 逆向查询即此形态）。两遍解析的
 * 打标/兜底语义由 {@code DatabaseIdSemanticLockTest} 锁定——<b>同 id 混用打标与未打标
 * 语句时，未打标会静默兜底成默认</b>，写 mapper 前须知。</p>
 *
 * <p>各驱动返回的产品名可能因版本/兼容模式不同而变化（如金仓在 PG 兼容模式下直接报 PostgreSQL），
 * 新库接入时先执行 {@code SELECT * FROM ... } 前用一条
 * {@code connection.getMetaData().getDatabaseProductName()} 验证，再决定是否补登记。</p>
 *
 * @author ct
 */
public final class DatabaseDialects
{
    public static final String MYSQL = "mysql";

    public static final String ORACLE = "oracle";

    public static final String POSTGRESQL = "postgresql";

    public static final String SQLSERVER = "sqlserver";

    public static final String DB2 = "db2";

    public static final String H2 = "h2";

    private static final Logger log = LoggerFactory.getLogger(DatabaseDialects.class);

    /** key 为产品名的小写包含式匹配片段，value 为方言 id；顺序即优先级（更具体的前置） */
    private static final Map<String, String> PRODUCT_NAME_TO_DIALECT = new LinkedHashMap<>();

    static
    {
        PRODUCT_NAME_TO_DIALECT.put("dm dbms", ORACLE);
        PRODUCT_NAME_TO_DIALECT.put("oracle", ORACLE);
        PRODUCT_NAME_TO_DIALECT.put("mariadb", MYSQL);
        PRODUCT_NAME_TO_DIALECT.put("mysql", MYSQL);
        PRODUCT_NAME_TO_DIALECT.put("kingbase", POSTGRESQL);
        PRODUCT_NAME_TO_DIALECT.put("highgo", POSTGRESQL);
        PRODUCT_NAME_TO_DIALECT.put("opengauss", POSTGRESQL);
        PRODUCT_NAME_TO_DIALECT.put("postgresql", POSTGRESQL);
        PRODUCT_NAME_TO_DIALECT.put("microsoft sql server", SQLSERVER);
        PRODUCT_NAME_TO_DIALECT.put("db2", DB2);
        PRODUCT_NAME_TO_DIALECT.put("h2", H2);
    }

    private DatabaseDialects()
    {
    }

    /**
     * 从数据源识别当前方言（每次调用借用一个池化连接读取元数据，导入/同步等低频操作开销可忽略）。
     *
     * @param dataSource 当前生效的数据源
     * @return 方言 id（见类常量）；数据源未配置、产品名未登记时返回 {@code null}
     * @throws ServiceException 连接或元数据读取失败
     */
    public static String resolve(DataSource dataSource)
    {
        if (dataSource == null)
        {
            return null;
        }
        try (Connection connection = dataSource.getConnection())
        {
            return resolve(connection.getMetaData().getDatabaseProductName());
        }
        catch (SQLException e)
        {
            throw new ServiceException("识别数据库方言失败：" + e.getMessage());
        }
    }

    /**
     * 按数据库产品名识别方言（纯函数，便于单测）。
     *
     * @param databaseProductName DatabaseMetaData.getDatabaseProductName() 的返回值
     * @return 方言 id；入参为 {@code null} 或未登记的产品名返回 {@code null}
     */
    public static String resolve(String databaseProductName)
    {
        if (databaseProductName == null)
        {
            return null;
        }
        String name = databaseProductName.toLowerCase();
        for (Map.Entry<String, String> entry : PRODUCT_NAME_TO_DIALECT.entrySet())
        {
            if (name.contains(entry.getKey()))
            {
                return entry.getValue();
            }
        }
        log.warn("未识别的数据库产品名 [{}]，方言解析返回 null", databaseProductName);
        return null;
    }
}
