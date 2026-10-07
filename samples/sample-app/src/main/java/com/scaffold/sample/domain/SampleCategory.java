package com.scaffold.sample.domain;

import java.util.ArrayList;
import java.util.List;
import com.scaffold.common.core.web.domain.BaseEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;


/**
 * 样例商品分类对象 SAMPLE_CATEGORY（树表案例）
 *
 * @author scaffold
 */
@Entity
@Table(name = "SAMPLE_CATEGORY")
public class SampleCategory extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 分类ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long categoryId;

    /** 分类名称 */
    private String categoryName;

    /** 父分类ID */
    private Long parentId;

    /** 祖级列表 */
    private String ancestors;

    /** 显示顺序 */
    private Integer orderNum;

    /** 状态（0正常 1停用） */
    private String status;

    /** 子分类 */
    @Transient
    private List<SampleCategory> children = new ArrayList<>();

    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public String getCategoryName() { return categoryName; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public Long getParentId() { return parentId; }
    public void setAncestors(String ancestors) { this.ancestors = ancestors; }
    public String getAncestors() { return ancestors; }
    public void setOrderNum(Integer orderNum) { this.orderNum = orderNum; }
    public Integer getOrderNum() { return orderNum; }
    public void setStatus(String status) { this.status = status; }
    public String getStatus() { return status; }
    public List<SampleCategory> getChildren() { return children; }
    public void setChildren(List<SampleCategory> children) { this.children = children; }
}
