package com.scaffold.common.core.jpa;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;

import com.scaffold.common.core.web.page.PageDomain;
import com.scaffold.common.core.web.page.TableDataInfo;

/**
 * 全仓统一的 Service 泛型基接口：单表 CRUD + 动态条件 + 分页的公共契约。
 * <p>
 * 业务 service 继承本接口、实现继承 {@link ScaffoldServiceImpl}，只需声明业务方法；
 * 公共 CRUD 落库/异常语义由基类统一（见其 javadoc），避免 31 张表各自手写一遍。
 *
 * @param <T> 实体类型
 * @param <ID> 主键类型
 */
public interface ScaffoldService<T, ID> {

    /** 按主键取数，不存在抛 {@link com.scaffold.common.core.exception.ServiceException}（替代原 selectXxxById + null 判断样板） */
    T requireById(ID id);

    /** 按主键取数，不存在返回 empty */
    Optional<T> findById(ID id);

    /** 全量列表（仅小表/字典类使用；大数据量走 {@link #findAll(Specification)} + 条件） */
    List<T> findAll();

    /** 条件列表（含 dataScope 时由调用方叠加 {@link #dataScope()} 语义，见 ScaffoldServiceImpl） */
    List<T> findAll(Specification<?> spec);

    /** 条件分页，返回对外契约 {@link TableDataInfo}（total/rows 与原 PageHelper 形态一致） */
    TableDataInfo page(Specification<?> spec, PageDomain pageDomain);

    /** 条件分页的 Spring Data 原生形态（需要 Page 语义的调用方使用） */
    Page<T> pageOf(Specification<?> spec, PageDomain pageDomain);

    /** 新增（审计字段自动填充） */
    T insert(T entity);

    /**
     * 更新（@Version 乐观锁：并发修改冲突抛
     * {@link com.scaffold.common.core.exception.ServiceException}，提示刷新重试）
     */
    T update(T entity);

    /** 按主键删除 */
    void removeById(ID id);

    /** 批量删除 */
    void removeByIds(Collection<ID> ids);

    /** 总数 */
    long count();
}
