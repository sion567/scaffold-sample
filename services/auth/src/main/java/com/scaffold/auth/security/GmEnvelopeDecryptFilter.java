package com.scaffold.auth.security;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;

import com.scaffold.auth.security.GmEnvelopeDecryptor.EnvelopeRejectException;
import com.scaffold.common.core.domain.R;

/**
 * 国密信封解密 Dubbo provider 过滤器（SPI，经 META-INF/dubbo/org.apache.dubbo.rpc.Filter 注册）。
 * <p>
 * 对识别出的 GmEnvelopeBody 参数完成防重放、验签与解密并替换调用参数，随后放行；
 * 协议校验失败时以 R.fail(code, msg) 短路返回（HTTP 200 报文，前端据此做
 * 40006/40003 重新签发签名钥并重放）。未绑定解密器（scaffold.auth.gm-envelope.enabled=false，
 * 默认）或调用参数不含信封 DTO 时直接放行，内部服务间调用不受影响。
 * <p>
 * SPI 实例由 Dubbo ExtensionLoader 创建、不受 Spring 容器管理，真实逻辑委托给
 * GmEnvelopeDecryptor（Spring bean），经 {@link #bind} 注入静态引用。
 *
 * @author scaffold
 */
@Activate(group = CommonConstants.PROVIDER)
public class GmEnvelopeDecryptFilter implements Filter {

    private static volatile GmEnvelopeDecryptor delegate;

    /** 由 GmEnvelopeConfiguration 在装配解密器时调用（enabled=false 时不绑定） */
    public static void bind(GmEnvelopeDecryptor decryptor) {
        delegate = decryptor;
    }

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        GmEnvelopeDecryptor decryptor = delegate;
        if (decryptor == null) {
            return invoker.invoke(invocation);
        }
        try {
            Object[] args = decryptor.decryptArguments(invocation);
            if (args != null && invocation instanceof RpcInvocation rpcInvocation) {
                rpcInvocation.setArguments(args);
            }
            return invoker.invoke(invocation);
        } catch (EnvelopeRejectException e) {
            AppResponse response = new AppResponse();
            response.setValue(R.fail(e.getCode(), e.getMessage()));
            return response;
        }
    }
}
