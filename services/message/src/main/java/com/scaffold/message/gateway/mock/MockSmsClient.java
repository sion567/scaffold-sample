package com.scaffold.message.gateway.mock;

import com.scaffold.message.gateway.SmsChannelClient;
import com.scaffold.message.gateway.SmsChannelConfig;
import com.scaffold.message.gateway.SmsSendResult;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/**
 * Mock 短信通道（默认实现：本地/联调/演示零外部依赖）。
 *
 * <p>受理即成功并返回通道消息 ID；回执闭环可用 POST /message/callback/sms/receipt 手工模拟。
 *
 * @author ct
 */
@Component
public class MockSmsClient implements SmsChannelClient {
  private static final String CHANNEL_TYPE = "MOCK";

  private static final AtomicLong SEQ = new AtomicLong();

  @Override
  public String channelType() {
    return CHANNEL_TYPE;
  }

  @Override
  public SmsSendResult send(
      SmsChannelConfig config, String mobile, String signName, String content) {
    String channelMsgId =
        "MOCK-"
            + UUID.randomUUID().toString().substring(0, 8).toUpperCase()
            + "-"
            + SEQ.incrementAndGet();
    return SmsSendResult.ok(channelMsgId);
  }
}
