package com.scaffold.common.log.domain;

import java.io.Serializable;
import java.util.Date;

/**
 * 业务留痕条目（{@code @TraceLog} 切面采集的单一留痕记录）。
 *
 * <p>由切面组装、经 {@code LogPersister#saveTrace} 投递；
 * 快照类字段已在切面侧做长度截断。</p>
 *
 * @author scaffold
 */
public class TraceLogEntry implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 业务场景（@TraceLog.scene） */
    private String scene;

    /** 业务类型（@TraceLog.bizType） */
    private String bizType;

    /** 业务主键（@TraceLog.bizIdEl 解析结果） */
    private String bizId;

    /** 操作人 */
    private String operator;

    /** 操作 IP */
    private String operateIp;

    /** 终端类型（WEB/APP/...，请求头 X-Terminal） */
    private String terminal;

    /** 方法名（全限定类名#方法） */
    private String methodName;

    /** 入参快照 JSON（saveArgs=true 时采集） */
    private String snapshotJson;

    /** 返回值快照 JSON（saveResult=true 时采集） */
    private String resultJson;

    /** 异常信息（方法抛错时） */
    private String errorMsg;

    /** 耗时（毫秒） */
    private Long costMs;

    /** 关键动作标记（@TraceLog.keyAction） */
    private Boolean keyAction;

    /** 操作时间 */
    private Date operateTime;

    public String getScene()
    {
        return scene;
    }

    public void setScene(String scene)
    {
        this.scene = scene;
    }

    public String getBizType()
    {
        return bizType;
    }

    public void setBizType(String bizType)
    {
        this.bizType = bizType;
    }

    public String getBizId()
    {
        return bizId;
    }

    public void setBizId(String bizId)
    {
        this.bizId = bizId;
    }

    public String getOperator()
    {
        return operator;
    }

    public void setOperator(String operator)
    {
        this.operator = operator;
    }

    public String getOperateIp()
    {
        return operateIp;
    }

    public void setOperateIp(String operateIp)
    {
        this.operateIp = operateIp;
    }

    public String getTerminal()
    {
        return terminal;
    }

    public void setTerminal(String terminal)
    {
        this.terminal = terminal;
    }

    public String getMethodName()
    {
        return methodName;
    }

    public void setMethodName(String methodName)
    {
        this.methodName = methodName;
    }

    public String getSnapshotJson()
    {
        return snapshotJson;
    }

    public void setSnapshotJson(String snapshotJson)
    {
        this.snapshotJson = snapshotJson;
    }

    public String getResultJson()
    {
        return resultJson;
    }

    public void setResultJson(String resultJson)
    {
        this.resultJson = resultJson;
    }

    public String getErrorMsg()
    {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg)
    {
        this.errorMsg = errorMsg;
    }

    public Long getCostMs()
    {
        return costMs;
    }

    public void setCostMs(Long costMs)
    {
        this.costMs = costMs;
    }

    public Boolean getKeyAction()
    {
        return keyAction;
    }

    public void setKeyAction(Boolean keyAction)
    {
        this.keyAction = keyAction;
    }

    public Date getOperateTime()
    {
        return operateTime;
    }

    public void setOperateTime(Date operateTime)
    {
        this.operateTime = operateTime;
    }
}
