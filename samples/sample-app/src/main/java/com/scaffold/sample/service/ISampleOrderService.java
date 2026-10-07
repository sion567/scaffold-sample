package com.scaffold.sample.service;

import java.util.List;
import com.scaffold.flow.api.FlowHistory;
import com.scaffold.sample.domain.SampleOrder;

/**
 * 样例订单 服务接口（主子表 + 工作流审批）
 *
 * @author scaffold
 */
public interface ISampleOrderService
{
    public SampleOrder selectByOrderId(Long orderId);

    public List<SampleOrder> selectList(SampleOrder query);

    /** 新增订单（主子表同事务保存） */
    public int insert(SampleOrder order);

    /** 修改订单（明细全删全插） */
    public int update(SampleOrder order);

    public int deleteByOrderIds(Long[] orderIds);

    /** 提交订单进入审批流 */
    public String submit(Long orderId, String operator);

    /** 审批：pass=true 通过办结，false 相邻退回提交人 */
    public String audit(Long orderId, boolean pass, String remark, String operator);

    /** 查询审批流转历史 */
    public List<FlowHistory> flowHistory(Long orderId);
}
