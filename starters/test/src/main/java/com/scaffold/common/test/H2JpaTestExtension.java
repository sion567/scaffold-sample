package com.scaffold.common.test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.h2.jdbcx.JdbcDataSource;
import org.h2.tools.RunScript;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

/**
 * {@link H2JpaTest} 的 JUnit 5 扩展：H2 内存库（MODE=Oracle）上手工装配
 * Hibernate EntityManagerFactory（生产同款 HibernateJpaVendorAdapter，ddl-auto=none）
 * 与 Spring Data repository 代理（JpaRepositoryFactory），两者都不经 Spring 容器。
 *
 * <p>对象均放在 JUnit ExtensionContext.Store：EMF/repository 工厂类级创建一次，
 * 生命周期随测试上下文自动回收；repository 代理基于 SharedEntityManagerCreator
 * 的线程安全共享 EntityManager。</p>
 *
 * @author ct
 */
class H2JpaTestExtension implements BeforeAllCallback, AfterAllCallback, ParameterResolver
{
    private static final ExtensionContext.Namespace NS =
            ExtensionContext.Namespace.create(H2JpaTestExtension.class);

    @Override
    public void beforeAll(ExtensionContext context) throws Exception
    {
        Class<?> outerClass = outerClassOf(context.getRequiredTestClass());
        H2JpaTest cfg = outerClass.getAnnotation(H2JpaTest.class);
        if (cfg == null)
        {
            throw new IllegalStateException(outerClass.getName() + " 缺少 @H2JpaTest 注解");
        }
        ExtensionContext.Store store = context.getRoot().getStore(NS);
        if (store.get(outerClass, EntityManagerFactory.class) != null)
        {
            return; // 类级 EMF 只建一次
        }

        String dbName = outerClass.getSimpleName().toLowerCase()
                + "_" + Integer.toHexString(outerClass.getName().hashCode());

        StringBuilder url = new StringBuilder("jdbc:h2:mem:").append(dbName)
                // MODE=Oracle 必须保留：SYSDATE、空串即 NULL 等 Oracle 语义全靠它
                .append(";MODE=Oracle")
                .append(";DB_CLOSE_DELAY=-1")
                // 兼容带引号的大小写混写 DDL，对齐 Oracle 标识符不区分大小写语义
                .append(";CASE_INSENSITIVE_IDENTIFIERS=TRUE");
        for (String option : cfg.urlOptions())
        {
            url.append(';').append(option);
        }

        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL(url.toString());
        dataSource.setUser("sa");
        dataSource.setPassword("");

        try (Connection connection = dataSource.getConnection())
        {
            for (String script : cfg.ddl())
            {
                runScript(connection, script);
            }
        }

        LocalContainerEntityManagerFactoryBean emfBean = new LocalContainerEntityManagerFactoryBean();
        emfBean.setDataSource(dataSource);
        emfBean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        emfBean.setPackagesToScan(cfg.entityPackages());
        Map<String, Object> jpaProps = new HashMap<>();
        // schema 一律由 Flyway/测试 DDL 管理，Hibernate 只做映射不建表
        jpaProps.put("hibernate.hbm2ddl.auto", "none");
        // 与 Spring Boot 自动装配一致：驼峰属性 → 下划线列名
        jpaProps.put("hibernate.physical_naming_strategy",
                "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy");
        if (cfg.showSql())
        {
            jpaProps.put("hibernate.show_sql", "true");
        }
        emfBean.setJpaPropertyMap(jpaProps);
        emfBean.afterPropertiesSet();
        EntityManagerFactory emf = emfBean.getObject();

        store.put(outerClass, emf);
        store.put(repoFactoryKey(outerClass),
                new JpaRepositoryFactory(SharedEntityManagerCreator.createSharedEntityManager(emf)));
        store.put(txKey(outerClass), new org.springframework.transaction.support.TransactionTemplate(
                new org.springframework.orm.jpa.JpaTransactionManager(emf)));
    }

    private static String repoFactoryKey(Class<?> outerClass)
    {
        return outerClass.getName() + "#repoFactory";
    }

    private static String txKey(Class<?> outerClass)
    {
        return outerClass.getName() + "#txTemplate";
    }

