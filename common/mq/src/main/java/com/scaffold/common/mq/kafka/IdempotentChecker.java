package com.scaffold.common.mq.kafka;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.scaffold.common.redis.service.RedisService;

/**
 * 消息消费幂等检查器（P1-3）：Redis SETNX 占位 + 状态标记。
 * <p>
 * 状态机：tryAcquire 占位("processing", 5min) → 处理成功 markDone("done", 24h)
 * → 处理失败 markFailed(删除，允许重试)。
 * <p>
 * 推荐用法：
 * <pre>
 * String eventId = envelope.getId();          // 无信封时用业务唯一键（eventHash 等）
 * if (!idempotentChecker.tryAcquire(eventId)) return;   // 已在处理/已处理，跳过
 * try {
 *     doConsume();
 *     idempotentChecker.markDone(eventId);
 * } catch (Exception e) {
 *     idempotentChecker.markFailed(eventId);  // 允许重试/重投递
 *     throw e;
 * }
 * </pre>
 *
 * @author ct
 */
@Component
public class IdempotentChecker
{
    private static final Logger log = LoggerFactory.getLogger(IdempotentChecker.class);

    private static final String KEY_PREFIX = "scaffold:mq:idempotent:";

    private static final String PROCESSING = "processing";

    private static final String DONE = "done";

    /** 占位超时：超过视为消费实例已死亡，允许其他实例重试 */
    private static final long PROCESSING_TTL_SECONDS = 300;

    /** 完成标记保留时长：窗口内的重复投递直接跳过 */
    private static final long DONE_TTL_SECONDS = 24 * 3600;

    private final RedisService redisService;

    public IdempotentChecker(RedisService redisService)
    {
        this.redisService = redisService;
    }

    /**
     * 占位：true=本次可以处理；false=正在被处理或已处理完成，跳过
     */
    public boolean tryAcquire(String eventId)
    {
        return redisService.setIfAbsent(key(eventId), PROCESSING, PROCESSING_TTL_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * 是否已成功处理过（DONE 窗口内）
     */
    public boolean isProcessed(String eventId)
    {
        return DONE.equals(redisService.getCacheObject(key(eventId)));
    }

    /**
     * 处理成功：标记完成，窗口内重复投递直接跳过
     */
    public void markDone(String eventId)
    {
        redisService.setCacheObject(key(eventId), DONE);
        redisService.expire(key(eventId), DONE_TTL_SECONDS);
    }

    /**
     * 处理失败：删除占位，允许重试或其他实例接手
     */
    public void markFailed(String eventId)
    {
        redisService.deleteObject(key(eventId));
    }

    private String key(String eventId)
    {
        return KEY_PREFIX + eventId;
    }
}
