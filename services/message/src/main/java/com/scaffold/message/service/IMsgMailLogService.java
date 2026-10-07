package com.scaffold.message.service;

import com.scaffold.common.core.web.page.PageDomain;
import com.scaffold.common.core.web.page.TableDataInfo;
import com.scaffold.message.domain.MsgMailLog;
import java.util.List;

/**
 * 邮件发送记录 服务层
 *
 * @author ct
 */
public interface IMsgMailLogService {
  MsgMailLog queryById(Long id);

  List<MsgMailLog> queryList(MsgMailLog query);

  int deleteByIds(Long[] ids);

  /**
   * 分页查询（JPA PageRequest 分页，分页参数取自请求上下文）
   */
  TableDataInfo queryPage(MsgMailLog query, PageDomain page);
}
