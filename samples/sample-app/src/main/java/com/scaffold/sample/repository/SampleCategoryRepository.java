package com.scaffold.sample.repository;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.scaffold.common.core.jpa.ScaffoldRepository;
import com.scaffold.sample.domain.SampleCategory;

/**
 * SampleCategory 数据访问（泛型基接口派生：CRUD + Specification 动态条件）
 *
 * @author scaffold
 */
@Repository
public interface SampleCategoryRepository extends ScaffoldRepository<SampleCategory, Long>
{
    /** 树子节点计数（删除前检查） */
    long countByParentId(Long parentId);
}
