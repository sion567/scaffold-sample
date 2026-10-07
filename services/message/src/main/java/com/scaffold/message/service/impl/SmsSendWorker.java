package com.scaffold.message.service.impl;

import com.scaffold.common.core.utils.StringUtils;
import com.scaffold.message.domain.MsgSmsChannel;
import com.scaffold.message.domain.MsgSmsLog;
import com.scaffold.message.domain.MsgSmsTemplate;
import com.scaffold.message.gateway.SmsChannelClient;
import com.scaffold.message.gateway.SmsChannelConfig;
import com.scaffold.message.gateway.SmsSendResult;
import com.scaffold.message.repository.MsgSmsChannelRepository;
import com.scaffold.message.repository.MsgSmsLogRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 短信异步发送 Worker（独立 Bean 避免 @Async 自调用代理失效）： 状态机 PENDING → SENDING → SUCCESS/FAILED，通道结果只回写日志。
 *
 * @author ct
 */
@Component
public class SmsSendWorker {
  private final MsgSmsLogRepository smsLogRepository;

  private final MsgSmsChannelRepository channelRepository;

  private final Map<String, SmsChannelClient> clients = new HashMap<>();

  public SmsSendWorker(
      MsgSmsLogRepository smsLogRepository,
      MsgSmsChannelRepository channelRepository,
      List<SmsChannelClient> clientList) {
    this.smsLogRepository = smsLogRepository;
    this.channelRepository = channelRepository;
    for (SmsChannelClient client : clientList) {
      clients.put(client.channelType(), client);
    }
  }

  @Async("messageSendExecutor")
  public void send(Long logId) {
    MsgSmsLog log = smsLogRepository.findById(logId).orElse(null);
    if (log == null) {
      return;
    }
    log.setSendStatus(MsgSmsLog.STATUS_SENDING);
    smsLogRepository.save(log);

    try {
      MsgSmsChannel channel = channelRepository.findById(log.getChannelId()).orElse(null);
      if (channel == null) {
        throw new IllegalStateException("渠道不存在或已删除（id=" + log.getChannelId() + "）");
      }
      SmsChannelClient client = clients.get(channel.getChannelType());
      if (client == null) {
        throw new IllegalStateException("渠道类型暂未接入（" + channel.getChannelType() + "）");
      }
      SmsChannelConfig config = MessageSendServiceImpl.buildChannelConfig(channel);
      SmsSendResult result =
          client.send(config, log.getMobile(), log.getSignName(), log.getContent());
      if (result.isSuccess()) {
        log.setSendStatus(MsgSmsLog.STATUS_SUCCESS);
        log.setChannelMsgId(result.getChannelMsgId());
      } else {
        log.setSendStatus(MsgSmsLog.STATUS_FAILED);
        log.setFailReason(result.getFailReason());
      }
    } catch (Exception ex) {
      log.setSendStatus(MsgSmsLog.STATUS_FAILED);
      log.setFailReason(StringUtils.substring(ex.getMessage(), 0, 500));
    }
    smsLogRepository.save(log);
  }

  /** 幂等键缺省值：mobile + 模板 + 参数摘要 */
  public static String digestKey(
      String mobile, MsgSmsTemplate template, Map<String, String> params) {
    String raw = mobile + "|" + template.getTemplateCode() + "|" + params;
    return md5(raw);
  }

  public static String md5(String text) {
    try {
      MessageDigest digest = MessageDigest.getInstance("MD5");
      byte[] bytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));
      StringBuilder sb = new StringBuilder();
      for (byte b : bytes) {
        sb.append(String.format("%02x", b));
      }
      return sb.substring(0, 32);
    } catch (Exception ex) {
      // MD5 一定存在；兜底取哈希值十六进制
      return Integer.toHexString(text.hashCode());
    }
  }
}
