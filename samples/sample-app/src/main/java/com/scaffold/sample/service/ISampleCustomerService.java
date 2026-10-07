package com.scaffold.sample.service;

import java.util.List;
import com.scaffold.sample.domain.SampleCustomer;

/**
 * 样例客户 服务接口
 *
 * @author scaffold
 */
public interface ISampleCustomerService
{
    public SampleCustomer selectByCustomerId(Long customerId);

    public List<SampleCustomer> selectList(SampleCustomer query);

    public int insert(SampleCustomer customer);

    public int update(SampleCustomer customer);

    public int deleteByCustomerIds(Long[] customerIds);
}
