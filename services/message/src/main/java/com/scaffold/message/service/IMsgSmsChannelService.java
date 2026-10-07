package com.scaffold.message.service;

import com.scaffold.common.core.web.page.PageDomain;
import com.scaffold.common.core.web.page.TableDataInfo;
import com.scaffold.message.domain.MsgSmsChannel;

/**
 * 短信渠道 服务层
 *
 * @author ct
 */
public interface IMsgSmsChannelService extends IMsgCrudService<MsgSmsChannel> {
  /** 连通测试（向指定手机发测试短信，BIZ_TYPE=TEST 跳过幂等） */
  Long testChannel(Long id, String mobile);

  /**
   * 分页查询（JPA PageRequest 分页，分页参数取自请求上下文）
   */
  TableDataInfo queryPage(MsgSmsChannel query, PageDomain page);
}
