package com.scaffold.auth.service;

import org.springframework.stereotype.Component;

import com.scaffold.common.core.constant.Constants;
import com.scaffold.common.core.utils.StringUtils;
import com.scaffold.common.core.utils.ip.IpUtils;
import com.scaffold.common.log.persist.LogPersister;
import com.scaffold.system.api.domain.SysLogininfor;

/**
 * 记录日志方法：登录/登出/注册/失败等认证事件经 {@link LogPersister} SPI 落库
 * （默认 SLF4J 输出，接入方注册自己的 LogPersister Bean 即可替换为远程审计服务）。
 *
 * @author scaffold
 */
@Component
public class SysRecordLogService
{
    private final LogPersister logPersister;

    public SysRecordLogService(LogPersister logPersister)
    {
        this.logPersister = logPersister;
    }

    /**
     * 记录登录信息
     *
     * @param username 用户名
     * @param status 状态
     * @param message 消息内容
     */
    public void recordLogininfor(String username, String status, String message)
    {
        SysLogininfor logininfor = new SysLogininfor();
        logininfor.setUserName(username);
        logininfor.setIpaddr(IpUtils.getIpAddr());
        logininfor.setMsg(message);
        // 日志状态
        if (StringUtils.equalsAny(status, Constants.LOGIN_SUCCESS, Constants.LOGOUT, Constants.REGISTER))
        {
            logininfor.setStatus(Constants.LOGIN_SUCCESS_STATUS);
        }
        else if (Constants.LOGIN_FAIL.equals(status))
        {
            logininfor.setStatus(Constants.LOGIN_FAIL_STATUS);
        }
        logPersister.saveLogininfor(logininfor);
    }
}
