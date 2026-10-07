package com.scaffold.message.repository;


import org.springframework.stereotype.Repository;

import com.scaffold.common.core.jpa.ScaffoldRepository;
import com.scaffold.message.domain.MsgSmsLog;

/**
 * 短信日志数据访问（msg_sms_log，写入侧应用生成雪花主键）。
 *
 * @author ct
 */
@Repository
public interface MsgSmsLogRepository extends ScaffoldRepository<MsgSmsLog, Long>
{
    /** 幂等键查询 */
    MsgSmsLog findByBizTypeAndBizIdAndTemplateCodeAndIdempotentKey(
            String bizType, String bizId, String templateCode, String idempotentKey);

    /** 回执按渠道消息号定位 */
    MsgSmsLog findByChannelMsgId(String channelMsgId);
}
