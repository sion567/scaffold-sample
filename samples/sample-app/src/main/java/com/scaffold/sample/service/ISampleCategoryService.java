package com.scaffold.sample.service;

import java.util.List;
import com.scaffold.sample.domain.SampleCategory;

/**
 * 样例商品分类 服务接口（树表）
 *
 * @author scaffold
 */
public interface ISampleCategoryService
{
    public SampleCategory selectByCategoryId(Long categoryId);

    public List<SampleCategory> selectList(SampleCategory query);

    /** 组装树结构 */
    public List<SampleCategory> buildTree(List<SampleCategory> list);

    public int insert(SampleCategory category);

    public int update(SampleCategory category);

    public String checkCategoryHasChildren(Long categoryId);

    public int deleteByCategoryId(Long categoryId);
}
