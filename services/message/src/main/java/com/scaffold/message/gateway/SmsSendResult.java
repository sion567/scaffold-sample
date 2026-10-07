package com.scaffold.message.gateway;

/**
 * 短信通道发送结果
 *
 * @author ct
 */
public class SmsSendResult {
  /** 是否受理成功 */
  private final boolean success;

  /** 通道方消息 ID（回执对账键） */
  private final String channelMsgId;

  /** 失败原因 */
  private final String failReason;

  public SmsSendResult(boolean success, String channelMsgId, String failReason) {
    this.success = success;
    this.channelMsgId = channelMsgId;
    this.failReason = failReason;
  }

  public static SmsSendResult ok(String channelMsgId) {
    return new SmsSendResult(true, channelMsgId, null);
  }

  public static SmsSendResult fail(String failReason) {
    return new SmsSendResult(false, null, failReason);
  }

  public boolean isSuccess() {
    return success;
  }

  public String getChannelMsgId() {
    return channelMsgId;
  }

  public String getFailReason() {
    return failReason;
  }
}
