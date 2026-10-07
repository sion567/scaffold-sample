package com.scaffold.common.core.jpa;

import org.springframework.data.jpa.domain.Specification;

/**
 * 数据权限条件传递点：DataScopeAspect 在进入业务方法前把按角色解析出的
 * {@link Specification} 放入本线程上下文，受控查询经
 * {@code ScaffoldServiceImpl#dataScope()} 取用并在方法返回后清理。
 * <p>
 * 与原 `${params.dataScope}` SQL 拼接方案的对应关系：切面仍持有
 * {@code @DataScope(deptField/userField...)} 配置（值改为实体属性名），
 * 产出物从"SQL 字符串"变为类型安全的 Specification。
 */
public final class DataScopeContext {

    private static final ThreadLocal<Specification<?>> CURRENT = new ThreadLocal<>();

    private DataScopeContext() {}

    public static void hold(Specification<?> spec) {
        CURRENT.set(spec);
    }

    public static void clear() {
        CURRENT.remove();
    }

    static Specification<?> current() {
        return CURRENT.get();
    }

    /**
     * 当前数据权限条件（静态取用点：受控服务无论是否继承 ScaffoldServiceImpl 均可调用）；
     * 切面未注入（管理员/未登录）时返回恒真条件。
     */
    @SuppressWarnings("unchecked")
    public static Specification<Object> spec() {
        Specification<?> s = CURRENT.get();
        return s != null ? (Specification<Object>) s : (root, query, cb) -> cb.conjunction();
    }
}
