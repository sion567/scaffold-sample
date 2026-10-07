package com.scaffold.message.service;

import com.scaffold.message.api.domain.InnerSendDTO;
import com.scaffold.message.api.domain.MailSendDTO;
import com.scaffold.message.api.domain.SmsSendDTO;
import java.util.List;

/**
 * 统一发送编排（§4.5：同步校验+渲染+落库(PENDING) 后立即返回 logId，@Async 执行真实发送）
 *
 * @author ct
 */
public interface IMessageSendService {
  /** 发送短信（幂等：重复返回已有 logId） */
  Long sendSms(SmsSendDTO dto);

  /** 发送邮件 */
  Long sendMail(MailSendDTO dto);

  /** 发送站内信（接收范围展开，返回接收人粒度 ID 列表） */
  List<Long> sendInner(InnerSendDTO dto);

  /** 失败重发（RETRY_COUNT + 1，上限 5） */
  Long retrySms(Long logId);

  Long retryMail(Long logId);

  /** 原文测试短信（不走模板：渠道连通测试，BIZ_TYPE=TEST） */
  Long sendRawSms(Long channelId, String mobile, String content, String operator);

  /** 原文测试邮件（不走模板：SMTP 连通测试，BIZ_TYPE=TEST） */
  Long sendRawMail(
      Long accountId, String toAddr, String subject, String htmlContent, String operator);
}
