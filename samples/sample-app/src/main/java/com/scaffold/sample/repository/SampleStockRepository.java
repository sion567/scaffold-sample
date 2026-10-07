package com.scaffold.sample.repository;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.scaffold.common.core.jpa.ScaffoldRepository;
import com.scaffold.sample.domain.SampleStock;

/**
 * SampleStock 数据访问（泛型基接口派生：CRUD + Specification 动态条件）
 *
 * @author scaffold
 */
@Repository
public interface SampleStockRepository extends ScaffoldRepository<SampleStock, Long>
{
    /** 低库存计数（库存预警任务用） */
    long countByQuantityLessThan(int quantity);
}
