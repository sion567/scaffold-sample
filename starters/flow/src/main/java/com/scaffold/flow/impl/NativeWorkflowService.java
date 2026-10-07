package com.scaffold.flow.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.scaffold.flow.api.FlowException;
import com.scaffold.flow.api.FlowHistory;
import com.scaffold.flow.api.FlowTask;
import com.scaffold.flow.api.WorkflowDefinition;
import com.scaffold.flow.api.WorkflowDefinitionContributor;
import com.scaffold.flow.api.WorkflowService;

/**
 * 自研内存兜底实现（flow.engine=native 默认启用；开发/联调环境可直接使用）
 *
 * <p>进程内内存实现：<b>重启即失，不用于生产</b>；生产环境使用 flow.engine=warmflow。</p>
 *
 * <p>流程定义不内置：构造时从 {@link WorkflowDefinitionContributor} 收集（无贡献者则
 * 注册表为空，发起未注册流程抛 {@link FlowException}）。支持线性流程与条件网关：
 * completeTask 携带流程变量时，网关按 {@code eq@@var|value} 求值立即路由（网关不落待办），
 * 与 warmflow 引擎语义对齐（契约测试 WorkflowContractTest 双实现验证）。</p>
 *
 * <p>职责边界：超时催办、消息触达归接入方的调度与消息组件，业务表是真值源——
 * 本实现只管待办/转办/相邻退回/历史，全部为内存操作。</p>
 *
 * @author ct
 */
public class NativeWorkflowService implements WorkflowService
{
    /** 流程注册表：processCode → 定义（构造时从 contributor 收集，运行期只读） */
    private final Map<String, WorkflowDefinition> processes;

    /** 活跃待办：taskId → task */
    private final Map<String, FlowTask> todos = new ConcurrentHashMap<>();

    /** 历史：bizId|processCode → 操作流水 */
    private final Map<String, List<FlowHistory>> histories = new ConcurrentHashMap<>();

    /** 已终止/已完成的实例：key → 终态动作 */
    private final Map<String, String> terminated = new ConcurrentHashMap<>();

    public NativeWorkflowService(List<WorkflowDefinitionContributor> contributors)
    {
        this.processes = WorkflowDefinitionContributor.index(
                contributors == null ? List.of() : contributors);
    }

    private String key(String bizId, String processCode)
    {
        return bizId + '|' + processCode;
    }

    private WorkflowDefinition defOf(String processCode)
    {
        WorkflowDefinition def = processes.get(processCode);
        if (def == null)
        {
            throw new FlowException("流程定义不存在: " + processCode
                    + "（请经 WorkflowDefinitionContributor 注册）");
        }
        return def;
    }

    @Override
    public synchronized FlowTask start(String processCode, String bizId, String startNode, String assignee)
    {
        WorkflowDefinition def = defOf(processCode);
        String first = def.firstNode();
        if (!first.equals(startNode))
        {
            throw new FlowException("首节点应为 " + first);
        }
        if (activeTask(bizId, processCode) != null || terminated.containsKey(key(bizId, processCode)))
        {
            throw new FlowException("同 bizId 重复发起: " + bizId);
        }
        FlowTask task = new FlowTask();
        task.setTaskId("NT-" + key(bizId, processCode));
        task.setBizId(bizId);
        task.setProcessCode(processCode);
        task.setNodeCode(startNode);
        task.setNodeName(nodeName(def, startNode));
        task.setAssignee(assignee);
        task.setCreateTime(new Date());
        todos.put(task.getTaskId(), task);
        history(bizId, processCode, "START", startNode, assignee, "发起");
        return task;
    }

    @Override
    public synchronized FlowTask completeTask(String taskId, String operator, String remark)
    {
        return completeTask(taskId, operator, remark, null);
    }

    @Override
    public synchronized FlowTask completeTask(String taskId, String operator, String remark,
            Map<String, Object> vars)
    {
        FlowTask task = todoOf(taskId);
        checkAssignee(task, operator);
        WorkflowDefinition def = defOf(task.getProcessCode());
        // 先路由后变更：网关无可命中分支时整体失败，不产生半次推进
        String nextNode = route(def, task.getNodeCode(), vars);
        history(task.getBizId(), task.getProcessCode(), "COMPLETE", task.getNodeCode(), operator, remark);
        todos.remove(taskId);
        if (def.node(nextNode).getNodeType() == WorkflowDefinition.NodeType.END)
        {
            terminated.put(key(task.getBizId(), task.getProcessCode()), "FINISHED");
            history(task.getBizId(), task.getProcessCode(), "TERMINATE", task.getNodeCode(), operator, "流程办结");
            return null;
        }
        FlowTask next = copy(task, nextNode);
        next.setNodeName(nodeName(def, nextNode));
        todos.put(next.getTaskId(), next);
        history(task.getBizId(), task.getProcessCode(), "START", next.getNodeCode(), next.getAssignee(), "推进");
        return next;
    }

