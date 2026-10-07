package com.scaffold.common.security.idempotent;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import com.scaffold.common.redis.service.RedisService;

/**
 * 接口幂等自动装配（starter 化）：scaffold.idempotent.enabled=false 可整体关闭
 * （注解保留在代码中但不生效）。
 *
 * @author ct
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "scaffold.idempotent", name = "enabled", havingValue = "true", matchIfMissing = true)
public class IdempotentAutoConfiguration
{
    @Bean
    public IdempotentAspect idempotentAspect(RedisService redisService)
    {
        return new IdempotentAspect(redisService);
    }
}
