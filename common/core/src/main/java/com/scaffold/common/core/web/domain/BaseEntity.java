package com.scaffold.common.core.web.domain;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Entity基类（JPA {@link MappedSuperclass}，所有持久化实体的公共审计列）。
 *
 * <p>审计字段（createBy/createTime/updateBy/updateTime）由 Spring Data 审计
 * 自动填充：{@code @EnableJpaAuditing + AuditorAware}（common/core 的
 * {@code JpaAuditingAutoConfiguration} 自动装配开启，auditor 从 SecurityContext
 * 取当前用户；上下文缺席时不覆盖业务代码手动 set 的值）。
 * 列名映射走 Hibernate 默认驼峰转下划线（create_by/create_time），无需显式 @Column。</p>
 *
 * <p>设计约定：主键不放在本类——存量表主键列名各异（user_id/dept_id/...）且是对外
 * 契约字段，由各实体自行 {@code @Id} 声明；乐观锁版本由 {@link VersionedEntity}
 * （对应带 VERSION 列的表）提供 {@code @Version}；查询用参数挂在 {@link #getParams()}。</p>
 *
 * @author ct
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public class BaseEntity implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 搜索值（查询视图参数，不落库） */
    @JsonIgnore
    @Transient
    private String searchValue;

    /** 创建者 */
    @CreatedBy
    private String createBy;

    /** 创建时间 */
    @CreatedDate
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 更新者 */
    @LastModifiedBy
    private String updateBy;

    /** 更新时间 */
    @LastModifiedDate
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    /** 备注 */
    private String remark;

    /** 请求参数（查询视图参数，不落库：beginTime/endTime 区间、排序附加参数等） */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @Transient
    private Map<String, Object> params;

    public String getSearchValue()
    {
        return searchValue;
    }

    public void setSearchValue(String searchValue)
    {
        this.searchValue = searchValue;
    }

    public String getCreateBy()
    {
        return createBy;
    }

    public void setCreateBy(String createBy)
    {
        this.createBy = createBy;
    }

    public Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }

    public String getUpdateBy()
    {
        return updateBy;
    }

    public void setUpdateBy(String updateBy)
    {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime()
    {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime)
    {
        this.updateTime = updateTime;
    }

    public String getRemark()
    {
        return remark;
    }

    public void setRemark(String remark)
    {
        this.remark = remark;
    }

    public Map<String, Object> getParams()
    {
        if (params == null)
        {
            params = new HashMap<>();
        }
        return params;
    }

    public void setParams(Map<String, Object> params)
    {
        this.params = params;
    }
}
