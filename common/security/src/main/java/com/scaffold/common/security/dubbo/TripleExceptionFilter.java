package com.scaffold.common.security.dubbo;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.AsyncRpcResult;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.scaffold.common.core.constant.HttpStatus;
import com.scaffold.common.core.exception.DemoModeException;
import com.scaffold.common.core.exception.InnerAuthException;
import com.scaffold.common.core.exception.ServiceException;
import com.scaffold.common.core.exception.auth.NotPermissionException;
import com.scaffold.common.core.exception.auth.NotRoleException;
import com.scaffold.common.core.utils.StringUtils;
import com.scaffold.common.core.web.domain.AjaxResult;

/**
 * Dubbo 提供端过滤器（Triple REST 场景）
 * 等价于原 MVC 的 GlobalExceptionHandler：将业务/权限异常转换为前端统一的 AjaxResult JSON 结构
 *
 * @author scaffold
 */
@Activate(group = CommonConstants.PROVIDER, order = 20000)
public class TripleExceptionFilter implements Filter
{
    private static final Logger log = LoggerFactory.getLogger(TripleExceptionFilter.class);

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException
    {
        Result result = invoker.invoke(invocation);
        if (result.hasException())
        {
            Throwable e = result.getException();
            // 仅处理 Triple REST 调用（存在 HTTP 请求上下文），普通 dubbo 协议异常保持原样传递
            if (RpcContext.getServiceContext().getRequest(org.apache.dubbo.remoting.http12.HttpRequest.class) != null)
            {
                log.error("请求地址'{}',发生异常.", RpcContext.getServiceContext().getRemoteApplicationName(), e);
                AppResponse response = new AppResponse(convert(e));
                response.setObjectAttachments(result.getObjectAttachments());
                return AsyncRpcResult.newDefaultAsyncResult(response, invocation);
            }
        }
        return result;
    }

    private AjaxResult convert(Throwable e)
    {
        if (e instanceof NotPermissionException || e instanceof NotRoleException)
        {
            return AjaxResult.error(HttpStatus.FORBIDDEN, "没有访问权限，请联系管理员授权");
        }
        if (e instanceof ServiceException)
        {
            Integer code = ((ServiceException) e).getCode();
            return StringUtils.isNotNull(code) ? AjaxResult.error(code, e.getMessage()) : AjaxResult.error(e.getMessage());
        }
        if (e instanceof InnerAuthException)
        {
            return AjaxResult.error(e.getMessage());
        }
        if (e instanceof DemoModeException)
        {
            return AjaxResult.error("演示模式，不允许操作");
        }
        if (e instanceof IllegalArgumentException)
        {
            // REST 参数绑定失败（类型不匹配、缺少路径参数等），向前端提示原始原因
            return AjaxResult.error(e.getMessage());
        }
        // 未预期异常不向前端透出内部细节（可能含类名/SQL 等），完整堆栈已在上方日志（带 traceId）
        return AjaxResult.error("系统内部错误，请稍后重试");
    }
}
