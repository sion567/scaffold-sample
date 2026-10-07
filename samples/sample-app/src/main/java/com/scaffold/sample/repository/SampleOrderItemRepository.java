package com.scaffold.sample.repository;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.scaffold.common.core.jpa.ScaffoldRepository;
import com.scaffold.sample.domain.SampleOrderItem;

/**
 * SampleOrderItem 数据访问（泛型基接口派生：CRUD + Specification 动态条件）
 *
 * @author scaffold
 */
@Repository
public interface SampleOrderItemRepository extends ScaffoldRepository<SampleOrderItem, Long>
{
    /** 订单明细（按 ITEM_ID 升序，对齐原 XML order by） */
    List<SampleOrderItem> findByOrderIdOrderByItemIdAsc(Long orderId);

    /** 删除订单全部明细（重改订单时先删后插） */
    void deleteByOrderId(Long orderId);
}
