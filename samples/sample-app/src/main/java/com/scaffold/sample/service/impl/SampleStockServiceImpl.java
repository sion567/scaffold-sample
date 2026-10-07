package com.scaffold.sample.service.impl;

import java.util.Arrays;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.scaffold.common.core.jpa.JpaSpecs;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.scaffold.common.security.utils.SecurityUtils;
import com.scaffold.sample.domain.SampleStock;
import com.scaffold.sample.repository.SampleStockRepository;
import com.scaffold.sample.service.ISampleStockService;

/**
 * 样例库存 服务实现
 *
 * @author scaffold
 */
@Service
public class SampleStockServiceImpl implements ISampleStockService
{
    @Autowired
    private SampleStockRepository stockRepository;

    @Override
    public SampleStock selectByStockId(Long stockId)
    {
        return stockRepository.findById(stockId).orElse(null);
    }

    @Override
    public List<SampleStock> selectList(SampleStock query)
    {
        return stockRepository.list(toSpec(query), Sort.by(Sort.Direction.DESC, "stockId"));
    }

    @Override
    public int insert(SampleStock stock)
    {
        stock.setUpdateBy(SecurityUtils.getUsername());
        stock.setUpdateTime(new Date());
        stockRepository.save(stock);
        return 1;
    }

    @Override
    public int update(SampleStock stock)
    {
        stock.setUpdateBy(SecurityUtils.getUsername());
        stock.setUpdateTime(new Date());
        stockRepository.save(stock);
        return 1;
    }

    @Override
    public int deleteByStockIds(Long[] stockIds)
    {
        stockRepository.deleteAllById(Arrays.asList(stockIds));
        return stockIds.length;
    }

    /** 动态条件（对齐原 SampleStockMapper.xml selectList） */
    private Specification<Object> toSpec(SampleStock query)
    {
        if (query == null)
        {
            return JpaSpecs.alwaysTrue();
        }
        return JpaSpecs.likeIf("productName", query.getProductName())
                .and(JpaSpecs.eqIf("categoryId", query.getCategoryId()))
                .and(JpaSpecs.likeIf("warehouse", query.getWarehouse()));
    }
}
