package com.scaffold.system.api;

import java.io.Serializable;

/**
 * 用户目录条目（站内信接收人展开等跨服务场景的轻量用户三元组）。
 *
 * <p>刻意不复用 {@code SysUser}：全字段 domain 含密码等敏感列，跨服务传输遵循最小字段集。</p>
 *
 * @author ct
 */
public class UserDirectoryItem implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 用户ID */
    private Long userId;

    /** 用户账号 */
    private String userName;

    /** 用户昵称 */
    private String nickName;

    public UserDirectoryItem()
    {
    }

    public UserDirectoryItem(Long userId, String userName, String nickName)
    {
        this.userId = userId;
        this.userName = userName;
        this.nickName = nickName;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getUserName()
    {
        return userName;
    }

    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    public String getNickName()
    {
        return nickName;
    }

    public void setNickName(String nickName)
    {
        this.nickName = nickName;
    }
}
