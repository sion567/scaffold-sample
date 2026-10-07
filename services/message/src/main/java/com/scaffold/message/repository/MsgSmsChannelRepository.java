package com.scaffold.message.repository;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.scaffold.common.core.jpa.ScaffoldRepository;
import com.scaffold.message.domain.MsgSmsChannel;

/**
 * 短信渠道数据访问（msg_sms_channel）。
 *
 * @author ct
 */
@Repository
public interface MsgSmsChannelRepository extends ScaffoldRepository<MsgSmsChannel, Long>
{
    /** 渠道名称唯一查询（未删除） */
    MsgSmsChannel findByChannelNameAndDelFlag(String channelName, String delFlag);

    /** 按类型取启用渠道（优先级升序 failover） */
    List<MsgSmsChannel> findByChannelTypeAndStatusAndDelFlagOrderByPriorityAscIdAsc(
            String channelType, String status, String delFlag);
}
