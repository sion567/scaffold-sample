// TODO(P2, flow-engine-jpa)：warm-flow-mybatis 适配在 mybatis-spring-boot 统一到 3.0.5 后
// 装配行为改变（Maven 时代的 4.0.1 混搭本身错配），P2 换 warm-flow-jpa 适配器后恢复默认执行；
// 当前以 @Tag 从默认 test 任务排除（见 build.gradle excludes），原因与计划见 AGENTS.md/任务清单。
package com.scaffold.flow.warmflow;

import java.util.List;

import org.dromara.warm.flow.core.dto.DefJson;
import org.dromara.warm.flow.core.dto.NodeJson;
import org.dromara.warm.flow.core.enums.NodeType;
import org.dromara.warm.flow.core.enums.SkipType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.scaffold.contract.ContractFlowContributor;
import com.scaffold.flow.api.FlowException;
import com.scaffold.flow.api.WorkflowDefinition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Warm-Flow 引擎适配测试：WorkflowDefinition → DefJson 映射、firstNode/previousNode
 * 查询、重复编码 fail-fast（契约流程复用 ContractFlowContributor）。
 *
 * @author ct
 */
@Tag("warmflow-engine")
class WarmFlowProcessDefinitionsTest
{
    private final WarmFlowProcessDefinitions definitions =
            new WarmFlowProcessDefinitions(List.of(new ContractFlowContributor()));

    @Test
    @DisplayName("已注册流程：DefJson 映射/首节点/相邻链齐备")
    void registered_processes()
    {
        assertTrue(definitions.contains(ContractFlowContributor.LINEAR));
        assertEquals("alpha", definitions.firstNode(ContractFlowContributor.LINEAR));
        assertEquals("beta", definitions.previousNode(ContractFlowContributor.LINEAR, "gamma"));
        assertNull(definitions.previousNode(ContractFlowContributor.LINEAR, "alpha"), "首节点无可退回");

        assertTrue(definitions.contains(ContractFlowContributor.GATEWAY));
        assertEquals("submit", definitions.firstNode(ContractFlowContributor.GATEWAY));
        assertEquals("submit", definitions.previousNode(ContractFlowContributor.GATEWAY, "approve"));
    }

    @Test
    @DisplayName("线性流程 DefJson：节点链与退回边完整")
    void linear_def_json()
    {
        DefJson def = definitions.of(ContractFlowContributor.LINEAR);
        assertEquals(ContractFlowContributor.LINEAR, def.getFlowCode());
        // start + alpha + beta + gamma + end
        assertEquals(5, def.getNodeList().size());
        NodeJson beta = node(def, "beta");
        assertEquals(NodeType.BETWEEN.getKey(), beta.getNodeType());
        assertTrue(beta.getSkipList().stream().anyMatch(s ->
                SkipType.PASS.getKey().equals(s.getSkipType()) && "gamma".equals(s.getNextNodeCode())));
        assertTrue(beta.getSkipList().stream().anyMatch(s ->
                SkipType.REJECT.getKey().equals(s.getSkipType()) && "alpha".equals(s.getNextNodeCode())));
    }

    @Test
    @DisplayName("网关流程 DefJson：SERIAL 节点携带条件跳转")
    void gateway_def_json()
    {
        DefJson def = definitions.of(ContractFlowContributor.GATEWAY);
        assertEquals(ContractFlowContributor.GATEWAY, def.getFlowCode());
        NodeJson gateway = node(def, "gateway");
        assertEquals(NodeType.SERIAL.getKey(), gateway.getNodeType(), "网关映射为 SERIAL");
        assertEquals(2, gateway.getSkipList().size());
        assertTrue(gateway.getSkipList().stream().anyMatch(s ->
                "approve".equals(s.getNextNodeCode()) && "eq@@needApprove|true".equals(s.getSkipCondition())));
        assertTrue(gateway.getSkipList().stream().anyMatch(s ->
                "end".equals(s.getNextNodeCode()) && "eq@@needApprove|false".equals(s.getSkipCondition())));
        // importDef 按 nowNodeCode 分组校验：每个 skip 必填 nowNodeCode
        assertTrue(def.getNodeList().stream().flatMap(n -> n.getSkipList().stream())
                .allMatch(s -> s.getNowNodeCode() != null && !s.getNowNodeCode().isEmpty()));
    }

    @Test
    @DisplayName("未注册流程：定义 null、首节点 null、退回 null")
    void unknown_process()
    {
        assertTrue(!definitions.contains("no-such"));
        assertNull(definitions.of("no-such"));
        assertNull(definitions.firstNode("no-such"));
        assertNull(definitions.previousNode("no-such", "alpha"));
    }

    @Test
    @DisplayName("重复编码跨贡献者 fail-fast")
    void duplicate_code_rejected()
    {
        WorkflowDefinition d1 = WorkflowDefinition.linear("dup-process", "贡献者一", "c", "a", "b");
        WorkflowDefinition d2 = WorkflowDefinition.linear("dup-process", "贡献者二", "c", "x");
        assertThrows(FlowException.class,
                () -> new WarmFlowProcessDefinitions(List.of(() -> List.of(d1), () -> List.of(d2))));
    }

    private NodeJson node(DefJson def, String nodeCode)
    {
        return def.getNodeList().stream()
                .filter(n -> nodeCode.equals(n.getNodeCode()))
                .findFirst().orElseThrow();
    }
}
