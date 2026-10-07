package com.scaffold.sample.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.scaffold.common.redis.lock.DistributedLock;
import com.scaffold.sample.repository.SampleStockRepository;

/**
 * 业务定时任务样例：库存低量预警
 *
 * 演示"分布式锁防重"的业务任务写法：多实例部署时同一时刻仅一个实例执行。
 * 注意：调度仍在业务进程内（@Scheduled），生产环境需要统一管控、失败重试、
 * 执行留痕时，请改接任务中心（sys_job + 执行日志回写，见 services/job 与 job-api 契约）。
 *
 * @author scaffold
 */
@Component
public class SampleStockWarningTask
{
    private static final Logger log = LoggerFactory.getLogger(SampleStockWarningTask.class);

    @Autowired
    private DistributedLock distributedLock;

    @Autowired
    private SampleStockRepository stockRepository;

    @Scheduled(fixedDelay = 60_000)
    public void warnLowStock()
    {
        distributedLock.executeWithLock("sample:task:stock-warning", 1000, 30_000, () -> {
            int lowCount = (int) stockRepository.countByQuantityLessThan(10);
            if (lowCount > 0)
            {
                log.warn("[库存预警] 当前低库存商品 {} 件，请及时补货", lowCount);
            }
            else
            {
                log.info("[库存预警] 库存状态健康");
            }
        });
    }
}
