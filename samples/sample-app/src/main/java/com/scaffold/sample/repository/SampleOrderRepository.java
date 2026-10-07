package com.scaffold.sample.repository;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.scaffold.common.core.jpa.ScaffoldRepository;
import com.scaffold.sample.domain.SampleOrder;

/**
 * SampleOrder 数据访问（泛型基接口派生：CRUD + Specification 动态条件）
 *
 * @author scaffold
 */
@Repository
public interface SampleOrderRepository extends ScaffoldRepository<SampleOrder, Long>
{
    /** 按订单号取单（唯一） */
    SampleOrder findByOrderNo(String orderNo);
}
