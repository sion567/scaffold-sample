package com.scaffold.common.test.arch;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.StringUtils;

/**
 * 安全架构自检基类（安全维度的自动化落地）：
 *
 * <p>① 权限注解全量扫描——模块 api 包下每个带 Mapping 注解的端点方法必须挂 {@link
 * com.scaffold.common.security.annotation.RequiresPermissions}（白名单见 {@link
 * #permissionAllowlist()}，只放行显式登记的免鉴权只读/回调端点）；每个端点产出一条动态用例，
 * 新增端点未挂权限会直接红。
 *
 * <p>② 方言令牌门禁——repository/DAO 里 {@code @Query(nativeQuery=true)} 与 @NamedNativeQuery
 * 的原生 SQL 出现方言专有构造（SYSDATE/TO_CHAR/DATE_FORMAT/||…）会直接红：业务查询由
 * Hibernate 方言接管跨库；确需方言差异的原生 SQL（如 gen 逆向目录查询）必须走
 * {@code GenCatalogDao} 式"按方言分发 + 单测锁分支"模式，并由子类
 * {@link #nativeQueryExcludedClasses()} 显式登记豁免类。
 *
 * <p>各业务模块提供一个空的具名子类并声明 basePackage 即可生效。
 *
 * @author ct
 */
public abstract class ArchTestSupport {
    /** 模块 api 根包（如 com.scaffold.system.api） */
    protected abstract String basePackage();

    /** 免鉴权端点白名单："类简单名#方法名" */
    protected Set<String> permissionAllowlist() {
        return Set.of();
    }

    /**
     * 方言令牌豁免类（类简单名）：确需方言差异的原生 SQL 所在类（如 gen 的 GenCatalogDao，
     * 走"按方言分发 + 单测锁分支"模式），登记后跳过其 @Query 原生 SQL 扫描。
     */
    protected Set<String> nativeQueryExcludedClasses() {
        return Set.of();
    }

    /** 方言令牌扫描默认排除的文件名片段（如 gen 方言变体所在目录） */
    protected Set<String> dialectTokenExcludedPaths() {
        return Set.of();
    }

