package com.scaffold.sample.service.impl;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.scaffold.common.core.jpa.JpaSpecs;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.scaffold.common.core.exception.ServiceException;
import com.scaffold.common.core.utils.StringUtils;
import com.scaffold.sample.domain.SampleCategory;
import com.scaffold.sample.repository.SampleCategoryRepository;
import com.scaffold.sample.service.ISampleCategoryService;

/**
 * 样例商品分类 服务实现（树表）
 *
 * @author scaffold
 */
@Service
public class SampleCategoryServiceImpl implements ISampleCategoryService
{
    @Autowired
    private SampleCategoryRepository categoryRepository;

    @Override
    public SampleCategory selectByCategoryId(Long categoryId)
    {
        return categoryRepository.findById(categoryId).orElse(null);
    }

    @Override
    public List<SampleCategory> selectList(SampleCategory query)
    {
        return categoryRepository.list(toSpec(query), Sort.by(Sort.Direction.ASC, "orderNum"));
    }

    @Override
    public List<SampleCategory> buildTree(List<SampleCategory> list)
    {
        List<SampleCategory> trees = new ArrayList<>();
        for (SampleCategory node : list)
        {
            if (node.getParentId() == null || node.getParentId() == 0L)
            {
                trees.add(buildChildren(node, list));
            }
        }
        return trees;
    }

    private SampleCategory buildChildren(SampleCategory parent, List<SampleCategory> all)
    {
        for (SampleCategory node : all)
        {
            if (parent.getCategoryId() != null && parent.getCategoryId().equals(node.getParentId()))
            {
                parent.getChildren().add(buildChildren(node, all));
            }
        }
        return parent;
    }

    @Override
    public int insert(SampleCategory category)
    {
        fillAncestors(category);
        categoryRepository.save(category);
        return 1;
    }

    @Override
    public int update(SampleCategory category)
    {
        if (category.getCategoryId().equals(category.getParentId()))
        {
            throw new ServiceException("上级分类不能选择自己");
        }
        fillAncestors(category);
        categoryRepository.save(category);
        return 1;
    }

    private void fillAncestors(SampleCategory category)
    {
        if (category.getParentId() == null || category.getParentId() == 0L)
        {
            category.setParentId(0L);
            category.setAncestors("0");
        }
        else
        {
            SampleCategory parent = categoryRepository.findById(category.getParentId()).orElse(null);
            if (StringUtils.isNull(parent))
            {
                throw new ServiceException("上级分类不存在");
            }
            category.setAncestors(parent.getAncestors() + "," + parent.getCategoryId());
        }
    }

    @Override
    public String checkCategoryHasChildren(Long categoryId)
    {
        return categoryRepository.countByParentId(categoryId) > 0 ? "1" : "0";
    }

    @Override
    public int deleteByCategoryId(Long categoryId)
    {
        if (categoryRepository.existsById(categoryId)) {
            categoryRepository.deleteById(categoryId);
            return 1;
        }
        return 0;
    }

    /** 动态条件（对齐原 SampleCategoryMapper.xml selectList） */
    private Specification<Object> toSpec(SampleCategory query)
    {
        if (query == null)
        {
            return JpaSpecs.alwaysTrue();
        }
        return JpaSpecs.likeIf("categoryName", query.getCategoryName())
                .and(JpaSpecs.eqIfNotBlank("status", query.getStatus()));
    }
}
