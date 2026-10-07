package com.scaffold.common.test;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * JPA 切片测试注解（不启动 Spring 容器）。
 *
 * <p>在 H2 内存库（MODE=Oracle）上手工组装 Hibernate {@code EntityManagerFactory}
 * 与 Spring Data repository 代理（{@code JpaRepositoryFactory}），加载生产在用的
 * 真实实体映射与 DDL。测试方法参数可直接声明 repository 接口或 {@code EntityManager}
 * （均为类级共享、线程安全）：</p>
 *
 * <pre>{@code
 * @H2JpaTest(
 *         ddl = "sql/system/sys_user_h2.sql",                    // classpath 建表+种子脚本，按序执行
 *         entityPackages = "com.scaffold.system.api.domain"      // 实体扫描包（生产持久化单元同款）
 * )
 * class SysUserRepositoryTest
 * {
 *     @Test
 *     void selectByUserName(SysUserRepository repository)
 *     {
 *         SysUser user = repository.findByUserName("admin").orElseThrow();
 *         assertEquals("admin", user.getUserName());
 *     }
 * }
 * }</pre>
 *
 * <p>使用约定：</p>
 * <ul>
 *   <li>{@link #ddl()} 为 H2 建表脚本（列集对照生产 flyway 基线，
 *       实体新增列时同步补脚本）；</li>
 *   <li>{@link #entityPackages()} 必须与生产 {@code @EntityScan} 覆盖面一致，
 *       避免切片映射集与生产漂移；</li>
 *   <li>H2 库名按测试类隔离，类与类之间互不污染；进程结束随 JVM 释放；</li>
 *   <li>本注解不启用 Spring 审计（@CreatedDate 等在切片内不生效，断言需手动构造）。</li>
 * </ul>
 *
 * @author ct
 */
@Documented
@Inherited
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(H2JpaTestExtension.class)
public @interface H2JpaTest
{
    /**
     * classpath 下的建表 + 种子数据脚本（H2 方言，按声明顺序执行，脚本内以 ; 分隔语句，须为 UTF-8 编码）
     */
    String[] ddl() default {};

    /**
     * 实体扫描包（等价生产持久化单元的 packagesToScan/@EntityScan 覆盖面）
     */
    String[] entityPackages();

    /**
     * 额外追加到 JDBC URL 的参数（自动以 ; 拼接）。
     * 默认 URL 已含：MODE=Oracle（空串即 NULL、SYSDATE 等 Oracle 语义）
     * + DB_CLOSE_DELAY=-1 + CASE_INSENSITIVE_IDENTIFIERS=TRUE（兼容带引号的大小写混写 DDL）。
     * 注意不要在这里重复传这几个参数。
     */
    String[] urlOptions() default {};

    /**
     * 是否打印 SQL（hibernate.show_sql），排查映射问题时打开
     */
    boolean showSql() default false;
}
