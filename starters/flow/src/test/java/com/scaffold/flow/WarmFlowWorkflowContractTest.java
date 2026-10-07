// TODO(P2, flow-engine-jpa)：warm-flow-mybatis 适配在 mybatis-spring-boot 统一到 3.0.5 后
// 装配行为改变（Maven 时代的 4.0.1 混搭本身错配），P2 换 warm-flow-jpa 适配器后恢复默认执行；
// 当前以 @Tag 从默认 test 任务排除（见 build.gradle excludes），原因与计划见 AGENTS.md/任务清单。
package com.scaffold.flow;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;

import com.scaffold.contract.ContractFlowContributor;
import com.scaffold.flow.api.WorkflowService;
import com.scaffold.flow.config.WorkflowAutoConfiguration;

import jakarta.annotation.Resource;

/**
 * Warm-Flow 1.8.9 生产实现的 SPI 契约测试：
 * H2(MODE=MySQL) + Warm-Flow MP starter 真实引擎，逐条验证 WorkflowContractTest 契约
 * （契约流程经 ContractFlowContributor 注入，与 native 实现同一定义同一套用例）。
 *
 * @author ct
 */
@Tag("warmflow-engine")
@SpringBootTest(classes = WarmFlowWorkflowContractTest.App.class, properties = {
        "flow.engine=warmflow",
        "spring.cloud.compatibility-verifier.enabled=false",
        "spring.cloud.nacos.config.enabled=false",
        "spring.cloud.nacos.config.import-check.enabled=false",
        "spring.config.import=optional:configserver:",
        "spring.datasource.url=jdbc:h2:mem:flowtest;MODE=MySQL;DB_CLOSE_DELAY=-1",
        // warm-flow-mybatis-plus 传递引入 dynamic-datasource：测试为单 H2，
        // 给动态路由配一个指向同一 H2 的 master，避免其找不到数据源阻塞启动
        "spring.datasource.dynamic.primary=master",
        "spring.datasource.dynamic.strict=false",
        "spring.datasource.dynamic.datasource.master.url=jdbc:h2:mem:flowtest;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.dynamic.datasource.master.driver-class-name=org.h2.Driver",
        // 测试环境无 Redis：排除 Redis 自动装配，业务锁退 JVM 路径
        // （FlowAutoConfig 必须保留——它负责初始化 Warm-Flow 引擎本体）
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.sql.init.schema-locations=classpath:flow/warmflow-schema-h2.sql",
        "spring.sql.init.mode=always"
})
class WarmFlowWorkflowContractTest extends WorkflowContractTest
{
    @Resource
    private WorkflowService warmflowService;

    @Override
    protected WorkflowService newService()
    {
        return warmflowService;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = { DataSourceTransactionManagerAutoConfiguration.class })
    @ComponentScan("com.scaffold.flow")
    // ContractFlowContributor 位于 com.scaffold.contract（扫描包之外），显式导入注册一次
    @org.springframework.context.annotation.Import({ WorkflowAutoConfiguration.class,
            ContractFlowContributor.class })
    static class App
    {
    }
}
