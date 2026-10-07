package com.scaffold.common.core.jpa;

import java.util.Optional;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.context.annotation.Bean;

import com.scaffold.common.core.context.SecurityContextHolder;
import com.scaffold.common.core.utils.StringUtils;

/**
 * JPA 审计自动装配：开启 {@code @CreatedBy/@CreatedDate/@LastModifiedBy/@LastModifiedDate}
 * 自动填充（BaseEntity 上的注解依赖本装配生效）。
 *
 * <p>审计人取 {@link SecurityContextHolder#getUserName()}——网关/HeaderInterceptor 透传的
 * 当前用户；上下文缺席（RPC 服务方线程、调度线程、启动阶段）返回 empty，不覆盖业务代码
 * 手动 set 的值。</p>
 *
 * <p>仅在容器中存在 {@link jakarta.persistence.EntityManagerFactory} 时装配
 * （排在 Hibernate 自动装配之后）：无库服务（auth/gateway/file 等不直连数据库）
 * 排除了 HibernateJpaAutoConfiguration，本装配随之退避——否则 @EnableJpaAuditing
 * 的 jpaMappingContext 会因 "JPA metamodel must not be empty" 拖垮启动。</p>
 *
 * @author ct
 */
@AutoConfiguration
@AutoConfigureAfter(HibernateJpaAutoConfiguration.class)
@ConditionalOnBean(jakarta.persistence.EntityManagerFactory.class)
@EnableJpaAuditing(auditorAwareRef = "scaffoldAuditorProvider")
public class JpaAuditingAutoConfiguration
{
    @Bean
    public AuditorAware<String> scaffoldAuditorProvider()
    {
        return () -> {
            String username = SecurityContextHolder.getUserName();
            return StringUtils.isEmpty(username) ? Optional.empty() : Optional.of(username);
        };
    }
}
