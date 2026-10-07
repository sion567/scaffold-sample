package com.scaffold.message.dubbo;

import com.scaffold.common.core.domain.R;
import com.scaffold.common.core.exception.ServiceException;
import com.scaffold.message.api.RemoteMessageService;
import com.scaffold.message.api.domain.InnerSendDTO;
import com.scaffold.message.api.domain.MailSendDTO;
import com.scaffold.message.api.domain.SmsSendDTO;
import com.scaffold.message.service.IMessageSendService;
import java.util.List;
import org.apache.dubbo.config.annotation.DubboService;

/**
 * 跨模块发送契约实现（无登录态系统侧调用，与 RemoteNoticeService 同款模式）。 异常交给 TripleExceptionFilter 统一转 AjaxResult JSON。
 *
 * @author ct
 */
@DubboService
public class RemoteMessageServiceImpl implements RemoteMessageService {
  private final IMessageSendService sendService;

  public RemoteMessageServiceImpl(IMessageSendService sendService) {
    this.sendService = sendService;
  }

  @Override
  public R<Long> sendSms(SmsSendDTO dto) {
    if (dto == null || dto.getMobile() == null) {
      return R.fail("入参不完整（mobile 必填）");
    }
    try {
      return R.ok(sendService.sendSms(dto));
    } catch (ServiceException ex) {
      return R.fail(ex.getMessage());
    }
  }

  @Override
  public R<Long> sendMail(MailSendDTO dto) {
    if (dto == null || dto.getTo() == null || dto.getTo().isEmpty()) {
      return R.fail("入参不完整（to 必填）");
    }
    try {
      return R.ok(sendService.sendMail(dto));
    } catch (ServiceException ex) {
      return R.fail(ex.getMessage());
    }
  }

  @Override
  public R<List<Long>> sendInner(InnerSendDTO dto) {
    if (dto == null) {
      return R.fail("入参不完整");
    }
    try {
      return R.ok(sendService.sendInner(dto));
    } catch (ServiceException ex) {
      return R.fail(ex.getMessage());
    }
  }
}
