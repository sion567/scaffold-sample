package com.scaffold.sample.service.impl;

import java.util.Arrays;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.scaffold.common.core.jpa.JpaSpecs;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.scaffold.common.core.exception.ServiceException;
import com.scaffold.common.core.utils.StringUtils;
import com.scaffold.sample.domain.SampleCustomer;
import com.scaffold.sample.repository.SampleCustomerRepository;
import com.scaffold.sample.service.ISampleCustomerService;

/**
 * 样例客户 服务实现
 *
 * @author scaffold
 */
@Service
public class SampleCustomerServiceImpl implements ISampleCustomerService
{
    @Autowired
    private SampleCustomerRepository customerRepository;

    @Override
    public SampleCustomer selectByCustomerId(Long customerId)
    {
        return customerRepository.findById(customerId).orElse(null);
    }

    @Override
    public List<SampleCustomer> selectList(SampleCustomer query)
    {
        return customerRepository.list(toSpec(query), Sort.by(Sort.Direction.DESC, "customerId"));
    }

    @Override
    public int insert(SampleCustomer customer)
    {
        if (StringUtils.isEmpty(customer.getCustomerName()))
        {
            throw new ServiceException("客户名称不能为空");
        }
        customerRepository.save(customer);
        return 1;
    }

    @Override
    public int update(SampleCustomer customer)
    {
        customerRepository.save(customer);
        return 1;
    }

    @Override
    public int deleteByCustomerIds(Long[] customerIds)
    {
        customerRepository.deleteAllById(Arrays.asList(customerIds));
        return customerIds.length;
    }

    /** 动态条件（对齐原 SampleCustomerMapper.xml selectList） */
    private Specification<Object> toSpec(SampleCustomer query)
    {
        if (query == null)
        {
            return JpaSpecs.alwaysTrue();
        }
        return JpaSpecs.likeIf("customerName", query.getCustomerName())
                .and(JpaSpecs.likeIf("phone", query.getPhone()))
                .and(JpaSpecs.eqIfNotBlank("status", query.getStatus()));
    }
}
