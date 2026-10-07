package com.scaffold.sample.domain;

import java.io.Serializable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.scaffold.common.core.annotation.Excel;

/**
 * 样例库存对象 SAMPLE_STOCK（单表 CRUD 案例）
 *
 * @author scaffold
 */
@Entity
@Table(name = "SAMPLE_STOCK")
public class SampleStock implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 库存ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long stockId;

    /** 商品名称 */
    @Excel(name = "商品名称")
    private String productName;

    /** 分类ID */
    @Excel(name = "分类ID")
    private Long categoryId;

    /** 库存数量 */
    @Excel(name = "库存数量")
    private Integer quantity;

    /** 仓库 */
    @Excel(name = "仓库")
    private String warehouse;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    /** 更新人 */
    private String updateBy;

    public void setStockId(Long stockId) { this.stockId = stockId; }
    public Long getStockId() { return stockId; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getProductName() { return productName; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public Long getCategoryId() { return categoryId; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Integer getQuantity() { return quantity; }
    public void setWarehouse(String warehouse) { this.warehouse = warehouse; }
    public String getWarehouse() { return warehouse; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateBy(String updateBy) { this.updateBy = updateBy; }
    public String getUpdateBy() { return updateBy; }
}
