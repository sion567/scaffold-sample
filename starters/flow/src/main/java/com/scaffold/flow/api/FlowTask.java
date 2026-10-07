package com.scaffold.flow.api;

/**
 * 流程待办任务（SPI 数据视图：所有引擎实现统一映射到本结构）
 *
 * <p>字段语义即契约：实现方不得引入引擎私有状态（如 BPMN executionId）——
 * 那些属于引擎内部，业务侧只认 bizId + 节点编码。</p>
 *
 * @author ct
 */
public class FlowTask
{
    /** 引擎待办 ID（完成/转办时传回） */
    private String taskId;

    /** 业务键（语义由接入方约定） */
    private String bizId;

    /** 流程定义编码（由接入方经 WorkflowDefinitionContributor 注册） */
    private String processCode;

    /** 当前节点编码（流程定义的节点状态机约定） */
    private String nodeCode;

    /** 节点名称（展示用） */
    private String nodeName;

    /** 办理人（待办归属） */
    private String assignee;

    private java.util.Date createTime;

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public String getBizId() { return bizId; }
    public void setBizId(String bizId) { this.bizId = bizId; }

    public String getProcessCode() { return processCode; }
    public void setProcessCode(String processCode) { this.processCode = processCode; }

    public String getNodeCode() { return nodeCode; }
    public void setNodeCode(String nodeCode) { this.nodeCode = nodeCode; }

    public String getNodeName() { return nodeName; }
    public void setNodeName(String nodeName) { this.nodeName = nodeName; }

    public String getAssignee() { return assignee; }
    public void setAssignee(String assignee) { this.assignee = assignee; }

    public java.util.Date getCreateTime() { return createTime; }
    public void setCreateTime(java.util.Date createTime) { this.createTime = createTime; }
}
