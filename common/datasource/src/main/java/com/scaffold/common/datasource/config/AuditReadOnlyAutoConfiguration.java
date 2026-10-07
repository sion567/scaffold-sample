package com.scaffold.common.datasource.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;
import com.scaffold.common.datasource.dubbo.AuditReadOnlyFilter;

/**
 * 审核人强制只读配置（可选等保能力，未配置即关闭）。
 *
 * <p>仅当配置了 {@code scaffold.audit.readonly-ds}（只读数据源 key）时本配置才装配，
 * 并注入 Dubbo PROVIDER 端 {@link AuditReadOnlyFilter} 的生效开关与数据源 key；
 * 未配置时切面默认关闭，所有调用走默认数据源路由。</p>
 *
 * <p>通过 META-INF/spring/...AutoConfiguration.imports 注册，
 * 凡依赖 scaffold-common-datasource 的服务自动获得该开关能力。</p>
 *
 * @author scaffold
 */
@Configuration
@ConditionalOnProperty("scaffold.audit.readonly-ds")
public class AuditReadOnlyAutoConfiguration
{
    /** 只读数据源 key（如 audit：app_aud 账号，仅 SELECT） */
    @Value("${scaffold.audit.readonly-ds}")
    private String readOnlyDs;

    @PostConstruct
    public void init()
    {
        AuditReadOnlyFilter.enable(readOnlyDs);
    }
}
