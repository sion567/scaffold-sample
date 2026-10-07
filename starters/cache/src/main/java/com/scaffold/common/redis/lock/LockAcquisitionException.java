package com.scaffold.common.redis.lock;

/**
 * 分布式锁获取失败（等待超时）。调用方捕获后通常提示"操作繁忙请重试"或走降级逻辑。
 *
 * @author ct
 */
public class LockAcquisitionException extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    public LockAcquisitionException(String message)
    {
        super(message);
    }
}
