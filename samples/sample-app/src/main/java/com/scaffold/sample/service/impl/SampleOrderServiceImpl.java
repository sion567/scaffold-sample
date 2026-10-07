package com.scaffold.sample.service.impl;

import java.util.Arrays;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.scaffold.common.core.jpa.JpaSpecs;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.scaffold.common.core.exception.ServiceException;
import com.scaffold.common.core.utils.StringUtils;
import com.scaffold.flow.api.FlowHistory;
import com.scaffold.flow.api.FlowTask;
import com.scaffold.flow.api.WorkflowService;
import com.scaffold.sample.domain.SampleOrder;
import com.scaffold.sample.domain.SampleOrderItem;
import com.scaffold.sample.flow.SampleOrderFlowContributor;
import com.scaffold.sample.repository.SampleOrderItemRepository;
import com.scaffold.sample.repository.SampleOrderRepository;
import com.scaffold.sample.service.ISampleOrderService;

/**
 * 样例订单 服务实现（主子表 + 工作流审批）
 *
 * 工作流语义：业务表是真值源，引擎状态仅服务编排，业务落库与引擎推进同事务；
 * submit 提交（推进到 audit）-> audit 审批（通过=办结，驳回=相邻退回 submit，可改后重新提交）。
 *
 * @author scaffold
 */
@Service
public class SampleOrderServiceImpl implements ISampleOrderService
{
    private static final String PROCESS_CODE = SampleOrderFlowContributor.ORDER_APPROVE_PROCESS;

    @Autowired
    private SampleOrderRepository orderRepository;

    @Autowired
    private SampleOrderItemRepository orderItemRepository;

    @Autowired
    private WorkflowService workflowService;

    @Override
    public SampleOrder selectByOrderId(Long orderId)
    {
        SampleOrder order = orderRepository.findById(orderId).orElse(null);
        if (order != null)
        {
            order.setItems(orderItemRepository.findByOrderIdOrderByItemIdAsc(orderId));
        }
        return order;
    }

    @Override
    public List<SampleOrder> selectList(SampleOrder query)
    {
        return orderRepository.list(toSpec(query), Sort.by(Sort.Direction.DESC, "orderId"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insert(SampleOrder order)
    {
        if (StringUtils.isEmpty(order.getOrderNo()))
        {
            throw new ServiceException("订单号不能为空");
        }
        calcTotalAmount(order);
        int rows = insertRow(order);
        insertItems(order);
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int update(SampleOrder order)
    {
        SampleOrder old = orderRepository.findById(order.getOrderId()).orElse(null);
        if (old == null)
        {
            throw new ServiceException("订单不存在");
        }
        if (!"0".equals(old.getStatus()) && !"3".equals(old.getStatus()))
        {
            throw new ServiceException("仅待提交或已驳回的订单允许修改");
        }
        calcTotalAmount(order);
        orderItemRepository.deleteByOrderId(order.getOrderId());
        insertItems(order);
        orderRepository.save(order);
        return 1;
    }

    private void calcTotalAmount(SampleOrder order)
    {
        BigDecimal total = BigDecimal.ZERO;
        if (order.getItems() != null)
        {
            for (SampleOrderItem item : order.getItems())
            {
                if (item.getPrice() != null && item.getQuantity() != null)
                {
                    total = total.add(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
                }
            }
        }
        order.setTotalAmount(total);
    }

    private void insertItems(SampleOrder order)
    {
        if (order.getItems() != null && !order.getItems().isEmpty())
        {
            for (SampleOrderItem item : order.getItems())
            {
                item.setOrderId(order.getOrderId());
            }
            orderItemRepository.saveAll(order.getItems());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteByOrderIds(Long[] orderIds)
    {
        for (Long orderId : orderIds)
        {
            SampleOrder old = orderRepository.findById(orderId).orElse(null);
            if (old != null && "1".equals(old.getStatus()))
            {
                throw new ServiceException("订单[" + old.getOrderNo() + "]审批中，不允许删除");
            }
        }
        orderRepository.deleteAllById(Arrays.asList(orderIds));
        return orderIds.length;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String submit(Long orderId, String operator)
    {
        SampleOrder order = orderRepository.findById(orderId).orElse(null);
        if (order == null)
        {
            throw new ServiceException("订单不存在");
        }
        if (!"0".equals(order.getStatus()) && !"3".equals(order.getStatus()))
        {
            throw new ServiceException("仅待提交或已驳回的订单可提交审批");
        }
        String firstNode = workflowService.firstNode(PROCESS_CODE);
        FlowTask active = workflowService.activeTask(orderId.toString(), PROCESS_CODE);
        if (active != null && "submit".equals(active.getNodeCode()))
        {
            // 驳回后重新提交：完成提交节点待办，推进到 audit
            workflowService.completeTask(active.getTaskId(), operator, "重新提交审批");
        }
        else if (active == null)
        {
            // 首次提交：发起实例后立即完成提交节点，待办落到 audit
            FlowTask startTask = workflowService.start(PROCESS_CODE, orderId.toString(), firstNode, operator);
            workflowService.completeTask(startTask.getTaskId(), operator, "提交审批");
        }
        else
        {
            throw new ServiceException("订单已在审批流程中");
        }
        updateRow(orderId, "1", null, operator);
        return "已提交审批";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String audit(Long orderId, boolean pass, String remark, String operator)
    {
        FlowTask task = workflowService.activeTask(orderId.toString(), PROCESS_CODE);
        if (task == null || !"audit".equals(task.getNodeCode()))
        {
            throw new ServiceException("当前无待审批任务");
        }
        if (pass)
        {
            FlowTask next = workflowService.completeTask(task.getTaskId(), operator, remark);
            updateRow(orderId, next == null ? "2" : "1", remark, operator);
            return "审批通过，订单已办结";
        }
        // 相邻退回：audit -> submit，状态置为已驳回，允许修改后重新提交
        workflowService.returnToPrevious(task.getTaskId(), operator, remark);
        updateRow(orderId, "3", remark, operator);
        return "已驳回提交人";
    }

    @Override
    public List<FlowHistory> flowHistory(Long orderId)
    {
        return workflowService.history(orderId.toString(), PROCESS_CODE);
    }

    /** IDENTITY 主键：save 即持久化并回填 orderId */
    private int insertRow(SampleOrder order)
    {
        orderRepository.save(order);
        return 1;
    }

    /** 定向状态更新（审批流推进/退回；对齐原 updateStatus） */
    private void updateRow(Long orderId, String status, String auditRemark, String updateBy)
    {
        orderRepository.findById(orderId).ifPresent(order -> {
            order.setStatus(status);
            order.setAuditRemark(auditRemark);
            order.setUpdateBy(updateBy);
            orderRepository.save(order);
        });
    }

    /** 动态条件（对齐原 SampleOrderMapper.xml selectList） */
    private Specification<Object> toSpec(SampleOrder query)
    {
        if (query == null)
        {
            return JpaSpecs.alwaysTrue();
        }
        return JpaSpecs.likeIf("orderNo", query.getOrderNo())
                .and(JpaSpecs.eqIf("customerId", query.getCustomerId()))
                .and(JpaSpecs.eqIfNotBlank("status", query.getStatus()));
    }
}
