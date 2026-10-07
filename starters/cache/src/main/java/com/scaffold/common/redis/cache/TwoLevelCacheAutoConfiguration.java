package com.scaffold.common.redis.cache;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.scaffold.common.redis.service.RedisService;

/**
 * 两级缓存自动装配（starter 化）：scaffold.cache.enabled=false 可关闭。
 * 服务已有自定义 CacheManager 时（@ConditionalOnMissingBean）自动让位。
 *
 * @author ct
 */
@AutoConfiguration
@ConditionalOnClass(Caffeine.class)
@ConditionalOnProperty(prefix = "scaffold.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableCaching
@EnableConfigurationProperties(TwoLevelCacheProperties.class)
public class TwoLevelCacheAutoConfiguration
{
    private static final Logger log = LoggerFactory.getLogger(TwoLevelCacheAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean(CacheManager.class)
    public TwoLevelCacheManager cacheManager(RedisService redisService,
                                             StringRedisTemplate stringRedisTemplate,
                                             TwoLevelCacheProperties properties)
    {
        return new TwoLevelCacheManager(redisService, stringRedisTemplate, properties);
    }

    /**
     * L1 失效广播监听：任一实例 put/evict/clear 后，其他实例立刻失效对应本地缓存
     */
    @Bean
    public RedisMessageListenerContainer twoLevelCacheInvalidationContainer(
            RedisConnectionFactory connectionFactory,
            ObjectProvider<TwoLevelCacheManager> cacheManager)
    {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener((message, pattern) -> {
            try
            {
                String body = new String(message.getBody(), StandardCharsets.UTF_8);
                int split = body.indexOf('|');
                if (split <= 0)
                {
                    return;
                }
                TwoLevelCacheManager manager = cacheManager.getIfAvailable();
                if (manager != null)
                {
                    manager.invalidateLocal(body.substring(0, split), body.substring(split + 1));
                }
            }
            catch (Exception e)
            {
                log.warn("[两级缓存] 失效广播处理异常: {}", e.getMessage());
            }
        }, new ChannelTopic(TwoLevelCacheManager.INVALIDATE_CHANNEL));
        return container;
    }
}
