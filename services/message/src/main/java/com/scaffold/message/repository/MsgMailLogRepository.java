package com.scaffold.message.repository;


import java.util.Date;

import org.springframework.stereotype.Repository;

import com.scaffold.common.core.jpa.ScaffoldRepository;
import com.scaffold.message.domain.MsgMailLog;

/**
 * 邮件日志数据访问（msg_mail_log，写入侧应用生成雪花主键）。
 *
 * @author ct
 */
@Repository
public interface MsgMailLogRepository extends ScaffoldRepository<MsgMailLog, Long>
{
    /** 幂等键查询 */
    MsgMailLog findByBizTypeAndBizIdAndTemplateCodeAndIdempotentKey(
            String bizType, String bizId, String templateCode, String idempotentKey);

    /** 当日发送成功计数（每日限额用；起始时间为当日零点，由调用方按本地时钟计算） */
    long countByAccountIdAndSendStatusAndSendTimeGreaterThanEqual(
            Long accountId, String sendStatus, Date since);
}
