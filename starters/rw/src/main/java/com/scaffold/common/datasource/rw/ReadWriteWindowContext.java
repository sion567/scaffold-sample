package com.scaffold.common.datasource.rw;

/**
 * 写后读一致性窗口上下文（P2-6）：@Master 写成功（事务则提交后）登记时间戳，
 * 窗口内的 @Slave 读强制回主库，防止从库复制延迟读到旧值。
 * ThreadLocal 残留无害：过期时间戳不命中窗口，且仅在显式 @Slave 方法上检查。
 *
 * @author ct
 */
public final class ReadWriteWindowContext
{
    private static final ThreadLocal<Long> LAST_WRITE_AT = new ThreadLocal<>();

    private ReadWriteWindowContext()
    {
    }

    public static void markWrite()
    {
        LAST_WRITE_AT.set(System.currentTimeMillis());
    }

    public static boolean isInWindow(long windowMillis)
    {
        Long lastWriteAt = LAST_WRITE_AT.get();
        return lastWriteAt != null && System.currentTimeMillis() - lastWriteAt <= windowMillis;
    }

    public static void clear()
    {
        LAST_WRITE_AT.remove();
    }
}
