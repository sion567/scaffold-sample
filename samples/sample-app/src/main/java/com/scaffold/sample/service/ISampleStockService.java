package com.scaffold.sample.service;

import java.util.List;
import com.scaffold.sample.domain.SampleStock;

/**
 * 样例库存 服务接口
 *
 * @author scaffold
 */
public interface ISampleStockService
{
    public SampleStock selectByStockId(Long stockId);

    public List<SampleStock> selectList(SampleStock query);

    public int insert(SampleStock stock);

    public int update(SampleStock stock);

    public int deleteByStockIds(Long[] stockIds);
}
