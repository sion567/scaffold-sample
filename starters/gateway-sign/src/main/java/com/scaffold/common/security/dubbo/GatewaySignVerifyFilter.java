package com.scaffold.common.security.dubbo;

import java.util.HashMap;
import java.util.Map;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.rpc.AsyncRpcResult;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.scaffold.common.core.constant.HttpStatus;
import com.scaffold.common.core.constant.SecurityConstants;
import com.scaffold.common.core.crypto.GatewaySm3Signature;
import com.scaffold.common.core.utils.SpringUtils;
import com.scaffold.common.core.utils.StringUtils;
import com.scaffold.common.core.web.domain.AjaxResult;
import com.scaffold.common.security.gateway.GatewaySignVerifier;

/**
 * Dubbo 提供端过滤器：网关身份头签名验签（网关 GatewaySignFilter 的下游配对）。
 * <p>
 * 只对 Triple REST（有 HTTP 请求上下文）生效；纯 dubbo 协议内部调用走 InnerAuth/SecurityContext，不在此拦截。
 * servlet 承载的服务（如 scaffold-auth）与 http12 承载的服务统一在此入口，不依赖 servlet Filter——
 * Dubbo 会把两种承载下的 REST 请求桥接为同一个 HttpRequest。
 * <p>
 * 顺序：order=-9000，早于 SecurityContextFilter（默认 0），伪造身份头在封装上下文之前即被处理。
 * 验签器 Bean 未注册或未配置密钥时自动停用；默认 alert 模式灰度，观察后切 block。
 *
 * @author ct
 */
@Activate(group = CommonConstants.PROVIDER, order = -9000)
public class GatewaySignVerifyFilter implements Filter
{
    private static final Logger log = LoggerFactory.getLogger(GatewaySignVerifyFilter.class);

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException
    {
        GatewaySignVerifier verifier = getVerifier();
        HttpRequest request = RpcContext.getServiceContext().getRequest(HttpRequest.class);
        if (verifier == null || !verifier.isEnabled() || request == null)
        {
            return invoker.invoke(invocation);
        }
        String path = request.path();
        if (verifier.isSkipPath(path))
        {
            return invoker.invoke(invocation);
        }
        String sign = request.header(GatewaySm3Signature.HEADER_SIGN);
        if (StringUtils.isEmpty(sign))
        {
            // 无签名的匿名请求放行（网关白名单、内网工具直连）；
            // 但携带身份头而无签名 = 绕过网关伪造，按 INVALID 处理
            if (hasIdentityHeader(request))
            {
                return handle(verifier, invoker, invocation, path, "携带身份头但缺少网关签名");
            }
            return invoker.invoke(invocation);
        }
        GatewaySignVerifier.Result result = verifier.verify(identityMap(request),
                request.header(GatewaySm3Signature.HEADER_TIMESTAMP),
                request.header(GatewaySm3Signature.HEADER_NONCE), sign);
        switch (result)
        {
            case OK:
                return invoker.invoke(invocation);
            case REPLAY:
                return handle(verifier, invoker, invocation, path, "网关签名 nonce 重放");
            case INVALID:
            default:
                return handle(verifier, invoker, invocation, path, "网关签名无效或时间戳越窗");
        }
    }

    private Result handle(GatewaySignVerifier verifier, Invoker<?> invoker, Invocation invocation,
                          String path, String reason)
    {
        if (!verifier.isBlockMode())
        {
            log.warn("[网关验签][alert] path={}, reason={}（block 模式将拒绝）", path, reason);
            return invoker.invoke(invocation);
        }
        log.error("[网关验签][block] 拒绝请求: path={}, reason={}", path, reason);
        AppResponse response = new AppResponse(AjaxResult.error(HttpStatus.UNAUTHORIZED, "非法请求"));
        return AsyncRpcResult.newDefaultAsyncResult(response, invocation);
    }

    private boolean hasIdentityHeader(HttpRequest request)
    {
        return StringUtils.isNotEmpty(request.header(SecurityConstants.DETAILS_USER_ID))
                || StringUtils.isNotEmpty(request.header(SecurityConstants.USER_KEY))
                || StringUtils.isNotEmpty(request.header(SecurityConstants.DETAILS_USERNAME));
    }

    /**
     * 身份参数取透传原始值（username 为 urlEncode 后的值），与网关签发侧保持一致，不在此解码
     */
    private Map<String, String> identityMap(HttpRequest request)
    {
        Map<String, String> identity = new HashMap<>();
        identity.put(SecurityConstants.DETAILS_USER_ID, request.header(SecurityConstants.DETAILS_USER_ID));
        identity.put(SecurityConstants.USER_KEY, request.header(SecurityConstants.USER_KEY));
        identity.put(SecurityConstants.DETAILS_USERNAME, request.header(SecurityConstants.DETAILS_USERNAME));
        return identity;
    }

    private GatewaySignVerifier getVerifier()
    {
        try
        {
            return SpringUtils.getBean(GatewaySignVerifier.class);
        }
        catch (Exception e)
        {
            // Bean 未注册/上下文未就绪：视为停用，不影响业务
            return null;
        }
    }
}
