package com.scaffold.system.service.impl;

import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.scaffold.common.core.constant.UserConstants;
import com.scaffold.common.core.exception.ServiceException;
import com.scaffold.common.core.jpa.JpaSpecs;
import com.scaffold.common.core.utils.StringUtils;
import com.scaffold.system.domain.SysPost;
import com.scaffold.system.repository.SysPostRepository;
import com.scaffold.system.repository.SysUserPostRepository;
import com.scaffold.system.service.ISysPostService;

/**
 * 岗位信息 服务层处理（JPA：Repository + Specification）
 *
 * @author ct
 */
@Service
public class SysPostServiceImpl implements ISysPostService
{
    private final SysPostRepository postRepository;
    private final SysUserPostRepository userPostRepository;

    public SysPostServiceImpl(SysPostRepository postRepository, SysUserPostRepository userPostRepository)
    {
        this.postRepository = postRepository;
        this.userPostRepository = userPostRepository;
    }

    /**
     * 查询岗位信息集合（postCode/postName 模糊、status 等值）
     *
     * @param post 岗位信息
     * @return 岗位信息集合
     */
    @Override
    public List<SysPost> selectPostList(SysPost post)
    {
        if (post == null)
        {
            post = new SysPost();
        }
        Specification<?> spec = JpaSpecs.likeIf("postCode", post.getPostCode())
                .and(JpaSpecs.eqIfNotBlank("status", post.getStatus()))
                .and(JpaSpecs.likeIf("postName", post.getPostName()));
        return postRepository.list(spec);
    }

    @Override
    public com.scaffold.common.core.web.page.TableDataInfo selectPostPage(SysPost post, com.scaffold.common.core.web.page.PageDomain page)
    {
        if (post == null)
        {
            post = new SysPost();
        }
        Specification<?> spec = JpaSpecs.likeIf("postCode", post.getPostCode())
                .and(JpaSpecs.eqIfNotBlank("status", post.getStatus()))
                .and(JpaSpecs.likeIf("postName", post.getPostName()));
        return com.scaffold.common.core.web.page.TableDataInfo.from(postRepository.page(spec,
                com.scaffold.common.core.utils.PageUtils.toPageRequest(page)));
    }

    /**
     * 查询所有岗位
     *
     * @return 岗位列表
     */
    @Override
    public List<SysPost> selectPostAll()
    {
        return postRepository.findAll();
    }

    /**
     * 通过岗位ID查询岗位信息
     *
     * @param postId 岗位ID
     * @return 角色对象信息
     */
    @Override
    public SysPost selectPostById(Long postId)
    {
        return postRepository.findById(postId).orElse(null);
    }

    /**
     * 根据用户ID获取岗位选择框列表
     *
     * @param userId 用户ID
     * @return 选中岗位ID列表
     */
    @Override
    public List<Long> selectPostListByUserId(Long userId)
    {
        return postRepository.selectPostIdsByUserId(userId);
    }

    /**
     * 校验岗位名称是否唯一
     *
     * @param post 岗位信息
     * @return 结果
     */
    @Override
    public boolean checkPostNameUnique(SysPost post)
    {
        Long postId = StringUtils.isNull(post.getPostId()) ? -1L : post.getPostId();
        SysPost info = postRepository.findByPostName(post.getPostName()).orElse(null);
        if (StringUtils.isNotNull(info) && info.getPostId().longValue() != postId.longValue())
        {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 校验岗位编码是否唯一
     *
     * @param post 岗位信息
     * @return 结果
     */
    @Override
    public boolean checkPostCodeUnique(SysPost post)
    {
        Long postId = StringUtils.isNull(post.getPostId()) ? -1L : post.getPostId();
        SysPost info = postRepository.findByPostCode(post.getPostCode()).orElse(null);
        if (StringUtils.isNotNull(info) && info.getPostId().longValue() != postId.longValue())
        {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 通过岗位ID查询岗位使用数量
     *
     * @param postId 岗位ID
     * @return 结果
     */
    @Override
    public int countUserPostById(Long postId)
    {
        return (int) userPostRepository.countByPostId(postId);
    }

    /**
     * 删除岗位信息
     *
     * @param postId 岗位ID
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deletePostById(Long postId)
    {
        postRepository.deleteById(postId);
        return 1;
    }

    /**
     * 批量删除岗位信息
     *
     * @param postIds 需要删除的岗位ID
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deletePostByIds(Long[] postIds)
    {
        for (Long postId : postIds)
        {
            SysPost post = selectPostById(postId);
            if (countUserPostById(postId) > 0)
            {
                throw new ServiceException(String.format("%1$s已分配,不能删除", post.getPostName()));
            }
        }
        postRepository.deleteAllById(java.util.Arrays.asList(postIds));
        return postIds.length;
    }

    /**
     * 新增保存岗位信息
     *
     * @param post 岗位信息
     * @return 结果
     */
    @Override
    public int insertPost(SysPost post)
    {
        postRepository.save(post);
        return 1;
    }

    /**
     * 修改保存岗位信息
     *
     * @param post 岗位信息
     * @return 结果
     */
    @Override
    public int updatePost(SysPost post)
    {
        postRepository.save(post);
        return 1;
    }
}
