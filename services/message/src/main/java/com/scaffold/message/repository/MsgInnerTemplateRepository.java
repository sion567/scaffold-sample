package com.scaffold.message.repository;


import org.springframework.stereotype.Repository;

import com.scaffold.common.core.jpa.ScaffoldRepository;
import com.scaffold.message.domain.MsgInnerTemplate;

/**
 * 站内信模板数据访问（msg_inner_template）。
 *
 * @author ct
 */
@Repository
public interface MsgInnerTemplateRepository extends ScaffoldRepository<MsgInnerTemplate, Long>
{
    /** 模板编码唯一查询（未删除） */
    MsgInnerTemplate findByTemplateCodeAndDelFlag(String templateCode, String delFlag);
}
