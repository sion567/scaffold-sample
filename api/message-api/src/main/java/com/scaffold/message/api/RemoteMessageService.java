package com.scaffold.message.api;

import com.scaffold.common.core.domain.R;
import com.scaffold.message.api.domain.InnerSendDTO;
import com.scaffold.message.api.domain.MailSendDTO;
import com.scaffold.message.api.domain.SmsSendDTO;
import java.util.List;

/**
 * 消息中心跨模块发送契约（docs/消息中心模块设计文档.md §6.3）。
 *
 * <p>供无登录态的系统侧业务模块调用（如 ct-alert 预警通知），与 {@code RemoteNoticeService} 同款模式：调用方
 * {@code @DubboReference(check = false, retries = 0)}。
 *
 * <p>语义：同步完成"入参校验 + 模板渲染校验 + 日志落库(PENDING)"后立即返回日志 ID， 真实发送异步执行——返回值不代表通道结果，通道结果看日志状态（查询接口 + 回执）。
 *
 * @author ct
 */
public interface RemoteMessageService {
  /**
   * 发送短信（bizType + bizId + templateCode + idempotentKey 幂等，重复调用返回已有日志 ID）
   *
   * @return R&lt;Long&gt; 日志 ID（msg_sms_log.ID）
   */
  R<Long> sendSms(SmsSendDTO dto);

  /**
   * 发送邮件
   *
   * @return R&lt;Long&gt; 日志 ID（msg_mail_log.ID）
   */
  R<Long> sendMail(MailSendDTO dto);

  /**
   * 发送站内信：接收范围（receiverIds/roleKeys/allUser 三选一）展开后按人一行落库
   *
   * @return R&lt;List&lt;Long&gt;&gt; 日志 ID 列表（接收人粒度，msg_inner_message.ID）
   */
  R<List<Long>> sendInner(InnerSendDTO dto);
}
