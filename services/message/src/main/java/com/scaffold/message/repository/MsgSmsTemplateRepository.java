package com.scaffold.message.repository;


import org.springframework.stereotype.Repository;

import com.scaffold.common.core.jpa.ScaffoldRepository;
import com.scaffold.message.domain.MsgSmsTemplate;

/**
 * 短信模板数据访问（msg_sms_template）。
 *
 * @author ct
 */
@Repository
public interface MsgSmsTemplateRepository extends ScaffoldRepository<MsgSmsTemplate, Long>
{
    /** 模板编码唯一查询（未删除） */
    MsgSmsTemplate findByTemplateCodeAndDelFlag(String templateCode, String delFlag);

    /** 渠道引用计数（删除渠道前检查） */
    long countByChannelIdAndDelFlag(Long channelId, String delFlag);
}
