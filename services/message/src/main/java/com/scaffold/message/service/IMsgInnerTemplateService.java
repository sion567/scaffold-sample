package com.scaffold.message.service;

import com.scaffold.common.core.web.page.PageDomain;
import com.scaffold.common.core.web.page.TableDataInfo;
import com.scaffold.message.domain.MsgInnerTemplate;

/**
 * 站内信模板 服务层
 *
 * @author ct
 */
public interface IMsgInnerTemplateService extends IMsgCrudService<MsgInnerTemplate> {
  /**
   * 分页查询（JPA PageRequest 分页，分页参数取自请求上下文）
   */
  TableDataInfo queryPage(MsgInnerTemplate query, PageDomain page);
}
