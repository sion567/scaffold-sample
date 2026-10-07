package com.scaffold.common.security.dubbo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcContextAttachment;
import org.apache.dubbo.rpc.RpcServiceContext;
import org.apache.dubbo.rpc.RpcException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.scaffold.common.core.constant.CacheConstants;
import com.scaffold.common.core.constant.SecurityConstants;
import com.scaffold.common.core.context.SecurityContextHolder;
import com.scaffold.common.core.utils.SpringUtils;
import com.scaffold.common.redis.service.RedisService;
import com.scaffold.system.api.model.LoginUser;

/**
 * SecurityContextFilter 纯 dubbo 入口单测：附件恢复用户三件套 + 按 userKey 反查 Redis 重建 LOGIN_USER。
 *
 * <p>Triple REST 入口依赖 HttpRequest/AuthUtil 链路，不在本单测范围。</p>
 *
 * @author scaffold
 */
class SecurityContextFilterTest
{
    private final SecurityContextFilter filter = new SecurityContextFilter();

    private final Invoker<Object> invoker = mock(Invoker.class);

    private final Invocation invocation = mock(Invocation.class);

    private final Result result = mock(Result.class);

    private final RedisService redisService = mock(RedisService.class);

    private MockedStatic<RpcContext> rpcContext;

    private MockedStatic<SpringUtils> springUtils;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp()
    {
        RpcServiceContext serviceContext = mock(RpcServiceContext.class);
        when(serviceContext.getRequest(any())).thenReturn(null);

        rpcContext = Mockito.mockStatic(RpcContext.class);
        rpcContext.when(RpcContext::getServiceContext).thenReturn(serviceContext);

        springUtils = Mockito.mockStatic(SpringUtils.class);
        springUtils.when(() -> SpringUtils.getBean(RedisService.class)).thenReturn(redisService);
    }

    @AfterEach
    void tearDown()
    {
        SecurityContextHolder.remove();
        rpcContext.close();
        springUtils.close();
    }

    /** 附件打桩：纯 dubbo 入口的服务端附件 */
    private void attach(String userId, String username, String userKey)
    {
        RpcContextAttachment serverAttachment = mock(RpcContextAttachment.class);
        when(serverAttachment.getAttachment(SecurityConstants.DETAILS_USER_ID)).thenReturn(userId);
        when(serverAttachment.getAttachment(SecurityConstants.DETAILS_USERNAME)).thenReturn(username);
        when(serverAttachment.getAttachment(SecurityConstants.USER_KEY)).thenReturn(userKey);
        rpcContext.when(RpcContext::getServerAttachment).thenReturn(serverAttachment);
    }

    /** 在 invoker.invoke 执行期间（上下文已恢复）捕获 LOGIN_USER 与 userId */
    private Object[] captureDuringInvoke()
    {
        Object[] captured = new Object[2];
        when(invoker.invoke(any())).thenAnswer(inv -> {
            captured[0] = SecurityContextHolder.get(SecurityConstants.LOGIN_USER, LoginUser.class);
            captured[1] = SecurityContextHolder.getUserId();
            return result;
        });
        return captured;
    }

    @Test
    @DisplayName("userKey 命中 Redis 会话 → LOGIN_USER 恢复，业务执行期间可见，结束后清理")
    void restoresLoginUserFromRedis()
    {
        attach("1", "admin", "uk-123");
        LoginUser loginUser = new LoginUser();
        when(redisService.getCacheObject(CacheConstants.LOGIN_TOKEN_KEY + "uk-123")).thenReturn(loginUser);
        Object[] captured = captureDuringInvoke();

        filter.invoke(invoker, invocation);

        assertSame(loginUser, captured[0]);
        assertEquals(1L, captured[1]);
        assertNull(SecurityContextHolder.get(SecurityConstants.LOGIN_USER, LoginUser.class));
    }

    @Test
    @DisplayName("userKey 为空（INNER 无登录态）→ 不查 Redis，LOGIN_USER 保持为空")
    void skipsRedisWithoutUserKey()
    {
        attach("1", null, null);

        filter.invoke(invoker, invocation);

        verifyNoInteractions(redisService);
        assertNull(SecurityContextHolder.get(SecurityConstants.LOGIN_USER, LoginUser.class));
    }

    @Test
    @DisplayName("Redis 异常 → 降级为无登录态，调用不打断")
    void degradesOnRedisFailure()
    {
        attach("1", "admin", "uk-404");
        when(redisService.getCacheObject(CacheConstants.LOGIN_TOKEN_KEY + "uk-404"))
                .thenThrow(new RuntimeException("redis down"));
        Object[] captured = captureDuringInvoke();

        Result actual = filter.invoke(invoker, invocation);

        assertSame(result, actual);
        assertNull(captured[0]);
        assertEquals(1L, captured[1]);
    }
}
