package com.scaffold.message.api.impl;

import com.scaffold.common.core.web.controller.BaseController;
import com.scaffold.common.core.web.domain.AjaxResult;
import com.scaffold.message.api.SmsCallbackResource;
import com.scaffold.message.domain.MsgSmsLog;
import com.scaffold.message.repository.MsgSmsLogRepository;
import java.util.Date;
import org.apache.dubbo.config.annotation.DubboService;

/**
 * 厂商回执实现（§6.4：channelMsgId 幂等，已终态直接 200； 负向回执把 SUCCESS 修正为 FAILED，正向回执回填 RECEIPT_TIME）。 网关白名单放行
 * /message/callback/sms/receipt（security.ignore.whites）。
 *
 * @author ct
 */
@DubboService
public class SmsCallbackResourceImpl extends BaseController implements SmsCallbackResource {
  private final MsgSmsLogRepository smsLogRepository;

  public SmsCallbackResourceImpl(MsgSmsLogRepository smsLogRepository) {
    this.smsLogRepository = smsLogRepository;
  }

  @Override
  public AjaxResult receipt(String channelMsgId, boolean success, String failReason, String sign) {
    // TODO(P4): 按渠道配置回调密钥验签 + 时间窗（±5 分钟防重放）；一期 Mock 免签联调
    MsgSmsLog log = smsLogRepository.findByChannelMsgId(channelMsgId);
    if (log == null) {
      return error("回执无匹配日志（channelMsgId=" + channelMsgId + "）");
    }
    // 幂等：已终态 SUCCESS 且再次正向回执直接 200；负向回执允许把 SUCCESS 修正为 FAILED（§4.5）
    if (MsgSmsLog.STATUS_SUCCESS.equals(log.getSendStatus()) && success) {
      return success();
    }
    if (success) {
      log.setSendStatus(MsgSmsLog.STATUS_SUCCESS);
      log.setReceiptTime(new Date());
    } else {
      log.setSendStatus(MsgSmsLog.STATUS_FAILED);
      log.setFailReason(failReason);
      log.setReceiptTime(new Date());
    }
    smsLogRepository.save(log);
    // TODO(P4): 转发 Kafka comm.message.receipt 供 ct-command 消费（§3.2 ④）
    return success();
  }
}
