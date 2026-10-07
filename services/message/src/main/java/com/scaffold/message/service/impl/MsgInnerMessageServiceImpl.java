package com.scaffold.message.service.impl;

import com.scaffold.common.core.exception.ServiceException;
import com.scaffold.common.core.jpa.JpaSpecs;
import com.scaffold.common.core.utils.PageUtils;
import com.scaffold.common.core.web.page.PageDomain;
import com.scaffold.common.core.web.page.TableDataInfo;
import com.scaffold.common.security.utils.SecurityUtils;
import com.scaffold.message.api.domain.InnerSendDTO;
import com.scaffold.message.domain.MsgInnerMessage;
import com.scaffold.message.repository.MsgInnerMessageRepository;
import com.scaffold.message.service.IMessageSendService;
import com.scaffold.message.service.IMsgInnerMessageService;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 站内信消息服务实现（用户端接口服务端强制 RECEIVER_ID = 当前登录人，§10.1 越权防护）
 *
 * @author ct
 */
@Service
public class MsgInnerMessageServiceImpl implements IMsgInnerMessageService {
  private final MsgInnerMessageRepository messageRepository;

  private final IMessageSendService sendService;

  public MsgInnerMessageServiceImpl(
      MsgInnerMessageRepository messageRepository, IMessageSendService sendService) {
    this.messageRepository = messageRepository;
    this.sendService = sendService;
  }

  @Override
  public List<MsgInnerMessage> queryList(MsgInnerMessage query) {
    return messageRepository.list(toSpec(query), Sort.by(Sort.Direction.DESC, "id"));
  }

  /** 动态条件（对齐原 MsgInnerMessageMapper.xml selectList） */
  private Specification<Object> toSpec(MsgInnerMessage query) {
    if (query == null) {
      return JpaSpecs.alwaysTrue();
    }
    return JpaSpecs.eqIf("receiverId", query.getReceiverId())
        .and(JpaSpecs.likeIf("receiverName", query.getReceiverName()))
        .and(JpaSpecs.eqIfNotBlank("readFlag", query.getReadFlag()))
        .and(JpaSpecs.eqIfNotBlank("templateCode", query.getTemplateCode()))
        .and(JpaSpecs.eqIfNotBlank("bizType", query.getBizType()))
        .and(JpaSpecs.dateRangeIf("createTime", query.getParams()));
  }

  @Override
  public List<MsgInnerMessage> queryMine(String readFlag) {
    return messageRepository.list(
        JpaSpecs.eqIf("receiverId", SecurityUtils.getUserId())
            .and(JpaSpecs.eqIfNotBlank("readFlag", readFlag)),
        Sort.by(Sort.Direction.DESC, "id"));
  }

  @Override
  public TableDataInfo queryMinePage(String readFlag, PageDomain page) {
    return TableDataInfo.from(messageRepository.page(
        JpaSpecs.eqIf("receiverId", SecurityUtils.getUserId())
            .and(JpaSpecs.eqIfNotBlank("readFlag", readFlag)),
        PageUtils.toPageRequest(page).withSort(Sort.by(Sort.Direction.DESC, "id"))));
  }

  @Override
  public int unreadCount() {
    return (int) messageRepository.countByReceiverIdAndReadFlag(SecurityUtils.getUserId(), "0");
  }

  @Override
  @Transactional
  public int markRead(Long id) {
    // 服务端强制归属校验：不信任前端传参，非本人消息更新 0 行
    int rows = messageRepository.markRead(id, SecurityUtils.getUserId(), new Date());
    if (rows == 0) {
      // 已读（幂等）或非本人消息均返回 0，不区分提示避免探测
      MsgInnerMessage message = messageRepository.findById(id).orElse(null);
      if (message == null || !SecurityUtils.getUserId().equals(message.getReceiverId())) {
        throw new ServiceException("消息不存在");
      }
    }
    return rows;
  }

  @Override
  @Transactional
  public int markAllRead() {
    return messageRepository.markAllRead(SecurityUtils.getUserId(), new Date());
  }

  @Override
  public int deleteByIds(Long[] ids) {
    // 撤回（管理端）：未读才可删，已读留痕
    for (Long id : ids) {
      MsgInnerMessage message = messageRepository.findById(id).orElse(null);
      if (message != null && MsgInnerMessage.READ_YES.equals(message.getReadFlag())) {
        throw new ServiceException("已读消息留痕不可删除（id=" + id + "）");
      }
    }
    messageRepository.deleteAllById(Arrays.asList(ids));
    return ids.length;
  }

  @Override
  public List<Long> sendManual(
      String templateCode,
      Map<String, String> params,
      List<Long> receiverIds,
      String jumpUrl,
      String operator) {
    InnerSendDTO dto = new InnerSendDTO();
    dto.setTemplateCode(templateCode);
    dto.setParams(params);
    dto.setReceiverIds(receiverIds);
    dto.setJumpUrl(jumpUrl);
    dto.setBizType("MANUAL");
    dto.setOperator(operator);
    return sendService.sendInner(dto);
  }

  /**
   * 分页查询（JPA PageRequest 分页，分页参数取自请求上下文）
   */
  @Override
  public TableDataInfo queryPage(MsgInnerMessage query, PageDomain page) {
    return TableDataInfo.from(messageRepository.page(toSpec(query), PageUtils.toPageRequest(page)));
  }
}