    @Override
    public synchronized java.util.List<FlowTask> todosByAssignee(String assignee, int limit)
    {
        if (assignee == null)
        {
            return List.of();
        }
        return todos.values().stream()
                .filter(t -> assignee.equals(t.getAssignee()))
                .sorted(java.util.Comparator.comparing(FlowTask::getCreateTime,
                        java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())))
                .limit(Math.max(limit, 0))
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public synchronized FlowTask transfer(String taskId, String newAssignee, String operator, String remark)
    {
        FlowTask task = todoOf(taskId);
        history(task.getBizId(), task.getProcessCode(), "TRANSFER", task.getNodeCode(), operator,
                remark + "（转给 " + newAssignee + "）");
        task.setAssignee(newAssignee);
        return task;
    }

    @Override
    public synchronized FlowTask returnToPrevious(String taskId, String operator, String remark)
    {
        FlowTask task = todoOf(taskId);
        WorkflowDefinition def = defOf(task.getProcessCode());
        String prevNode = def.previousNode(task.getNodeCode());
        if (prevNode == null)
        {
            throw new FlowException("首节点无可退回");
        }
        todos.remove(taskId);
        history(task.getBizId(), task.getProcessCode(), "RETURN", task.getNodeCode(), operator, remark);
        FlowTask prev = copy(task, prevNode);
        prev.setNodeName(nodeName(def, prevNode));
        todos.put(prev.getTaskId(), prev);
        history(task.getBizId(), task.getProcessCode(), "START", prev.getNodeCode(), prev.getAssignee(), "退回重办");
        return prev;
    }

    @Override
    public synchronized void terminate(String bizId, String processCode, String operator, String remark)
    {
        defOf(processCode);
        String k = key(bizId, processCode);
        if (terminated.containsKey(k))
        {
            throw new FlowException("实例已终态");
        }
        terminated.put(k, "TERMINATED");
        todos.values().removeIf(t -> k.equals(key(t.getBizId(), t.getProcessCode())));
        history(bizId, processCode, "TERMINATE", "-", operator, remark);
    }

    @Override
    public synchronized FlowTask activeTask(String bizId, String processCode)
    {
        String k = key(bizId, processCode);
        return todos.values().stream()
                .filter(t -> k.equals(key(t.getBizId(), t.getProcessCode())))
                .findFirst().orElse(null);
    }

    @Override
    public synchronized List<FlowHistory> history(String bizId, String processCode)
    {
        List<FlowHistory> list = histories.get(key(bizId, processCode));
        if (list == null)
        {
            return List.of();
        }
        list.sort(Comparator.comparing(FlowHistory::getOperateTime));
        return new ArrayList<>(list);
    }

    // ─── 内部 ─────────────────────────────────────────────────────────────

    /**
     * 沿 PASS 边路由到下一个落待办的 BETWEEN 节点或 END；GATEWAY 不落待办，
     * 命中后立即按 vars 继续路由（与 warmflow 引擎的网关即过语义一致）。
     */
    private String route(WorkflowDefinition def, String fromNode, Map<String, Object> vars)
    {
        String current = fromNode;
        for (int hop = 0; hop <= def.getNodes().size(); hop++)
        {
            WorkflowDefinition.Skip picked = null;
            for (WorkflowDefinition.Skip s : def.node(current).getSkips())
            {
                if (s.getType() == WorkflowDefinition.SkipType.PASS
                        && WorkflowDefinition.matches(s.getCondition(), vars))
                {
                    picked = s;
                    break;
                }
            }
            if (picked == null)
            {
                throw new FlowException("节点 " + current + " 无可命中分支"
                        + (vars == null || vars.isEmpty() ? "（网关节点须携带流程变量）" : ""));
            }
            WorkflowDefinition.Node target = def.node(picked.getNextNodeCode());
            if (target.getNodeType() != WorkflowDefinition.NodeType.GATEWAY)
            {
                return target.getNodeCode();
            }
            current = target.getNodeCode();
        }
        throw new FlowException("网关路由未收敛: " + fromNode);
    }

    private String nodeName(WorkflowDefinition def, String nodeCode)
    {
        return def.node(nodeCode).getNodeName();
    }

    private FlowTask todoOf(String taskId)
    {
        FlowTask task = todos.get(taskId);
        if (task == null)
        {
            throw new FlowException("待办不存在或已办结: " + taskId);
        }
        return task;
    }

    private void checkAssignee(FlowTask task, String operator)
    {
        // 办理人校验由接入方（业务权限体系）执行；SPI 只做待办存在性守卫
    }

    private FlowTask copy(FlowTask source, String nodeCode)
    {
        FlowTask next = new FlowTask();
        next.setTaskId(source.getTaskId() + "@" + nodeCode);
        next.setBizId(source.getBizId());
        next.setProcessCode(source.getProcessCode());
        next.setNodeCode(nodeCode);
        next.setNodeName(nodeCode);
        next.setAssignee(source.getAssignee());
        next.setCreateTime(new Date());
        return next;
    }

    private void history(String bizId, String processCode, String action, String nodeCode, String operator,
            String remark)
    {
        FlowHistory record = new FlowHistory();
        record.setBizId(bizId);
        record.setProcessCode(processCode);
        record.setAction(action);
        record.setNodeCode(nodeCode);
        record.setOperator(operator);
        record.setRemark(remark);
        record.setOperateTime(new Date());
        histories.computeIfAbsent(key(bizId, processCode), k -> new ArrayList<>()).add(record);
    }

    @Override
    public String firstNode(String processCode)
    {
        WorkflowDefinition def = processes.get(processCode);
        return def == null ? null : def.firstNode();
    }
}
