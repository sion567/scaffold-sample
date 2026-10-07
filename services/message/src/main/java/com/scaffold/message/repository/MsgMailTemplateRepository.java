package com.scaffold.message.repository;


import org.springframework.stereotype.Repository;

import com.scaffold.common.core.jpa.ScaffoldRepository;
import com.scaffold.message.domain.MsgMailTemplate;

/**
 * 邮件模板数据访问（msg_mail_template）。
 *
 * @author ct
 */
@Repository
public interface MsgMailTemplateRepository extends ScaffoldRepository<MsgMailTemplate, Long>
{
    /** 模板编码唯一查询（未删除） */
    MsgMailTemplate findByTemplateCodeAndDelFlag(String templateCode, String delFlag);

    /** 账号引用计数（删除账号前检查） */
    long countByAccountIdAndDelFlag(Long accountId, String delFlag);
}
