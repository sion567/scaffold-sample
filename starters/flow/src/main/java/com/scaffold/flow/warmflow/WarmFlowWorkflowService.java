package com.scaffold.flow.warmflow;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.dto.DefJson;
import org.dromara.warm.flow.core.dto.FlowParams;
import org.dromara.warm.flow.core.entity.Definition;
import org.dromara.warm.flow.core.entity.HisTask;
import org.dromara.warm.flow.core.entity.Instance;
import org.dromara.warm.flow.core.entity.Task;
import org.dromara.warm.flow.core.enums.PublishStatus;
import org.dromara.warm.flow.core.enums.SkipType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.scaffold.flow.api.FlowException;
import com.scaffold.flow.api.FlowHistory;
import com.scaffold.flow.api.FlowTask;
import com.scaffold.flow.api.WorkflowDefinitionContributor;
import com.scaffold.flow.api.WorkflowService;

/**
 * Warm-Flow 1.8.9 生产实现（flow.engine=warmflow 时启用）。
 *
 * <p>激活方式：flow.engine=warmflow + 接入方提供 DataSource（Warm-Flow MP starter 装配引擎，
 * flow_ 官方表 DDL 由部署初始化）。Bean 由 {@link com.scaffold.flow.config.WorkflowAutoConfiguration}
 * 注册（不依赖宿主扫描本包）。可选注入 StringRedisTemplate 做集群 businessId 防重锁。
 * 流程定义不内置：经 {@link WorkflowDefinitionContributor} 注入，由
 * {@link WarmFlowProcessDefinitions} 转换为 DefJson 并按需部署。</p>
 *
 * <p>1.8.9 适配要点（踩坑记录）：</p>
 * <ul>
 * <li>静态门面 FlowFactory → {@link FlowEngine}；流程定义导入走 {@code importDef(DefJson)}，
 *     导入后须显式 active + publish，否则 start NPE；</li>
 * <li>orm 实体 businessId 为 exist=false：按 bizId 查待办/历史必须先查实例再关联；</li>
 * <li>撤回终止走 taskService.termination，禁止直接删实例（遗留孤儿待办）；</li>
 * <li>并发防重短板（flow_task 无乐观锁）：businessId 锁由本类统一处理——有 Redis 走
 *     SETNX 分布式锁，无 Redis 退 JVM 锁（单机开发态）。</li>
 * </ul>
 *
 * @author ct
 */
public class WarmFlowWorkflowService implements WorkflowService
{
    private static final Logger log = LoggerFactory.getLogger(WarmFlowWorkflowService.class);

    /** 集群防重锁前缀与 TTL（TTL 覆盖单次推进的最长耗时） */
    private static final String LOCK_PREFIX = "flow:lock:";

    private static final Duration LOCK_TTL = Duration.ofSeconds(10);

    /** 实例变量键：当前待办办理人（SPI 的 assignee 语义；转办/推进时随办理人传递） */
    private static final String VAR_ASSIGNEE = "spiAssignee";

    /** 实例变量键：发起人（合成 START 历史用） */
    private static final String VAR_START_BY = "spiStartBy";

    /** 实例变量键：流程编码（1.8.9 实例→定义的 flowCode 查询不可靠，自存自证） */
    private static final String VAR_PROCESS = "spiProcess";

    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;

    /** 流程注册表（来自 WorkflowDefinitionContributor） */
    private final WarmFlowProcessDefinitions definitions;

    public WarmFlowWorkflowService(ObjectProvider<StringRedisTemplate> redisTemplateProvider,
            List<WorkflowDefinitionContributor> contributors)
    {
        this.redisTemplateProvider = redisTemplateProvider;
        this.definitions = new WarmFlowProcessDefinitions(contributors);
    }

