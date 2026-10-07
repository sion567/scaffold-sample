package com.scaffold.flow.api;

import java.util.List;

/**
 * 工作流引擎门面 SPI
 *
 * <p><b>操作面契约</b>（仅抽象脚手架约定的通用能力，禁止扩展 BPMN 会签/加签等
 * 引擎私有能力——抽象层不得变成第二个引擎）：</p>
 * <ul>
 *   <li>start：按流程定义发起实例，产生首个待办（bizId 幂等：同 bizId 同流程重复 start 抛异常）</li>
 *   <li>completeTask：完成当前待办推进到下一节点（节点编码由流程定义的线性顺序决定，仅相邻推进）</li>
 *   <li>transfer：转办（待办改派，历史留痕）</li>
 *   <li>returnTo：相邻退回（仅允许退到上一节点，禁止任意跳转）</li>
 *   <li>terminate：终止（撤回/作废，历史保留）</li>
 *   <li>history：按 bizId 全量操作历史（操作留痕与审计）</li>
 * </ul>
 *
 * <p><b>语义约定（所有实现必须遵守，契约测试 WorkflowContractTest 逐条验证）：</b></p>
 * <ul>
 *   <li>业务表是真值源：引擎状态仅服务编排，业务落库与引擎推进须同事务（实现方数据源由接入方装配）</li>
 *   <li>流程定义由接入方经 {@link WorkflowDefinitionContributor} 注册，starter 不内置业务流程</li>
 *   <li>completeTask 校验待办归属与节点匹配；错配抛 {@link FlowException}</li>
 *   <li>所有查询按 bizId 定位，不暴露引擎内部主键语义</li>
 * </ul>
 *
 * @author ct
 */
public interface WorkflowService
{
    /**
     * 发起流程实例（产生首个节点待办）
     *
     * @param processCode 流程定义编码
     * @param bizId       业务键（语义由接入方约定，如单据号/资源 ID）
     * @param startNode   首节点编码
     * @param assignee    首节点办理人
     * @return 首个待办
     * @throws FlowException 同 bizId 重复发起 / 流程定义不存在
     */
    FlowTask start(String processCode, String bizId, String startNode, String assignee);

    /**
     * 完成当前待办，推进到下一节点
     *
     * @return 下一节点的待办（推进到结束节点返回 null）
     * @throws FlowException 待办不存在 / 节点或办理人错配
     */
    FlowTask completeTask(String taskId, String operator, String remark);

    /** 转办：待办改派给新办理人（历史留痕） */
    FlowTask transfer(String taskId, String newAssignee, String operator, String remark);

    /** 相邻退回：当前待办退回上一节点（禁止任意跳转） */
    FlowTask returnToPrevious(String taskId, String operator, String remark);

    /** 终止实例（撤回/作废；已终态抛 FlowException） */
    void terminate(String bizId, String processCode, String operator, String remark);

    /** 按 bizId 查当前活跃待办（无则 null） */
    FlowTask activeTask(String bizId, String processCode);

    /** 按 bizId 查全量操作历史（时间升序） */
    List<FlowHistory> history(String bizId, String processCode);

    /**
     * 完成当前待办并携带流程变量（网关条件路由用，如 needApprove=true/false；
     * 线性流程可忽略 vars 走三参重载）
     */
    default FlowTask completeTask(String taskId, String operator, String remark, java.util.Map<String, Object> vars)
    {
        return completeTask(taskId, operator, remark);
    }

    /**
     * 我的待办：按办理人查活跃待办（时间倒序，limit 截断）。
     * 办理人来源：start/completeTask/transfer 传递的 assignee（责任随办理人传递）。
     */
    default List<FlowTask> todosByAssignee(String assignee, int limit)
    {
        return List.of();
    }

    /**
     * 流程首节点（start 的 startNode 入参必须等于它；未知流程返回 null）。
     * 业务发起方由此免硬编码节点名。
     */
    default String firstNode(String processCode)
    {
        return null;
    }
}
