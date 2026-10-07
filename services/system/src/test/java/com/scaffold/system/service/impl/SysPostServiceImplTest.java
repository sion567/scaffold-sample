package com.scaffold.system.service.impl;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.scaffold.common.core.constant.UserConstants;
import com.scaffold.common.core.exception.ServiceException;
import com.scaffold.system.domain.SysPost;
import com.scaffold.system.repository.SysPostRepository;
import com.scaffold.system.repository.SysUserPostRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SysPostServiceImpl Mock 测试（唯一性 + 删除保护 + 基础查询）。
 *
 * <p>覆盖维度：
 * <ul>
 *   <li>基础查询：selectPostList / selectPostAll / selectPostById / selectPostListByUserId</li>
 *   <li>唯一性：checkPostNameUnique / checkPostCodeUnique</li>
 *   <li>删除保护：deletePostByIds 岗位已分配用户时禁止删除</li>
 *   <li>CRUD：insertPost / updatePost / deletePostById</li>
 * </ul>
 *
 * @author ct
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysPostServiceImplTest
{
    @Mock
    private SysPostRepository postRepository;

    @Mock
    private SysUserPostRepository userPostRepository;

    private SysPostServiceImpl postService;

    @BeforeEach
    void setUp()
    {
        postService = new SysPostServiceImpl(postRepository, userPostRepository);
    }

    // ─────────────────────────────────────────────
    // 基础查询
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("基础查询")
    class BasicQueryTests
    {
        @Test
        @DisplayName("selectPostList → 透传 mapper")
        void selectPostList_passesThrough()
        {
            when(postRepository.list(any())).thenReturn(List.of());
            postService.selectPostList(new SysPost());
            verify(postRepository).list(any());
        }

        @Test
        @DisplayName("selectPostAll → 透传 mapper")
        void selectPostAll_passesThrough()
        {
            when(postRepository.findAll()).thenReturn(List.of());
            assertNotNull(postService.selectPostAll());
        }

        @Test
        @DisplayName("selectPostById → 透传 mapper")
        void selectPostById_passesThrough()
        {
            SysPost post = post(1L, "CEO");
            when(postRepository.findById(1L)).thenReturn(java.util.Optional.of(post));
            assertEquals("CEO", postService.selectPostById(1L).getPostName());
        }

        @Test
        @DisplayName("selectPostListByUserId → 透传 mapper")
        void selectPostListByUserId_passesThrough()
        {
            when(postRepository.selectPostIdsByUserId(1L)).thenReturn(List.of(10L, 20L));
            List<Long> result = postService.selectPostListByUserId(1L);
            assertEquals(2, result.size());
        }
    }

    // ─────────────────────────────────────────────
    // checkPostNameUnique — 唯一性
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("checkPostNameUnique 唯一性")
    class PostNameUniqueTests
    {
        @Test
        @DisplayName("岗位名无冲突 → UNIQUE")
        void noConflict_returnsUnique()
        {
            when(postRepository.findByPostName("CEO")).thenReturn(java.util.Optional.empty());
            assertEquals(UserConstants.UNIQUE, postService.checkPostNameUnique(post(null, "CEO")));
        }

        @Test
        @DisplayName("岗位名冲突（不同 postId）→ NOT_UNIQUE")
        void conflictDifferentId_returnsNotUnique()
        {
            when(postRepository.findByPostName("CEO")).thenReturn(java.util.Optional.of(post(2L, "CEO")));
            assertEquals(UserConstants.NOT_UNIQUE, postService.checkPostNameUnique(post(1L, "CEO")));
        }

        @Test
        @DisplayName("岗位名冲突（同 postId）→ UNIQUE（自己不算冲突）")
        void conflictSameId_returnsUnique()
        {
            when(postRepository.findByPostName("CEO")).thenReturn(java.util.Optional.of(post(1L, "CEO")));
            assertEquals(UserConstants.UNIQUE, postService.checkPostNameUnique(post(1L, "CEO")));
        }

        @Test
        @DisplayName("postId 为 null → 用 -1L 比较，无冲突 → UNIQUE")
        void nullPostId_noConflict_returnsUnique()
        {
            when(postRepository.findByPostName("新岗位")).thenReturn(java.util.Optional.empty());
            assertEquals(UserConstants.UNIQUE, postService.checkPostNameUnique(post(null, "新岗位")));
        }
    }

    // ─────────────────────────────────────────────
    // checkPostCodeUnique — 唯一性
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("checkPostCodeUnique 唯一性")
    class PostCodeUniqueTests
    {
        @Test
        @DisplayName("岗位编码无冲突 → UNIQUE")
        void noConflict_returnsUnique()
        {
            when(postRepository.findByPostCode("ceo")).thenReturn(java.util.Optional.empty());
            assertEquals(UserConstants.UNIQUE, postService.checkPostCodeUnique(postCode(null, "ceo")));
        }

        @Test
        @DisplayName("岗位编码冲突（不同 postId）→ NOT_UNIQUE")
        void conflictDifferentId_returnsNotUnique()
        {
            when(postRepository.findByPostCode("ceo")).thenReturn(java.util.Optional.of(postCode(2L, "ceo")));
            assertEquals(UserConstants.NOT_UNIQUE, postService.checkPostCodeUnique(postCode(1L, "ceo")));
        }

        @Test
        @DisplayName("岗位编码冲突（同 postId）→ UNIQUE（自己不算冲突）")
        void conflictSameId_returnsUnique()
        {
            when(postRepository.findByPostCode("ceo")).thenReturn(java.util.Optional.of(postCode(1L, "ceo")));
            assertEquals(UserConstants.UNIQUE, postService.checkPostCodeUnique(postCode(1L, "ceo")));
        }
    }

    // ─────────────────────────────────────────────
    // deletePostByIds — 分配保护
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("deletePostByIds 岗位分配保护")
    class DeletePostTests
    {
        @Test
        @DisplayName("岗位已分配用户 → ServiceException，不删除")
        void postHasUsers_throwsAndDoesNotDelete()
        {
            when(postRepository.findById(1L)).thenReturn(java.util.Optional.of(post(1L, "已分配岗")));
            when(userPostRepository.countByPostId(1L)).thenReturn(3L);

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> postService.deletePostByIds(new Long[]{1L}));

            assertTrue(ex.getMessage().contains("已分配"));
            verify(postRepository, never()).deleteAllById(any());
        }

        @Test
        @DisplayName("岗位未分配用户 → 正常删除")
        void postNoUsers_deletes()
        {
            when(postRepository.findById(2L)).thenReturn(java.util.Optional.of(post(2L, "空闲岗")));
            when(userPostRepository.countByPostId(2L)).thenReturn(0L);
            

            int rows = postService.deletePostByIds(new Long[]{2L});

            assertEquals(1, rows);
            verify(postRepository).deleteAllById(java.util.Arrays.asList(2L));
        }

        @Test
        @DisplayName("批量删除：含已分配岗位 → 第一条抛异常，后面的未处理")
        void batchWithAllocated_throwsFirst()
        {
            when(postRepository.findById(1L)).thenReturn(java.util.Optional.of(post(1L, "已分配")));
            when(userPostRepository.countByPostId(1L)).thenReturn(1L);

            assertThrows(ServiceException.class,
                    () -> postService.deletePostByIds(new Long[]{1L, 2L}));

            verify(postRepository, never()).deleteAllById(any());
        }
    }

    // ─────────────────────────────────────────────
    // countUserPostById
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("countUserPostById")
    class CountUserPostTests
    {
        @Test
        @DisplayName("有用户使用 → 返回 > 0")
        void hasUsers_returnsPositive()
        {
            when(userPostRepository.countByPostId(1L)).thenReturn(5L);
            assertEquals(5, postService.countUserPostById(1L));
        }

        @Test
        @DisplayName("无用户使用 → 返回 0")
        void noUsers_returnsZero()
        {
            when(userPostRepository.countByPostId(99L)).thenReturn(0L);
            assertEquals(0, postService.countUserPostById(99L));
        }
    }

    // ─────────────────────────────────────────────
    // CRUD — insert/update/delete
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("insertPost / updatePost / deletePostById")
    class CrudTests
    {
        @Test
        @DisplayName("insertPost → 透传 mapper")
        void insertPost_passesThrough()
        {
            when(postRepository.save(any(SysPost.class))).thenAnswer(inv -> inv.getArgument(0));
            assertEquals(1, postService.insertPost(post(null, "新岗位")));
        }

        @Test
        @DisplayName("updatePost → 透传 mapper")
        void updatePost_passesThrough()
        {
            when(postRepository.save(any(SysPost.class))).thenAnswer(inv -> inv.getArgument(0));
            assertEquals(1, postService.updatePost(post(1L, "更新岗位")));
        }

        @Test
        @DisplayName("deletePostById → 透传 mapper")
        void deletePostById_passesThrough()
        {
            
            assertEquals(1, postService.deletePostById(1L));
        }
    }

    // ─────────────────────────────────────────────
    // 辅助方法
    // ─────────────────────────────────────────────

    private SysPost post(Long postId, String postName)
    {
        SysPost p = new SysPost();
        p.setPostId(postId);
        p.setPostName(postName);
        p.setPostCode("code_" + postId);
        p.setStatus(UserConstants.NORMAL);
        return p;
    }

    private SysPost postCode(Long postId, String postCode)
    {
        SysPost p = new SysPost();
        p.setPostId(postId);
        p.setPostName("name_" + postId);
        p.setPostCode(postCode);
        p.setStatus(UserConstants.NORMAL);
        return p;
    }
}
