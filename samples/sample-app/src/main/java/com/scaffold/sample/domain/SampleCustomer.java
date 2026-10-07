package com.scaffold.sample.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.scaffold.common.core.annotation.Excel;
import com.scaffold.common.core.web.domain.BaseEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import com.scaffold.common.sensitive.annotation.Sensitive;
import com.scaffold.common.sensitive.enums.DesensitizedType;

/**
 * 样例客户对象 SAMPLE_CUSTOMER（单表 CRUD 案例）
 *
 * @author scaffold
 */
@Entity
@Table(name = "SAMPLE_CUSTOMER")
public class SampleCustomer extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 客户ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long customerId;

    /** 客户姓名 */
    @Excel(name = "客户姓名")
    private String customerName;

    /** 手机号（返回前端时自动脱敏） */
    @Excel(name = "手机号")
    @Sensitive(desensitizedType = DesensitizedType.PHONE)
    private String phone;

    /** 邮箱 */
    @Excel(name = "邮箱")
    private String email;

    /** 状态（0正常 1停用） */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getCustomerName() { return customerName; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getPhone() { return phone; }
    public void setEmail(String email) { this.email = email; }
    public String getEmail() { return email; }
    public void setStatus(String status) { this.status = status; }
    public String getStatus() { return status; }

    @Override
    public String toString() { return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("customerId", customerId).append("customerName", customerName).toString(); }
}
