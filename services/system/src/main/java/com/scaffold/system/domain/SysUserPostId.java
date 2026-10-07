package com.scaffold.system.domain;

import java.io.Serializable;
import java.util.Objects;

/**
 * SysUserPost 复合主键（userId + postId）。
 *
 * @author scaffold
 */
public class SysUserPostId implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Long userId;

    private Long postId;

    public SysUserPostId()
    {
    }

    public SysUserPostId(Long userId, Long postId)
    {
        this.userId = userId;
        this.postId = postId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public Long getPostId()
    {
        return postId;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof SysUserPostId that))
        {
            return false;
        }
        return Objects.equals(userId, that.userId) && Objects.equals(postId, that.postId);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(userId, postId);
    }
}
