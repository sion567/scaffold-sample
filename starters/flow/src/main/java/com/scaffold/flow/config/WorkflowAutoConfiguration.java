package com.scaffold.flow.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.scaffold.flow.api.WorkflowDefinitionContributor;
import com.scaffold.flow.api.WorkflowService;
import com.scaffold.flow.impl.NativeWorkflowService;
import com.scaffold.flow.warmflow.WarmFlowWorkflowService;

/**
 * 工作流引擎自动装配（真正的 starter 语义，不依赖宿主扫描 com.scaffold.flow 包）：
 * <ul>
 * <li>flow.engine 未配置或 =native：内存兜底实现（开发/联调，重启即失）；</li>
 * <li>flow.engine=warmflow：Warm-Flow 1.8.9 生产实现（需接入方 DataSource +
 *     flow_ 官方表 DDL；可选注入 StringRedisTemplate 做集群防重锁）；</li>
 * <li>流程定义不内置：接入方声明 {@link WorkflowDefinitionContributor} Bean 即可把自己的
 *     流程定义注入当前引擎（无贡献者时注册表为空，发起未注册流程抛 FlowException）；</li>
 * <li>已有 WorkflowService Bean（接入方自定义）时不重复注册。</li>
 * <li>native 模式宿主须在 application 排除 Warm-Flow 装配（其 FlowAutoConfig 在
 *     无 DataSource 上下文必然失败，且存在 WarmFlowProperties 双注册冲突）：
 *     {@code spring.autoconfigure.exclude=org.dromara.warm.flow.spring.boot.config.FlowAutoConfig}；
 *     warmflow 模式无需此行。</li>
 * </ul>
 *
 * @author ct
 */
@AutoConfiguration
public class WorkflowAutoConfiguration
{
    /**
     * native 模式下摘除 Warm-Flow 自身的 FlowAutoConfig——其 SB2 starter imports 在
     * 无 DataSource 上下文必然失败（WarmFlowProperties 双注册冲突），且会让仅依赖
     * starter 做接口隔离的宿主被迫装配引擎。关键：除配置类定义外必须连带删除其
     * @Bean 方法派生的定义（initFlow 等），否则引擎仍会实例化并失败。
     */
    public static org.springframework.beans.factory.config.BeanFactoryPostProcessor warmFlowAutoConfigOff()
    {
        return beanFactory -> {
            if (beanFactory instanceof org.springframework.beans.factory.support.DefaultListableBeanFactory lf)
            {
                String engine = lf.resolveEmbeddedValue("${flow.engine:native}");
                if (!"warmflow".equals(engine))
                {
                    String configBean = "org.dromara.warm.flow.spring.boot.config.FlowAutoConfig";
                    if (lf.containsBeanDefinition(configBean))
                    {
                        lf.removeBeanDefinition(configBean);
                        for (String name : lf.getBeanDefinitionNames())
                        {
                            if (configBean.equals(lf.getBeanDefinition(name).getFactoryBeanName()))
                            {
                                lf.removeBeanDefinition(name);
                            }
                        }
                    }
                }
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean(WorkflowService.class)
    @ConditionalOnProperty(name = "flow.engine", havingValue = "native", matchIfMissing = true)
    public NativeWorkflowService nativeWorkflowService(
            ObjectProvider<WorkflowDefinitionContributor> contributors)
    {
        return new NativeWorkflowService(contributors.orderedStream().toList());
    }

    @Bean
    @ConditionalOnMissingBean(WorkflowService.class)
    @ConditionalOnProperty(name = "flow.engine", havingValue = "warmflow")
    public WarmFlowWorkflowService warmFlowWorkflowService(ObjectProvider<StringRedisTemplate> redisTemplate,
            ObjectProvider<WorkflowDefinitionContributor> contributors)
    {
        return new WarmFlowWorkflowService(redisTemplate, contributors.orderedStream().toList());
    }
}
