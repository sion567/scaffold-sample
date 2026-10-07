package com.scaffold.flow.api;

/**
 * 工作流 SPI 业务异常（状态守卫/参数校验统一抛出）
 *
 * @author ct
 */
public class FlowException extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    public FlowException(String message)
    {
        super(message);
    }

    public FlowException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
