package com.scaffold.message.service;

import com.scaffold.common.core.web.page.PageDomain;
import com.scaffold.common.core.web.page.TableDataInfo;
import com.scaffold.message.domain.MsgMailAccount;

/**
 * 邮箱账号 服务层
 *
 * @author ct
 */
public interface IMsgMailAccountService extends IMsgCrudService<MsgMailAccount> {
  /** SMTP 连通测试（BIZ_TYPE=TEST） */
  Long testAccount(Long id, String toAddr);

  /**
   * 分页查询（JPA PageRequest 分页，分页参数取自请求上下文）
   */
  TableDataInfo queryPage(MsgMailAccount query, PageDomain page);
}
