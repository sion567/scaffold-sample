package com.scaffold.message.service.impl;

import com.scaffold.common.core.jpa.JpaSpecs;
import com.scaffold.common.core.utils.PageUtils;
import com.scaffold.common.core.web.page.PageDomain;
import com.scaffold.common.core.web.page.TableDataInfo;
import com.scaffold.message.domain.MsgMailLog;
import com.scaffold.message.repository.MsgMailLogRepository;
import com.scaffold.message.service.IMsgMailLogService;
import java.util.Arrays;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

/**
 * 邮件发送记录服务实现
 *
 * @author ct
 */
@Service
public class MsgMailLogServiceImpl implements IMsgMailLogService {
  private final MsgMailLogRepository logRepository;

  public MsgMailLogServiceImpl(MsgMailLogRepository logRepository) {
    this.logRepository = logRepository;
  }

  @Override
  public MsgMailLog queryById(Long id) {
    return logRepository.findById(id).orElse(null);
  }

  @Override
  public List<MsgMailLog> queryList(MsgMailLog query) {
    return logRepository.list(toSpec(query), Sort.by(Sort.Direction.DESC, "id"));
  }


  /**
   * 分页查询（JPA PageRequest 分页，分页参数取自请求上下文）
   */
  @Override
  public TableDataInfo queryPage(MsgMailLog query, PageDomain page) {
    return TableDataInfo.from(logRepository.page(toSpec(query), PageUtils.toPageRequest(page)));
  }
  /** 动态条件（对齐原 MsgMailLogMapper.xml selectList） */
  private Specification<Object> toSpec(MsgMailLog query) {
    if (query == null) {
      return JpaSpecs.alwaysTrue();
    }
    return JpaSpecs.likeIf("toAddrs", query.getToAddrs())
        .and(JpaSpecs.eqIfNotBlank("sendStatus", query.getSendStatus()))
        .and(JpaSpecs.eqIfNotBlank("templateCode", query.getTemplateCode()))
        .and(JpaSpecs.eqIfNotBlank("bizType", query.getBizType()))
        .and(JpaSpecs.dateRangeIf("sendTime", query.getParams()));
  }

  @Override
  public int deleteByIds(Long[] ids) {
    logRepository.deleteAllById(Arrays.asList(ids));
    return ids.length;
  }
}
