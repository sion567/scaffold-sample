package com.scaffold.sample.flow;

import java.util.List;
import org.springframework.stereotype.Component;
import com.scaffold.flow.api.WorkflowDefinition;
import com.scaffold.flow.api.WorkflowDefinitionContributor;

/**
 * 订单审批流程定义贡献者
 *
 * 工作流 starter 不内置任何流程，接入方声明 Contributor 即可注册自己的流程定义
 * （native / warmflow 引擎通用）：submit 提交 -> audit 审批 -> done 办结，
 * 相邻退回边由 linear 工厂自动生成（audit 可退回 submit）。
 *
 * @author scaffold
 */
@Component
public class SampleOrderFlowContributor implements WorkflowDefinitionContributor
{
    /** 订单审批流程编码 */
    public static final String ORDER_APPROVE_PROCESS = "sample-order-approve";

    @Override
    public List<WorkflowDefinition> definitions()
    {
        return List.of(WorkflowDefinition.linear(ORDER_APPROVE_PROCESS, "订单审批流", "样例",
                "submit", "audit", "done"));
    }
}
