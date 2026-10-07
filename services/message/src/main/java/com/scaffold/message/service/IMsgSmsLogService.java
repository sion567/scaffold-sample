package com.scaffold.message.service;

import com.scaffold.common.core.web.page.PageDomain;
import com.scaffold.common.core.web.page.TableDataInfo;
import com.scaffold.message.domain.MsgSmsLog;
import java.util.List;

/**
 * 短信发送日志 服务层（重发委托 IMessageSendService）
 *
 * @author ct
 */
public interface IMsgSmsLogService {
  MsgSmsLog queryById(Long id);

  List<MsgSmsLog> queryList(MsgSmsLog query);

  int deleteByIds(Long[] ids);

  /**
   * 分页查询（JPA PageRequest 分页，分页参数取自请求上下文）
   */
  TableDataInfo queryPage(MsgSmsLog query, PageDomain page);
}
