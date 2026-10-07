package com.scaffold.common.core.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import com.scaffold.common.core.utils.uuid.SnowflakeIdGenerator;

/**
 * 雪花 ID 生成器自动装配（全局唯一 Bean，@ConditionalOnMissingBean 可覆盖）：
 * 各服务经 scaffold.snowflake.worker-id / scaffold.snowflake.data-center-id 配置；
 * 多实例部署时同一服务的各实例 worker-id 必须不同（0~31）。
 *
 * @author scaffold
 */
@AutoConfiguration
public class SnowflakeIdGeneratorConfiguration
{
    @Bean
    @ConditionalOnMissingBean
    public SnowflakeIdGenerator snowflakeIdGenerator(
            @Value("${scaffold.snowflake.worker-id:1}") long workerId,
            @Value("${scaffold.snowflake.data-center-id:1}") long dataCenterId)
    {
        return new SnowflakeIdGenerator(workerId, dataCenterId);
    }
}