    @Override
    public FlowTask start(String processCode, String bizId, String startNode, String assignee)
    {
        requireKnownProcess(processCode);
        String first = definitions.firstNode(processCode);
        if (!first.equals(startNode))
        {
            throw new FlowException("流程首节点为 " + first + "，不支持从 " + startNode + " 发起");
        }
        return withBizLock(bizId, () -> {
            if (!FlowEngine.insService().list(FlowEngine.newIns().setBusinessId(bizId)).isEmpty())
            {
                throw new FlowException("业务键 " + bizId + " 已存在流程实例，禁止重复发起");
            }
            deployIfNeeded(processCode);
            Map<String, Object> vars = new HashMap<>();
            vars.put(VAR_ASSIGNEE, assignee);
            vars.put(VAR_START_BY, assignee);
            vars.put(VAR_PROCESS, processCode);
            FlowParams params = FlowParams.build()
                    .flowCode(processCode)
                    .handler(assignee == null ? "system" : assignee)
                    .permissionFlag(List.of(assignee == null ? "system" : assignee))
                    .variable(vars);
            Instance instance = FlowEngine.insService().start(bizId, params);
            log.info("[工作流] 流程发起: process={}, bizId={}, instance={}", processCode, bizId, instance.getId());
            return activeTask(bizId, processCode);
        });
    }

    @Override
    public FlowTask completeTask(String taskId, String operator, String remark)
    {
        return completeTask(taskId, operator, remark, null);
    }

    @Override
    public FlowTask completeTask(String taskId, String operator, String remark, Map<String, Object> vars)
    {
        Task task = requireTask(taskId);
        FlowParams params = FlowParams.build()
                .skipType(SkipType.PASS.getKey())
                .handler(operator)
                // 责任随办理人传递：下一节点待办归属本操作人（todosByAssignee 的数据基础）
                .permissionFlag(List.of(operator))
                .message(remark == null || remark.isEmpty() ? "同意" : remark);
        if (vars != null && !vars.isEmpty())
        {
            params.variable(vars);
        }
        FlowEngine.taskService().skip(Long.valueOf(taskId), params);
        return activeTaskByInstance(task.getInstanceId());
    }

    @Override
    public FlowTask transfer(String taskId, String newAssignee, String operator, String remark)
    {
        Task task = requireTask(taskId);
        // 转办目标走 addHandlers（Warm-Flow transfer 语义：handler=操作人，addHandlers=新办理人）
        FlowEngine.taskService().transfer(Long.valueOf(taskId), FlowParams.build()
                .handler(operator)
                .addHandlers(List.of(newAssignee))
                .message(remark == null || remark.isEmpty() ? "转办" : remark));
        keepAssignee(task.getInstanceId(), newAssignee);
        return activeTaskByInstance(task.getInstanceId());
    }

    @Override
    public FlowTask returnToPrevious(String taskId, String operator, String remark)
    {
        Task task = requireTask(taskId);
        String flowCode = processCodeOf(task.getInstanceId());
        if (definitions.previousNode(flowCode, task.getNodeCode()) == null)
        {
            throw new FlowException("首节点无可退回");
        }
        FlowEngine.taskService().skip(Long.valueOf(taskId), FlowParams.build()
                .skipType(SkipType.REJECT.getKey())
                .handler(operator)
                .message(remark == null || remark.isEmpty() ? "退回上一节点" : remark));
        return activeTaskByInstance(task.getInstanceId());
    }

    @Override
    public void terminate(String bizId, String processCode, String operator, String remark)
    {
        requireKnownProcess(processCode);
        withBizLock(bizId, () -> {
            List<Task> tasks = tasksOf(bizId, processCode);
            if (tasks.isEmpty())
            {
                throw new FlowException("业务键 " + bizId + " 无进行中流程实例，不可终止");
            }
            for (Task task : tasks)
            {
                FlowEngine.taskService().termination(task.getId(), FlowParams.build()
                        .handler(operator)
                        .message(remark == null ? "终止" : remark)
                        .ignore(true));
            }
            return null;
        });
    }

    @Override
    public FlowTask activeTask(String bizId, String processCode)
    {
        List<Task> tasks = tasksOf(bizId, processCode);
        if (tasks.isEmpty())
        {
            return null;
        }
        Task task = tasks.get(0);
        FlowTask result = new FlowTask();
        result.setTaskId(String.valueOf(task.getId()));
        result.setBizId(bizId);
        result.setProcessCode(processCode);
        result.setNodeCode(task.getNodeCode());
        result.setNodeName(task.getNodeName());
        result.setAssignee(currentAssignee(task.getInstanceId()));
        result.setCreateTime(task.getCreateTime());
        return result;
    }

