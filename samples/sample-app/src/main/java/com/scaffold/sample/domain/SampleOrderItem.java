package com.scaffold.sample.domain;

import java.io.Serializable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.math.BigDecimal;

/**
 * 样例订单明细对象 SAMPLE_ORDER_ITEM（主子表案例的子表）
 *
 * @author scaffold
 */
@Entity
@Table(name = "SAMPLE_ORDER_ITEM")
public class SampleOrderItem implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 明细ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long itemId;

    /** 所属订单ID */
    private Long orderId;

    /** 商品名称 */
    private String productName;

    /** 数量 */
    private Integer quantity;

    /** 单价 */
    private BigDecimal price;

    public void setItemId(Long itemId) { this.itemId = itemId; }
    public Long getItemId() { return itemId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getOrderId() { return orderId; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getProductName() { return productName; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Integer getQuantity() { return quantity; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getPrice() { return price; }
}
