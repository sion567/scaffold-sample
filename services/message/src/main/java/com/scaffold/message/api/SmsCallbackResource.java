package com.scaffold.message.api;

import com.scaffold.common.core.web.domain.AjaxResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 厂商回执回调接口（§6.4：白名单放行，实现内验签 + channelMsgId 幂等； Mock 渠道同一接口手工模拟回执，供联调演示完整闭环）
 *
 * @author ct
 */
@RequestMapping("/message/callback/sms")
public interface SmsCallbackResource {
  /**
   * 短信回执：channelMsgId + 状态 + 失败原因
   *
   * @param success 厂商回执结果（true=送达 false=失败）
   * @param sign 回调签名（按渠道配置回调密钥校验，一期 Mock 免签）
   */
  @PostMapping("/receipt")
  AjaxResult receipt(
      @RequestParam("channelMsgId") String channelMsgId,
      @RequestParam("success") boolean success,
      @RequestParam(value = "failReason", required = false) String failReason,
      @RequestParam(value = "sign", required = false) String sign);
}
