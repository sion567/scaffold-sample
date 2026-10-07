package com.scaffold.system.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Date;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 用户第三方绑定表 sys_user_identity（外部 ID 映射）
 *
 * 给本地 sys_user.user_id 提供一层外部账号 ID 映射，
 * 支持用第三方唯一 ID（unionid/openid/钉钉 userid/企业 SSO subject）反查本地用户。
 *
 * @author ct
 */
@Entity
@Table(name = "sys_user_identity")
public class SysUserIdentity
{
    /** 主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 本地用户ID（sys_user.user_id） */
    private Long userId;

    /** 身份源类型：local/wechat/qq/dingtalk/corp_sso */
    private String idpType;

    /** 身份源唯一ID（unionid/openid/userid/subject） */
    private String idpUid;

    /** 头像昵称等快照（JSON） */
    private String extra;

    /** 绑定时间 */
    private Date createTime;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getIdpType()
    {
        return idpType;
    }

    public void setIdpType(String idpType)
    {
        this.idpType = idpType;
    }

    public String getIdpUid()
    {
        return idpUid;
    }

    public void setIdpUid(String idpUid)
    {
        this.idpUid = idpUid;
    }

    public String getExtra()
    {
        return extra;
    }

    public void setExtra(String extra)
    {
        this.extra = extra;
    }

    public Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("userId", getUserId())
            .append("idpType", getIdpType())
            .append("idpUid", getIdpUid())
            .append("extra", getExtra())
            .append("createTime", getCreateTime())
            .toString();
    }
}
