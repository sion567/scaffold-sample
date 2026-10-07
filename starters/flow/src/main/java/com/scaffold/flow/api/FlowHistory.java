package com.scaffold.flow.api;

import java.util.Date;

/**
 * 流程历史记录（SPI 数据视图：操作留痕与审计）
 *
 * @author ct
 */
public class FlowHistory
{
    private String bizId;

    private String processCode;

    private String nodeCode;

    /** 动作：START/COMPLETE/TRANSFER/RETURN/TERMINATE */
    private String action;

    private String operator;

    private String remark;

    private Date operateTime;

    public String getBizId() { return bizId; }
    public void setBizId(String bizId) { this.bizId = bizId; }

    public String getProcessCode() { return processCode; }
    public void setProcessCode(String processCode) { this.processCode = processCode; }

    public String getNodeCode() { return nodeCode; }
    public void setNodeCode(String nodeCode) { this.nodeCode = nodeCode; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public Date getOperateTime() { return operateTime; }
    public void setOperateTime(Date operateTime) { this.operateTime = operateTime; }
}
