package com.scaffold.flow.warmflow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.dromara.warm.flow.core.dto.DefJson;
import org.dromara.warm.flow.core.dto.NodeJson;
import org.dromara.warm.flow.core.dto.SkipJson;
import org.dromara.warm.flow.core.enums.NodeType;
import org.dromara.warm.flow.core.enums.SkipType;

import com.scaffold.flow.api.WorkflowDefinition;
import com.scaffold.flow.api.WorkflowDefinitionContributor;

/**
 * Warm-Flow 引擎适配：把引擎无关的 {@link WorkflowDefinition}（由
 * {@link WorkflowDefinitionContributor} 注入）转换为 1.8.9 导入格式 DefJson，
 * 并提供 firstNode/previousNode 查询。starter 不内置任何业务流程。
 *
 * <p>1.8.9 导入格式要点（踩坑记录，改动须回归契约测试）：</p>
 * <ul>
 * <li>DefJson 对象树导入；skip 必填 nowNodeCode（importDef 按 nowNodeCode 分组校验，漏填 NPE）；</li>
 * <li>网关 skipCondition 表达式 {@code eq@@var|value}，与 {@link WorkflowDefinition#matches}
 *     为同一语法；网关节点映射 {@link NodeType#SERIAL}；</li>
 * <li>importDef 之后须显式 active + publish，否则 start NPE
 *     （见 WarmFlowWorkflowService#deployIfNeeded）。</li>
 * </ul>
 *
 * @author ct
 */
public final class WarmFlowProcessDefinitions
{
    /** 流程注册表：processCode → 定义（构造时从 contributor 收集，运行期只读） */
    private final Map<String, WorkflowDefinition> definitions;

    public WarmFlowProcessDefinitions(List<WorkflowDefinitionContributor> contributors)
    {
        this.definitions = WorkflowDefinitionContributor.index(
                contributors == null ? List.of() : contributors);
    }

    public boolean contains(String processCode)
    {
        return definitions.containsKey(processCode);
    }

    /** 已注册流程的 DefJson（未注册返回 null） */
    public DefJson of(String processCode)
    {
        WorkflowDefinition definition = definitions.get(processCode);
        return definition == null ? null : toDefJson(definition);
    }

    /** 首个业务节点（未注册流程返回 null） */
    public String firstNode(String processCode)
    {
        WorkflowDefinition definition = definitions.get(processCode);
        return definition == null ? null : definition.firstNode();
    }

    /** 上一业务节点（REJECT 边；首节点返回 null = 无可退回） */
    public String previousNode(String processCode, String nodeCode)
    {
        WorkflowDefinition definition = definitions.get(processCode);
        return definition == null ? null : definition.previousNode(nodeCode);
    }

    // ─── WorkflowDefinition → DefJson 映射 ────────────────────────────────

    private DefJson toDefJson(WorkflowDefinition definition)
    {
        List<NodeJson> nodes = new ArrayList<>();
        int i = 0;
        for (WorkflowDefinition.Node n : definition.getNodes())
        {
            List<SkipJson> skips = new ArrayList<>();
            for (WorkflowDefinition.Skip s : n.getSkips())
            {
                skips.add(skip(n.getNodeCode(), s.getSkipName(),
                        s.getType() == WorkflowDefinition.SkipType.REJECT
                                ? SkipType.REJECT.getKey() : SkipType.PASS.getKey(),
                        s.getNextNodeCode(), s.getCondition()));
            }
            nodes.add(node(warmNodeType(n.getNodeType()), n.getNodeCode(), n.getNodeName(),
                    null, skips, (20 + i * 160) + ",120"));
            i++;
        }
        DefJson def = new DefJson();
        def.setFlowCode(definition.getProcessCode());
        def.setFlowName(definition.getProcessName());
        def.setCategory(definition.getCategory());
        def.setVersion("v1");
        def.setNodeList(nodes);
        return def;
    }

    private Integer warmNodeType(WorkflowDefinition.NodeType nodeType)
    {
        return switch (nodeType)
        {
            case START -> NodeType.START.getKey();
            case BETWEEN -> NodeType.BETWEEN.getKey();
            case GATEWAY -> NodeType.SERIAL.getKey();
            case END -> NodeType.END.getKey();
        };
    }

    private NodeJson node(Integer nodeType, String nodeCode, String nodeName, String permissionFlag,
            List<SkipJson> skips, String coordinate)
    {
        NodeJson n = new NodeJson();
        n.setNodeType(nodeType);
        n.setNodeCode(nodeCode);
        n.setNodeName(nodeName);
        n.setPermissionFlag(permissionFlag);
        n.setCoordinate(coordinate);
        n.setSkipList(skips);
        return n;
    }

    private SkipJson skip(String nowNodeCode, String skipName, String skipType,
            String nextNodeCode, String skipCondition)
    {
        SkipJson s = new SkipJson();
        // 1.8.9 importDef 按 nowNodeCode 分组校验，漏填直接 NPE
        s.setNowNodeCode(nowNodeCode);
        s.setSkipName(skipName);
        s.setSkipType(skipType);
        s.setNextNodeCode(nextNodeCode);
        s.setSkipCondition(skipCondition);
        return s;
    }
}