    @Override
    public List<FlowHistory> history(String bizId, String processCode)
    {
        requireKnownProcess(processCode);
        List<FlowHistory> result = new ArrayList<>();
        for (Instance instance : FlowEngine.insService().list(FlowEngine.newIns().setBusinessId(bizId)))
        {
            FlowHistory start = new FlowHistory();
            start.setBizId(bizId);
            start.setProcessCode(processCode);
            start.setNodeCode(definitions.firstNode(processCode));
            start.setAction("START");
            Map<String, Object> startVars = instance.getVariableMap();
            Object startBy = startVars == null ? null : startVars.get(VAR_START_BY);
            start.setOperator(startBy == null ? "system" : String.valueOf(startBy));
            start.setOperateTime(instance.getCreateTime());
            result.add(start);

            List<HisTask> hisTasks = FlowEngine.hisTaskService()
                    .list(FlowEngine.newHisTask().setInstanceId(instance.getId()));
            hisTasks.sort(Comparator.comparing(HisTask::getCreateTime));
            for (HisTask his : hisTasks)
            {
                FlowHistory item = new FlowHistory();
                item.setBizId(bizId);
                item.setProcessCode(processCode);
                item.setNodeCode(his.getNodeCode());
                item.setAction(SkipType.REJECT.getKey().equals(his.getSkipType()) ? "RETURN" : "COMPLETE");
                item.setOperator(his.getApprover());
                item.setRemark(his.getMessage());
                item.setOperateTime(his.getCreateTime());
                result.add(item);
            }
        }
        result.sort(Comparator.comparing(FlowHistory::getOperateTime,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return result;
    }

    @Override
    public List<FlowTask> todosByAssignee(String assignee, int limit)
    {
        if (assignee == null || assignee.isEmpty())
        {
            return List.of();
        }
        // Warm-Flow 待办的办理人挂在 task.permissionList（flow_user 表）；全量拉取后过滤——
        // 在办待办量级远小于分页下推的成本阈值，待办中心页立项时再议下推
        List<Task> all = FlowEngine.taskService().list(FlowEngine.newTask());
        List<FlowTask> mine = new ArrayList<>();
        for (Task task : all)
        {
            Instance instance = FlowEngine.insService().getById(task.getInstanceId());
            Map<String, Object> vars = instance == null ? null : instance.getVariableMap();
            String owner = vars == null || vars.get(VAR_ASSIGNEE) == null
                    ? null : String.valueOf(vars.get(VAR_ASSIGNEE));
            // 办理人匹配：实例变量为准（我们写入），task.permissionList 为辅（部分通道不回填）
            boolean mine1 = assignee.equals(owner)
                    || (task.getPermissionList() != null && task.getPermissionList().contains(assignee));
            if (!mine1)
            {
                continue;
            }
            FlowTask item = new FlowTask();
            item.setTaskId(String.valueOf(task.getId()));
            item.setNodeCode(task.getNodeCode());
            item.setNodeName(task.getNodeName());
            item.setAssignee(owner == null ? assignee : owner);
            item.setCreateTime(task.getCreateTime());
            if (instance != null)
            {
                item.setBizId(instance.getBusinessId());
                item.setProcessCode(owner == null ? null : String.valueOf(vars.get(VAR_PROCESS)));
            }
            mine.add(item);
            if (mine.size() >= Math.max(limit, 0))
            {
                break;
            }
        }
        return mine;
    }

    // ─── 内部 ─────────────────────────────────────────────────────────────

    private void requireKnownProcess(String processCode)
    {
        if (!definitions.contains(processCode))
        {
            throw new FlowException("未知流程定义: " + processCode);
        }
    }

    /** 流程定义按需部署：已发布跳过，否则注册表取 DefJson → 导入 → 激活 → 发布 */
    private void deployIfNeeded(String processCode)
    {
        List<Definition> defs = FlowEngine.defService().queryByCodeList(List.of(processCode));
        boolean published = defs.stream().anyMatch(d -> PublishStatus.PUBLISHED.getKey().equals(d.getIsPublish()));
        if (published)
        {
            return;
        }
        DefJson def = definitions.of(processCode);
        if (def == null)
        {
            throw new FlowException("未知流程定义: " + processCode);
        }
        Definition imported = FlowEngine.defService().importDef(def);
        // 1.8.9 DefJson 导入后须显式激活再发布，否则 start NPE
        FlowEngine.defService().active(imported.getId());
        FlowEngine.defService().publish(imported.getId());
        log.info("[工作流] 流程定义部署完成: process={}", processCode);
    }

    /** businessId 防重锁：有 Redis 走 SETNX 分布式锁（TTL 10s，finally 释放），否则退 JVM 锁 */
    private <T> T withBizLock(String bizId, java.util.function.Supplier<T> action)
    {
        StringRedisTemplate redis = redisTemplateProvider.getIfAvailable();
        if (redis == null)
        {
            synchronized (bizId.intern())
            {
                return action.get();
            }
        }
        String key = LOCK_PREFIX + bizId;
        Boolean got = redis.opsForValue().setIfAbsent(key, "1", LOCK_TTL);
        if (!Boolean.TRUE.equals(got))
        {
            throw new FlowException("业务键 " + bizId + " 正在处理中，请稍后重试");
        }
        try
        {
            return action.get();
        }
        finally
        {
            redis.delete(key);
        }
    }

    private Task requireTask(String taskId)
    {
        Task task = FlowEngine.taskService().getById(Long.valueOf(taskId));
        if (task == null)
        {
            throw new FlowException("待办不存在: " + taskId);
        }
        return task;
    }

    private List<Task> tasksOf(String bizId, String processCode)
    {
        requireKnownProcess(processCode);
        List<Task> tasks = new ArrayList<>();
        for (Instance instance : FlowEngine.insService().list(FlowEngine.newIns().setBusinessId(bizId)))
        {
            // 1.8.9 orm 实体 businessId 为 exist=false：须经实例关联查待办
            tasks.addAll(FlowEngine.taskService().list(FlowEngine.newTask().setInstanceId(instance.getId())));
        }
        return tasks;
    }

    private FlowTask activeTaskByInstance(Long instanceId)
    {
        List<Task> tasks = FlowEngine.taskService().list(FlowEngine.newTask().setInstanceId(instanceId));
        if (tasks.isEmpty())
        {
            return null;
        }
        Task task = tasks.get(0);
        FlowTask result = new FlowTask();
        result.setTaskId(String.valueOf(task.getId()));
        result.setNodeCode(task.getNodeCode());
        result.setNodeName(task.getNodeName());
        result.setAssignee(currentAssignee(instanceId));
        result.setCreateTime(task.getCreateTime());
        return result;
    }

    private void keepAssignee(Long instanceId, String assignee)
    {
        Instance instance = FlowEngine.insService().getById(instanceId);
        if (instance != null)
        {
            Map<String, Object> vars = instance.getVariableMap() == null
                    ? new HashMap<>() : new HashMap<>(instance.getVariableMap());
            vars.put(VAR_ASSIGNEE, assignee);
            // 实体 variable 字段为 JSON 字符串（1.8.9），写入须序列化
            instance.setVariable(com.alibaba.fastjson2.JSON.toJSONString(vars));
            FlowEngine.insService().updateById(instance);
        }
    }

    private String processCodeOf(Long instanceId)
    {
        Instance instance = FlowEngine.insService().getById(instanceId);
        Map<String, Object> vars = instance == null ? null : instance.getVariableMap();
        Object value = vars == null ? null : vars.get(VAR_PROCESS);
        return value == null ? null : String.valueOf(value);
    }

    private String currentAssignee(Long instanceId)
    {
        Instance instance = FlowEngine.insService().getById(instanceId);
        if (instance == null)
        {
            return null;
        }
        Map<String, Object> vars = instance.getVariableMap();
        Object value = vars == null ? null : vars.get(VAR_ASSIGNEE);
        return value == null ? null : String.valueOf(value);
    }

    @Override
    public String firstNode(String processCode)
    {
        return definitions.firstNode(processCode);
    }
}
