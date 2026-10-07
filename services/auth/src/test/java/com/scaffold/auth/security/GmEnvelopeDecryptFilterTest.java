package com.scaffold.auth.security;

import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcInvocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.scaffold.auth.form.LoginBody;
import com.scaffold.auth.security.GmEnvelopeDecryptor.EnvelopeRejectException;
import com.scaffold.common.core.domain.R;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * GmEnvelopeDecryptFilter（Dubbo SPI 壳）测试：委托/短路/换参三类行为。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GmEnvelopeDecryptFilterTest
{
    private final GmEnvelopeDecryptFilter filter = new GmEnvelopeDecryptFilter();

    @SuppressWarnings("unchecked")
    private final Invoker<Object> invoker = mock(Invoker.class);

    @BeforeEach
    void setUp()
    {
        when(invoker.invoke(any(Invocation.class))).thenReturn(new AppResponse("ok"));
    }

    @AfterEach
    void unbind()
    {
        GmEnvelopeDecryptFilter.bind(null);
    }

    @Test
    @DisplayName("未绑定解密器（信封关闭）：直接放行")
    void unboundPassThrough()
    {
        GmEnvelopeDecryptFilter.bind(null);
        RpcInvocation inv = new RpcInvocation();
        inv.setArguments(new Object[]{new LoginBody()});

        Result result = filter.invoke(invoker, inv);

        assertEquals("ok", result.getValue());
        verify(invoker).invoke(inv);
    }

    @Test
    @DisplayName("协议校验失败：短路返回 R.fail(code, msg)，不再调用下游")
    void rejectShortCircuits()
    {
        GmEnvelopeDecryptor decryptor = mock(GmEnvelopeDecryptor.class);
        when(decryptor.decryptArguments(any(RpcInvocation.class)))
                .thenThrow(new EnvelopeRejectException(GmEnvelopeDecryptor.CODE_REPLAY, "请求重复，请勿重复提交"));
        GmEnvelopeDecryptFilter.bind(decryptor);

        Result result = filter.invoke(invoker, new RpcInvocation());

        R<?> value = (R<?>) result.getValue();
        assertEquals(GmEnvelopeDecryptor.CODE_REPLAY, value.getCode());
        assertEquals("请求重复，请勿重复提交", value.getMsg());
        verify(invoker, never()).invoke(any());
    }

    @Test
    @DisplayName("解密成功：下游收到替换后的参数")
    void successReplacesArguments()
    {
        GmEnvelopeDecryptor decryptor = mock(GmEnvelopeDecryptor.class);
        LoginBody decrypted = new LoginBody();
        when(decryptor.decryptArguments(any(RpcInvocation.class))).thenReturn(new Object[]{decrypted});
        GmEnvelopeDecryptFilter.bind(decryptor);

        RpcInvocation inv = new RpcInvocation();
        inv.setArguments(new Object[]{new LoginBody()});

        Result result = filter.invoke(invoker, inv);

        assertSame("ok", result.getValue());
        verify(invoker).invoke(inv);
        assertEquals(decrypted, inv.getArguments()[0]);
    }
}
