package com.scaffold.contract;

import java.util.List;

import com.scaffold.flow.api.WorkflowDefinition;
import com.scaffold.flow.api.WorkflowDefinitionContributor;

/**
 * 契约测试流程贡献者：与生产业务解耦的通用流程（线性 + 条件网关），所有引擎实现
 * （native / warmflow）注册同一套定义、跑同一套契约用例。接入方可参照本类写法
 * 声明自己的 {@link WorkflowDefinitionContributor} Bean。
 *
 * <p>位于 com.scaffold.contract（非 com.scaffold.flow 扫描包内）：
 * warmflow 契约测试经 @Import 显式注册，避免组件扫描重复注册同名 Bean。</p>
 *
 * @author ct
 */
public class ContractFlowContributor implements WorkflowDefinitionContributor
{
    /** 线性流程：alpha → beta → gamma（相邻推进/退回，3 个业务节点） */
    public static final String LINEAR = "contract-linear";

    /** 网关流程：submit → gateway(needApprove) → approve → end / false 直达 end 办结 */
    public static final String GATEWAY = "contract-gateway";

    @Override
    public List<WorkflowDefinition> definitions()
    {
        return List.of(linear(), gateway());
    }

    /** 线性流程（工厂一步构造：自动生成相邻推进与退回边） */
    public static WorkflowDefinition linear()
    {
        return WorkflowDefinition.linear(LINEAR, "契约线性流程", "contract", "alpha", "beta", "gamma");
    }

    /** 条件网关流程（自由组装：gateway 按流程变量 needApprove 路由） */
    public static WorkflowDefinition gateway()
    {
        return WorkflowDefinition.of(GATEWAY, "契约网关流程", "contract", List.of(
                WorkflowDefinition.node(WorkflowDefinition.NodeType.START, "start", "开始",
                        List.of(WorkflowDefinition.skip(WorkflowDefinition.SkipType.PASS, "提交", "submit", null))),
                WorkflowDefinition.node(WorkflowDefinition.NodeType.BETWEEN, "submit", "提交",
                        List.of(WorkflowDefinition.skip(WorkflowDefinition.SkipType.PASS, "送审", "gateway", null))),
                WorkflowDefinition.node(WorkflowDefinition.NodeType.GATEWAY, "gateway", "审批判定",
                        List.of(WorkflowDefinition.skipIf("需审批", "approve", "needApprove", "true"),
                                WorkflowDefinition.skipIf("免审批", "end", "needApprove", "false"))),
                WorkflowDefinition.node(WorkflowDefinition.NodeType.BETWEEN, "approve", "审批",
                        List.of(WorkflowDefinition.skip(WorkflowDefinition.SkipType.PASS, "审批通过", "end", null),
                                WorkflowDefinition.skip(WorkflowDefinition.SkipType.REJECT, "退回提交", "submit", null))),
                WorkflowDefinition.node(WorkflowDefinition.NodeType.END, "end", "结束", List.of())));
    }
}
