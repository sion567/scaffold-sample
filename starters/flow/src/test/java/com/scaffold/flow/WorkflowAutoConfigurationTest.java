// TODO(P2, flow-engine-jpa)：warm-flow-mybatis 适配在 mybatis-spring-boot 统一到 3.0.5 后
// 装配行为改变（Maven 时代的 4.0.1 混搭本身错配），P2 换 warm-flow-jpa 适配器后恢复默认执行；
// 当前以 @Tag 从默认 test 任务排除（见 build.gradle excludes），原因与计划见 AGENTS.md/任务清单。
package com.scaffold.flow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import com.scaffold.flow.api.WorkflowService;
import com.scaffold.flow.config.WorkflowAutoConfiguration;
import com.scaffold.flow.impl.NativeWorkflowService;
import com.scaffold.flow.warmflow.WarmFlowWorkflowService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 自动装配验证（「通电」修复）：宿主不扫描 com.scaffold.flow 包时，
 * AutoConfiguration.imports 仍应注册 WorkflowService（默认 native）。
 *
 * @author ct
 */
@Tag("warmflow-engine")
@SpringBootTest(classes = WorkflowAutoConfigurationTest.App.class, properties = {
        "spring.cloud.nacos.config.enabled=false",
        "spring.cloud.nacos.config.import-check.enabled=false",
        // native 模式排除 Warm-Flow 引擎装配（无 DataSource 上下文其 FlowAutoConfig 必然失败）
        "spring.autoconfigure.exclude=org.dromara.warm.flow.spring.boot.config.FlowAutoConfig"
})
class WorkflowAutoConfigurationTest
{
    @SpringBootApplication
    @org.springframework.context.annotation.Import(WorkflowAutoConfiguration.class)
    static class App
    {
    }

    @Autowired
    private ApplicationContext context;

    @Test
    @DisplayName("默认注册 native 实现，且不注册 warmflow 实现")
    void native_registered_by_default()
    {
        WorkflowService service = context.getBean(WorkflowService.class);
        assertNotNull(service);
        assertTrue(service instanceof NativeWorkflowService);
        // native 模式下 Warm-Flow 引擎自动装配必须被摘除（否则无 DataSource 上下文启动失败）
        assertFalse(context.containsBean("org.dromara.warm.flow.spring.boot.config.FlowAutoConfig"),
                "FlowAutoConfig 应被 WarmFlowAutoConfigOff 摘除");
        assertEquals(0, context.getBeanNamesForType(
                org.dromara.warm.flow.core.config.WarmFlow.class).length);
        assertFalse(context.getBeanNamesForType(WarmFlowWorkflowService.class).length > 0,
                "flow.engine 未配置为 warmflow 时不应注册生产实现");
    }
}
