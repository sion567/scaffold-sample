package com.scaffold.message.service.impl;

import com.scaffold.common.core.jpa.JpaSpecs;
import com.scaffold.common.core.utils.PageUtils;
import com.scaffold.common.core.web.page.PageDomain;
import com.scaffold.common.core.web.page.TableDataInfo;
import com.scaffold.message.domain.MsgSmsLog;
import com.scaffold.message.repository.MsgSmsLogRepository;
import com.scaffold.message.service.IMsgSmsLogService;
import java.util.Arrays;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

/**
 * 短信发送日志服务实现（查询/清理；重发在 Resource 层委托 IMessageSendService）
 *
 * @author ct
 */
@Service
public class MsgSmsLogServiceImpl implements IMsgSmsLogService {
  private final MsgSmsLogRepository logRepository;

  public MsgSmsLogServiceImpl(MsgSmsLogRepository logRepository) {
    this.logRepository = logRepository;
  }

  @Override
  public MsgSmsLog queryById(Long id) {
    return logRepository.findById(id).orElse(null);
  }

  @Override
  public List<MsgSmsLog> queryList(MsgSmsLog query) {
    return logRepository.list(toSpec(query), Sort.by(Sort.Direction.DESC, "id"));
  }


  /**
   * 分页查询（JPA PageRequest 分页，分页参数取自请求上下文）
   */
  @Override
  public TableDataInfo queryPage(MsgSmsLog query, PageDomain page) {
    return TableDataInfo.from(logRepository.page(toSpec(query), PageUtils.toPageRequest(page)));
  }
  /** 动态条件（对齐原 MsgSmsLogMapper.xml selectList） */
  private Specification<Object> toSpec(MsgSmsLog query) {
    if (query == null) {
      return JpaSpecs.alwaysTrue();
    }
    return JpaSpecs.likeIf("mobile", query.getMobile())
        .and(JpaSpecs.eqIfNotBlank("sendStatus", query.getSendStatus()))
        .and(JpaSpecs.eqIfNotBlank("templateCode", query.getTemplateCode()))
        .and(JpaSpecs.eqIfNotBlank("channelType", query.getChannelType()))
        .and(JpaSpecs.eqIfNotBlank("bizType", query.getBizType()))
        .and(JpaSpecs.dateRangeIf("sendTime", query.getParams()));
  }

  @Override
  public int deleteByIds(Long[] ids) {
    logRepository.deleteAllById(Arrays.asList(ids));
    return ids.length;
  }
}
