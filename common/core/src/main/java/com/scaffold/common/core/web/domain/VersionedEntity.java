package com.scaffold.common.core.web.domain;

import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;

/**
 * 带乐观锁版本的实体基类（对应基线中含 {@code VERSION BIGINT DEFAULT 0} 列的表）。
 *
 * <p>{@code @Version} 由 JPA 自动维护：持久化时初始化、更新时自增并进 WHERE 条件，
 * 并发修改冲突抛 {@code ObjectOptimisticLockingFailureException}——替代原
 * common/mybatis 的 OptimisticLockerInnerInterceptor 与手写 {@code version=version+1} SQL。</p>
 *
 * <p>无 VERSION 列的实体（关系表/日志表等）直接继承 {@link BaseEntity} 即可。</p>
 *
 * @author ct
 */
@MappedSuperclass
public abstract class VersionedEntity extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 乐观锁版本号（JPA 自动维护，业务代码不要手动 set） */
    @Version
    private Integer version;

    public Integer getVersion()
    {
        return version;
    }

    public void setVersion(Integer version)
    {
        this.version = version;
    }
}
