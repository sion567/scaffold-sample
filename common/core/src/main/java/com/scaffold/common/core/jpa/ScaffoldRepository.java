package com.scaffold.common.core.jpa;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * 全仓统一的 JPA Repository 泛型基接口。
 * <p>
 * 业务 repository 一律继承本接口（{@code public interface SysUserRepository
 * extends ScaffoldRepository<SysUser, Long>}），即同时获得 CRUD（JpaRepository）
 * 与动态条件/分页（JpaSpecificationExecutor）能力，无需再声明其它父接口。
 * {@code @NoRepositoryBean} 防止 Spring Data 为本接口本身创建代理。
 * <p>
 * 条件构造统一走 {@link JpaSpecs}（返回 Specification&lt;Object&gt;，
 * 字段名为字符串、类型参数无实际约束），因此本接口提供通配符桥接方法
 * {@link #list}/{@link #page}/{@link #count}，调用点免强转。
 */
@NoRepositoryBean
public interface ScaffoldRepository<T, ID> extends JpaRepository<T, ID>, JpaSpecificationExecutor<T> {

    /** 条件列表（{@link JpaSpecs} 组合条件） */
    @SuppressWarnings("unchecked")
    default List<T> list(Specification<?> spec) {
        return ((JpaSpecificationExecutor<T>) this).findAll((Specification<T>) spec);
    }

    /** 条件列表 + 固定排序 */
    @SuppressWarnings("unchecked")
    default List<T> list(Specification<?> spec, org.springframework.data.domain.Sort sort) {
        return ((JpaSpecificationExecutor<T>) this).findAll((Specification<T>) spec, sort);
    }

    /** 条件分页（{@link JpaSpecs} 组合条件 + PageRequest） */
    @SuppressWarnings("unchecked")
    default Page<T> page(Specification<?> spec, Pageable pageable) {
        return ((JpaSpecificationExecutor<T>) this).findAll((Specification<T>) spec, pageable);
    }

    /** 条件计数 */
    @SuppressWarnings("unchecked")
    default long countBy(Specification<?> spec) {
        return ((JpaSpecificationExecutor<T>) this).count((Specification<T>) spec);
    }
}
