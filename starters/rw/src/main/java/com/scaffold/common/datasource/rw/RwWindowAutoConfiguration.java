package com.scaffold.common.datasource.rw;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;

/**
 * 写后读一致性窗口自动装配（starter 化）：scaffold.ds.rw.enabled=false 可关闭。
 *
 * @author ct
 */
@AutoConfiguration
@ConditionalOnClass(DynamicDataSourceContextHolder.class)
@ConditionalOnProperty(prefix = "scaffold.ds.rw", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RwWindowAutoConfiguration
{
    @Bean
    public ReadWriteWindowAspect readWriteWindowAspect(
            @Value("${scaffold.ds.rw.read-after-write-seconds:2}") long readAfterWriteSeconds)
    {
        return new ReadWriteWindowAspect(readAfterWriteSeconds);
    }
}