    private void runScript(Connection connection, String classpathLocation) throws Exception
    {
        try (InputStream in = H2JpaTestExtension.class.getClassLoader().getResourceAsStream(classpathLocation))
        {
            if (in == null)
            {
                throw new IllegalStateException("DDL 脚本不存在: " + classpathLocation);
            }
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8))
            {
                RunScript.execute(connection, reader);
            }
        }
    }

    @Override
    public void afterAll(ExtensionContext context)
    {
        Class<?> outerClass = outerClassOf(context.getRequiredTestClass());
        // @Nested 内部类的 afterAll 也会触发本回调——清理只允许最外层类做一次，
        // 否则嵌套类之间会把 EMF 提前移除/关闭，后续嵌套类重建 DDL 时撞表
        if (context.getRequiredTestClass() != outerClass)
        {
            return;
        }
        ExtensionContext.Store store = context.getRoot().getStore(NS);
        EntityManagerFactory emf = store.remove(outerClass, EntityManagerFactory.class);
        if (emf != null)
        {
            emf.close();
        }
        store.remove(repoFactoryKey(outerClass));
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
    {
        Class<?> outerClass = outerClassOf(parameterContext.getDeclaringExecutable().getDeclaringClass());
        if (outerClass.getAnnotation(H2JpaTest.class) == null)
        {
            return false;
        }
        Class<?> type = parameterContext.getParameter().getType();
        return EntityManager.class == type || isRepositoryInterface(type);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
    {
        Class<?> type = parameterContext.getParameter().getType();
        ExtensionContext.Store store = extensionContext.getRoot().getStore(NS);
        Class<?> outerClass = outerClassOf(extensionContext.getRequiredTestClass());
        if (EntityManager.class == type)
        {
            EntityManagerFactory emf = store.get(outerClass, EntityManagerFactory.class);
            return SharedEntityManagerCreator.createSharedEntityManager(emf);
        }
        JpaRepositoryFactory factory = store.get(repoFactoryKey(outerClass), JpaRepositoryFactory.class);
        Object repository = factory.getRepository(asRepositoryInterface(type));
        org.springframework.transaction.support.TransactionTemplate tx =
                store.get(txKey(outerClass), org.springframework.transaction.support.TransactionTemplate.class);
        // 每个方法调用包在事务里（等价 @Transactional 测试语义：写操作自动提交）
        return java.lang.reflect.Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type },
                (proxy, method, args) -> tx.execute(status -> {
                    try {
                        return method.invoke(repository, args);
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        throw (e.getCause() instanceof RuntimeException re) ? re
                                : new IllegalStateException(e.getCause());
                    } catch (IllegalAccessException e) {
                        throw new IllegalStateException(e);
                    }
                }));
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends JpaRepository<?, ?>> asRepositoryInterface(Class<?> type)
    {
        if (!isRepositoryInterface(type))
        {
            throw new ParameterResolutionException("参数须为 JpaRepository 子接口或 EntityManager: " + type.getName());
        }
        return (Class<? extends JpaRepository<?, ?>>) type;
    }

    /** 是否为（直接或间接继承 JpaRepository 的）接口 */
    private static boolean isRepositoryInterface(Class<?> type)
    {
        if (!type.isInterface())
        {
            return false;
        }
        if (JpaRepository.class.isAssignableFrom(type) && type != JpaRepository.class)
        {
            return true;
        }
        for (Type face : type.getGenericInterfaces())
        {
            if (face instanceof Class<?> c && isRepositoryInterface(c))
            {
                return true;
            }
            // 泛型父接口（如 ScaffoldRepository<T, ID> extends JpaRepository）：经原始类型判断
            if (face instanceof ParameterizedType pt && pt.getRawType() instanceof Class<?> raw
                    && isRepositoryInterface(raw))
            {
                return true;
            }
        }
        return false;
    }

    private static Class<?> outerClassOf(Class<?> testClass)
    {
        Class<?> outer = testClass;
        while (outer.getDeclaringClass() != null)
        {
            outer = outer.getDeclaringClass();
        }
        return outer;
    }
}
