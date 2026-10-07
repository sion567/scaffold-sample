package com.scaffold.common.core.jpa;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import com.scaffold.common.core.exception.ServiceException;
import com.scaffold.common.core.utils.PageUtils;
import com.scaffold.common.core.web.page.PageDomain;
import com.scaffold.common.core.web.page.TableDataInfo;

/**
 * {@link ScaffoldService} 的泛型基实现：repository 经泛型实际类型自动注入
 * （{@code @Autowired protected R repository}，子类声明
 * {@code extends ScaffoldServiceImpl<SysUserRepository, SysUser, Long>} 即可）。
 * <p>
 * 公共语义约定（各服务一致，不再各写各的）：
 * <ul>
 *   <li>requireById 缺失抛 ServiceException("数据不存在或已被删除")；</li>
 *   <li>update 走 saveAndFlush 让 @Version 冲突同步暴露，翻译为"数据已被他人修改"业务异常；</li>
 *   <li>page 由 PageDomain 构建 PageRequest（排序属性名注入由 Spring Data 属性解析兜底）；</li>
 *   <li>{@link #dataScope()}：DataScopeAspect 在同线程写入的条件（无注入时恒真），
 *       数据权限受控的查询由业务方法自行叠加，如
 *       {@code repository.findAll(dataScope().and(spec))}。</li>
 * </ul>
 *
 * @param <R> 具体 repository 类型
 * @param <T> 实体类型
 * @param <ID> 主键类型
 */
public abstract class ScaffoldServiceImpl<R extends ScaffoldRepository<T, ID>, T, ID> implements ScaffoldService<T, ID> {

    /** 按子类声明的泛型实际类型注入对应 repository（Spring 解析 R 的类型参数定位 bean） */
    @Autowired
    protected R repository;

    @Override
    public T requireById(ID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ServiceException("数据不存在或已被删除"));
    }

    @Override
    public Optional<T> findById(ID id) {
        return repository.findById(id);
    }

    @Override
    public List<T> findAll() {
        return repository.findAll();
    }

    @Override
    public List<T> findAll(Specification<?> spec) {
        return repository.list(spec);
    }

    @Override
    public TableDataInfo page(Specification<?> spec, PageDomain pageDomain) {
        return TableDataInfo.from(repository.page(spec, PageUtils.toPageRequest(pageDomain)));
    }

    @Override
    public Page<T> pageOf(Specification<?> spec, PageDomain pageDomain) {
        return repository.page(spec, PageUtils.toPageRequest(pageDomain));
    }

    @Override
    public T insert(T entity) {
        return repository.save(entity);
    }

    @Override
    public T update(T entity) {
        try {
            return repository.saveAndFlush(entity);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new ServiceException("数据已被他人修改，请刷新后重试");
        }
    }

    @Override
    public void removeById(ID id) {
        repository.deleteById(id);
    }

    @Override
    public void removeByIds(Collection<ID> ids) {
        repository.deleteAllById(ids);
    }

    @Override
    public long count() {
        return repository.count();
    }

    /** 数据权限条件（DataScopeAspect 同线程注入；未注入返回恒真条件） */
    protected Specification<Object> dataScope() {
        return DataScopeContext.spec();
    }
}
