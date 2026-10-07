package com.scaffold.sample.domain;

import java.math.BigDecimal;
import java.util.List;
import com.scaffold.common.core.annotation.Excel;
import com.scaffold.common.core.web.domain.BaseEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;


/**
 * 样例订单对象 SAMPLE_ORDER（主子表 + 工作流案例）
 *
 * @author scaffold
 */
@Entity
@Table(name = "SAMPLE_ORDER")
public class SampleOrder extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 订单ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

    /** 订单号 */
    @Excel(name = "订单号")
    private String orderNo;

    /** 客户ID */
    @Excel(name = "客户ID")
    private Long customerId;

    /** 订单总额 */
    @Excel(name = "订单总额")
    private BigDecimal totalAmount;

    /** 状态（0待提交 1审批中 2已通过 3已驳回） */
    @Excel(name = "状态", readConverterExp = "0=待提交,1=审批中,2=已通过,3=已驳回")
    private String status;

    /** 审批意见 */
    private String auditRemark;

    /** 订单明细（子表） */
    @Transient
    private List<SampleOrderItem> items;

    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getOrderId() { return orderId; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public String getOrderNo() { return orderNo; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public Long getCustomerId() { return customerId; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setStatus(String status) { this.status = status; }
    public String getStatus() { return status; }
    public void setAuditRemark(String auditRemark) { this.auditRemark = auditRemark; }
    public String getAuditRemark() { return auditRemark; }
    public List<SampleOrderItem> getItems() { return items; }
    public void setItems(List<SampleOrderItem> items) { this.items = items; }
}
