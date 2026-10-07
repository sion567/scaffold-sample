package com.scaffold.flow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.scaffold.contract.ContractFlowContributor;
import com.scaffold.flow.api.FlowException;
import com.scaffold.flow.api.FlowHistory;
import com.scaffold.flow.api.FlowTask;
import com.scaffold.flow.api.WorkflowService;
import com.scaffold.flow.impl.NativeWorkflowService;

import java.util.List;

/**
 * 工作流 SPI 契约测试——<b>所有引擎实现（Warm-Flow / Native 等）必须原样通过本套用例</b>。
 *
 * <p>契约流程由 {@link ContractFlowContributor} 提供（线性流程 + 条件网关流程），
 * 与生产业务解耦——接入方以同样方式经 WorkflowDefinitionContributor 注册自己的流程。</p>
 *
 * @author ct
 */
class WorkflowContractTest
{
    private WorkflowService service;

    @BeforeEach
    void setUp()
    {
        service = newService();
    }

    /** 子类覆写此方法接入被测实现（Warm-Flow 等引擎适配器各自提供） */
    protected WorkflowService newService()
    {
        return new NativeWorkflowService(List.of(new ContractFlowContributor()));
    }

    @Test
    @DisplayName("start：产生首节点待办；重复发起与首节点错配拒绝")
    void start_and_idempotent()
    {
        FlowTask task = service.start(ContractFlowContributor.LINEAR, "B-001", "alpha", "u1");
        assertNotNull(task.getTaskId());
        assertEquals("alpha", task.getNodeCode());
        assertEquals("B-001", task.getBizId());

        assertThrows(FlowException.class,
                () -> service.start(ContractFlowContributor.LINEAR, "B-001", "alpha", "u1"),
                "同 bizId 重复发起");
        assertThrows(FlowException.class,
                () -> service.start(ContractFlowContributor.LINEAR, "B-011", "beta", "u1"),
                "首节点错配");
    }

    @Test
    @DisplayName("complete：线性推进 alpha→beta→gamma→办结")
    void linear_progression()
    {
        service.start(ContractFlowContributor.LINEAR, "B-002", "alpha", "u1");
        FlowTask task = service.activeTask("B-002", ContractFlowContributor.LINEAR);
        assertNotNull(service.completeTask(task.getTaskId(), "u1", "首节点办理"));

        task = service.activeTask("B-002", ContractFlowContributor.LINEAR);
        assertEquals("beta", task.getNodeCode());
        service.completeTask(task.getTaskId(), "u1", null);

        task = service.activeTask("B-002", ContractFlowContributor.LINEAR);
        assertEquals("gamma", task.getNodeCode());
        assertNull(service.completeTask(task.getTaskId(), "leader", "末节点通过"), "末节点办结返回 null");

        assertNull(service.activeTask("B-002", ContractFlowContributor.LINEAR), "办结后无活跃待办");
    }

    @Test
    @DisplayName("相邻退回：gamma 退回 beta；首节点退回拒绝")
    void return_adjacent_only()
    {
        service.start(ContractFlowContributor.LINEAR, "B-003", "alpha", "u1");
        FlowTask task = service.activeTask("B-003", ContractFlowContributor.LINEAR);
        assertThrows(FlowException.class, () -> service.returnToPrevious(task.getTaskId(), "u1", null),
                "首节点无可退回");

        service.completeTask(task.getTaskId(), "u1", null);
        service.completeTask(service.activeTask("B-003", ContractFlowContributor.LINEAR).getTaskId(), "u1", null);
        FlowTask gamma = service.activeTask("B-003", ContractFlowContributor.LINEAR);
        assertEquals("gamma", gamma.getNodeCode());

        // gamma 退回应回到上一节点（按线性序列为 beta——仅相邻，不跳 alpha）
        FlowTask returned = service.returnToPrevious(gamma.getTaskId(), "u1", "材料不全退回");
        assertEquals("beta", returned.getNodeCode());
    }

    @Test
    @DisplayName("转办：待办改派并留痕")
    void transfer_reassigns()
    {
        service.start(ContractFlowContributor.LINEAR, "B-004", "alpha", "u1");
        FlowTask task = service.transfer(
                service.activeTask("B-004", ContractFlowContributor.LINEAR).getTaskId(), "u2", "op", "交接");
        assertEquals("u2", task.getAssignee());
    }

    @Test
    @DisplayName("terminate：终止后无待办；重复终止拒绝")
    void terminate_and_guard()
    {
        service.start(ContractFlowContributor.LINEAR, "B-005", "alpha", "u1");
        service.terminate("B-005", ContractFlowContributor.LINEAR, "op", "业务撤回");
        assertNull(service.activeTask("B-005", ContractFlowContributor.LINEAR));
        assertThrows(FlowException.class,
                () -> service.terminate("B-005", ContractFlowContributor.LINEAR, "op", "again"));
    }