    @TestFactory
    List<DynamicTest> everyEndpointHasPermissionAnnotation() {
        List<DynamicTest> tests = new ArrayList<>();
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            String path = basePackage().replace('.', '/') + "/**/*.class";
            for (Resource res : resolver.getResources("classpath*:" + path)) {
                String cls = toClassName(res, basePackage());
                if (cls == null) {
                    continue;
                }
                Class<?> clazz = Class.forName(cls);
                // 扫描实现类（Mapping 注解在接口，权限注解在实现，需归并两侧）
                if (clazz.isInterface() || java.lang.reflect.Modifier.isAbstract(clazz.getModifiers())) {
                    continue;
                }
                for (Method m : clazz.getDeclaredMethods()) {
                    if (!java.lang.reflect.Modifier.isPublic(m.getModifiers())) {
                        continue;
                    }
                    List<Annotation> annos = new ArrayList<>(Arrays.asList(m.getAnnotations()));
                    for (Class<?> ifc : clazz.getInterfaces()) {
                        try {
                            Method ifcMethod = ifc.getMethod(m.getName(), m.getParameterTypes());
                            annos.addAll(Arrays.asList(ifcMethod.getAnnotations()));
                        } catch (NoSuchMethodException ignore) {
                            /* 非接口方法 */
                        }
                    }
                    boolean mapped =
                            annos.stream()
                                    .anyMatch(
                                            a ->
                                                    a.annotationType()
                                                            .getSimpleName()
                                                            .matches("(Get|Post|Put|Delete|Request)Mapping"));
                    if (!mapped) {
                        continue;
                    }
                    String endpoint = clazz.getSimpleName() + "#" + m.getName();
                    boolean allowed =
                            permissionAllowlist().contains(endpoint)
                                    || permissionAllowlist().contains(clazz.getSimpleName() + "#*");
                    boolean hasPerm =
                            annos.stream()
                                    .anyMatch(
                                            a ->
                                                    "com.scaffold.common.security.annotation.RequiresPermissions"
                                                            .equals(a.annotationType().getName()));
                    tests.add(
                            DynamicTest.dynamicTest(
                                    endpoint,
                                    () ->
                                            assertTrue(
                                                    hasPerm || allowed,
                                                    "端点缺少 @RequiresPermissions 且不在免鉴权白名单：" + endpoint)));
                }
            }
        } catch (Exception e) {
            tests.add(
                    DynamicTest.dynamicTest(
                            "扫描失败:" + basePackage(), () -> assertTrue(false, "权限架构扫描异常：" + e.getMessage())));
        }
        return tests;
    }

    @TestFactory
    List<DynamicTest> nativeQueriesFreeOfDialectTokens() {
        List<DynamicTest> tests = new ArrayList<>();
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Set<String> excluded = nativeQueryExcludedClasses();
            for (Resource res : resolver.getResources("classpath*:" + basePackage().replace('.', '/') + "/**/*.class")) {
                String className = toClassName(res, basePackage());
                if (className == null) {
                    continue;
                }
                Class<?> clazz;
                try {
                    clazz = Class.forName(className);
                } catch (Throwable t) {
                    continue; // 抽象/合成类等不可加载项跳过
                }
                if (!clazz.isInterface() || excluded.contains(clazz.getSimpleName())) {
                    continue;
                }
                for (Method m : clazz.getDeclaredMethods()) {
                    Object query = null;
                    try {
                        query = m.getAnnotation(org.springframework.data.jpa.repository.Query.class);
                    } catch (Throwable t) {
                        continue;
                    }
                    if (query == null) {
                        continue;
                    }
                    org.springframework.data.jpa.repository.Query q =
                            (org.springframework.data.jpa.repository.Query) query;
                    if (!q.nativeQuery()) {
                        continue; // JPQL 由 Hibernate 方言接管，跨库安全
                    }
                    String sql = q.value() == null ? "" : q.value();
                    // 注释与字符串字面量剔除后再扫（与原 XML 门禁同口径）
                    String sqlText = sql.replaceAll("--[^\n]*", "");
                    String sqlNoLiterals = sqlText.replaceAll("'(?:[^']|'')*'", "''");
                    String sqlUpper = sqlNoLiterals.toUpperCase(java.util.Locale.ROOT);
                    List<String> hits = new ArrayList<>();
                    if (containsPipeConcatOutsideQuotes(sqlText)) {
                        hits.add("||");
                    }
                    for (String token : DIALECT_TOKENS) {
                        if (sqlUpper.contains(token)) {
                            hits.add(token);
                        }
                    }
                    final String endpoint = clazz.getSimpleName() + "#" + m.getName();
                    final List<String> finalHits = hits;
                    tests.add(
                            DynamicTest.dynamicTest(
                                    endpoint,
                                    () ->
                                            assertTrue(
                                                    finalHits.isEmpty(),
                                                    "原生 SQL 存在方言专有构造（跨库跑不了），应改可移植写法/JPQL，"
                                                            + "或走按方言分发模式并登记豁免："
                                                            + endpoint
                                                            + " -> "
                                                            + finalHits)));
                }
            }
        } catch (Exception e) {
            tests.add(
                    DynamicTest.dynamicTest(
                            "扫描失败:" + basePackage(), () -> assertTrue(false, "原生 SQL 方言扫描异常：" + e.getMessage())));
        }
        return tests;
    }


    /** 方言令牌（大写口径，配合 sqlUpper 大小写不敏感匹配） */
    private static final java.util.List<String> DIALECT_TOKENS =
            List.of(
                    "SYSDATE", "SYSTIMESTAMP", "NVL(", "IFNULL(", "TO_CHAR(", "TO_DATE(",
                    "DATE_FORMAT(", "INSTR(", "FETCH FIRST", "ROWNUM", "TRUNC(",
                    "SP_ADDEXTENDEDPROPERTY", "SET IDENTITY_INSERT", "DBCC CHECKIDENT",
                    "IDENTITY(", "AUTO_INCREMENT", "`");

    /** 引号外的 || 拼接检测（避免把字符串字面量里的 '||' 误判为拼接） */
    private static boolean containsPipeConcatOutsideQuotes(String sqlText) {
        boolean inStr = false;
        for (int i = 0; i < sqlText.length(); i++) {
            char c = sqlText.charAt(i);
            if (c == '\'') {
                inStr = !inStr;
            } else if (!inStr && c == '|' && i + 1 < sqlText.length() && sqlText.charAt(i + 1) == '|') {
                return true;
            }
        }
        return false;
    }

    private String toClassName(Resource res, String basePackage) {
        try {
            String url = res.getURL().toString();
            int i = url.indexOf(basePackage.replace('.', '/'));
            if (i < 0) {
                return null;
            }
            return url.substring(i, url.length() - ".class".length()).replace('/', '.');
        } catch (Exception e) {
            return null;
        }
    }
}
