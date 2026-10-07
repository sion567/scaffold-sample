package com.scaffold.common.datasource.rw;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;

/**
 * 读写分离写后读一致性切面（P2-6，借鉴 CPF ReadWriteRoutingContext）：
 * <ul>
 * <li>@Master 写方法成功返回后登记写时间；处于事务时注册 afterCommit 回调——
 *     在"真正对从库可见之前"才开始计时；</li>
 * <li>窗口内的 @Slave 读通过 DynamicDataSourceContextHolder 临时压入 master 数据源，
 *     执行完弹出，不影响既有路由栈。</li>
 * </ul>
 * 说明：未标注 @Master 的普通写不登记（无法穷举写路径），需要强一致保障的读请在写方法上补 @Master。
 *
 * @author ct
 */
@Aspect
public class ReadWriteWindowAspect
{
    private static final Logger log = LoggerFactory.getLogger(ReadWriteWindowAspect.class);

    private static final String MASTER = "master";

    private final long windowMillis;

    public ReadWriteWindowAspect(long readAfterWriteSeconds)
    {
        this.windowMillis = readAfterWriteSeconds * 1000L;
    }

    @Around("@annotation(com.scaffold.common.datasource.annotation.Master)")
    public Object aroundMaster(ProceedingJoinPoint point) throws Throwable
    {
        Object result = point.proceed();
        markWriteAfterCommit();
        return result;
    }

    @Around("@annotation(com.scaffold.common.datasource.annotation.Slave)")
    public Object aroundSlave(ProceedingJoinPoint point) throws Throwable
    {
        boolean forceMaster = ReadWriteWindowContext.isInWindow(windowMillis);
        if (forceMaster)
        {
            log.debug("[读写分离] 处于写后读窗口，@Slave 读回主库: {}", point.getSignature().toShortString());
            DynamicDataSourceContextHolder.push(MASTER);
        }
        try
        {
            return point.proceed();
        }
        finally
        {
            if (forceMaster)
            {
                DynamicDataSourceContextHolder.poll();
            }
        }
    }

    /**
     * 有事务时等 afterCommit 再登记（避免"事务未提交、窗口已开"的假一致），
     * 无事务直接登记
     */
    private void markWriteAfterCommit()
    {
        if (TransactionSynchronizationManager.isSynchronizationActive())
        {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization()
            {
                @Override
                public void afterCommit()
                {
                    ReadWriteWindowContext.markWrite();
                }
            });
        }
        else
        {
            ReadWriteWindowContext.markWrite();
        }
    }
}