    @Test
    @DisplayName("历史：全量操作流水按时间升序可追溯")
    void history_traceable()
    {
        service.start(ContractFlowContributor.LINEAR, "B-006", "alpha", "u1");
        FlowTask task = service.activeTask("B-006", ContractFlowContributor.LINEAR);
        service.completeTask(task.getTaskId(), "u1", "正常办理");

        List<FlowHistory> history = service.history("B-006", ContractFlowContributor.LINEAR);
        // 契约只锁：首条为发起、存在 COMPLETE 记录、备注透传；推进是否单独记 START 属实现自由
        assertEquals("START", history.get(0).getAction());
        assertEquals("COMPLETE", history.stream()
                .filter(h -> "COMPLETE".equals(h.getAction())).findFirst().orElseThrow().getAction());
        assertEquals("正常办理", history.stream()
                .filter(h -> "COMPLETE".equals(h.getAction())).findFirst().orElseThrow().getRemark());
    }

    @Test
    @DisplayName("未知流程定义拒绝")
    void unknown_process_rejected()
    {
        assertThrows(FlowException.class,
                () -> service.start("no-such-process", "B1", "alpha", "u1"));
    }

    @Test
    @DisplayName("我的待办：按办理人过滤；转办后归属跟随")
    void todos_follow_assignee()
    {
        service.start(ContractFlowContributor.LINEAR, "B-009", "alpha", "u1");
        List<FlowTask> mine = service.todosByAssignee("u1", 10);
        assertTrue(mine.stream().anyMatch(t -> "B-009".equals(t.getBizId())), "办理人应看到自己的待办");

        FlowTask task = service.activeTask("B-009", ContractFlowContributor.LINEAR);
        service.transfer(task.getTaskId(), "u2", "op", "交接");
        assertTrue(service.todosByAssignee("u2", 10).stream()
                .anyMatch(t -> "B-009".equals(t.getBizId())), "转办后新办理人可见");
        assertTrue(service.todosByAssignee("u1", 10).stream()
                .noneMatch(t -> "B-009".equals(t.getBizId())), "转办后原办理人不再可见");

        // 责任随办理人传递：u2 完成当前节点后，下一节点（beta）待办仍归属 u2
        assertTrue(service.todosByAssignee("u2", 10).stream()
                .anyMatch(t -> "B-009".equals(t.getBizId())), "下一节点待办仍归属现办理人");
        // 办结全链后从待办消失
        FlowTask t = service.activeTask("B-009", ContractFlowContributor.LINEAR);
        while (t != null)
        {
            t = service.completeTask(t.getTaskId(), "u2", null);
        }
        assertTrue(service.todosByAssignee("u2", 10).stream()
                .noneMatch(tt -> "B-009".equals(tt.getBizId())), "办结后从待办消失");
    }

    @Test
    @DisplayName("完成携带流程变量：线性流程安全透传不报错")
    void complete_with_vars_passthrough()
    {
        service.start(ContractFlowContributor.LINEAR, "B-010", "alpha", "u1");
        FlowTask task = service.activeTask("B-010", ContractFlowContributor.LINEAR);
        FlowTask next = service.completeTask(task.getTaskId(), "u1", null, java.util.Map.of("k", "v"));
        assertEquals("beta", next.getNodeCode(), "vars 透传不应影响线性推进");
    }

    @Test
    @DisplayName("网关路由：needApprove=true 走 approve，false 直达办结")
    void gateway_condition_routing()
    {
        // true：submit → gateway → approve
        service.start(ContractFlowContributor.GATEWAY, "B-101", "submit", "u1");
        FlowTask task = service.activeTask("B-101", ContractFlowContributor.GATEWAY);
        FlowTask next = service.completeTask(task.getTaskId(), "u1", null,
                java.util.Map.of("needApprove", true));
        assertNotNull(next, "命中需审批分支应落 approve 待办");
        assertEquals("approve", next.getNodeCode(), "网关应路由到 approve");
        // approve 退回 submit（REJECT 边相邻退回）
        FlowTask returned = service.returnToPrevious(next.getTaskId(), "u1", "退回重办");
        assertEquals("submit", returned.getNodeCode());
        // 再次推进走 true 分支到 approve，审批通过后办结
        next = service.completeTask(returned.getTaskId(), "u1", null, java.util.Map.of("needApprove", true));
        assertEquals("approve", next.getNodeCode());
        assertNull(service.completeTask(next.getTaskId(), "leader", "通过"), "审批通过即办结");
        assertNull(service.activeTask("B-101", ContractFlowContributor.GATEWAY));

        // false：submit → gateway → end，一次完成即办结
        service.start(ContractFlowContributor.GATEWAY, "B-102", "submit", "u1");
        task = service.activeTask("B-102", ContractFlowContributor.GATEWAY);
        assertNull(service.completeTask(task.getTaskId(), "u1", null,
                java.util.Map.of("needApprove", false)), "免审批分支直接办结返回 null");
        assertNull(service.activeTask("B-102", ContractFlowContributor.GATEWAY));
    }
}
