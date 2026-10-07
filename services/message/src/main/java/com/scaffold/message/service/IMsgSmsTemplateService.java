package com.scaffold.message.service;

import com.scaffold.common.core.web.page.PageDomain;
import com.scaffold.common.core.web.page.TableDataInfo;
import com.scaffold.message.domain.MsgSmsTemplate;

/**
 * 短信模板 服务层
 *
 * @author ct
 */
public interface IMsgSmsTemplateService extends IMsgCrudService<MsgSmsTemplate> {
  /**
   * 分页查询（JPA PageRequest 分页，分页参数取自请求上下文）
   */
  TableDataInfo queryPage(MsgSmsTemplate query, PageDomain page);
}
