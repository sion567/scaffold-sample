package com.scaffold.message.gateway;

/**
 * 短信渠道客户端 SPI（gateway 包）：实现由 Spring 容器按 channelType 装配。
 *
 * <p>渠道类型：ALIYUN / TENCENT / HUAWEI / CTYUN / MOCK（字典 message_sms_channel_type）； 真实厂商 SDK 依赖按 §11
 * 分期在 P4 引入，一期默认 Mock。
 *
 * @author ct
 */
public interface SmsChannelClient {
  /** 渠道类型标识（对应 MSG_SMS_CHANNEL.CHANNEL_TYPE） */
  String channelType();

  /**
   * 发送短信：入参已渲染完成的正文（国内文本短信）
   *
   * @return 通道侧结果（channelMsgId 用于回执对账）
   */
  SmsSendResult send(SmsChannelConfig config, String mobile, String signName, String content);
}
