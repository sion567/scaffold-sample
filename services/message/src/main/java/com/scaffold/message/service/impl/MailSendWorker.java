package com.scaffold.message.service.impl;

import com.scaffold.common.core.utils.StringUtils;
import com.scaffold.common.core.utils.crypto.Sm4FieldCrypto;
import com.scaffold.message.config.MailSenderFactory;
import com.scaffold.message.domain.MsgMailAccount;
import com.scaffold.message.domain.MsgMailLog;
import com.scaffold.message.repository.MsgMailAccountRepository;
import com.scaffold.message.repository.MsgMailLogRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 邮件异步发送 Worker（§7.2：动态构建 JavaMailSender → MIME → SMTP； 无回执链路，SMTP 异常即终态）。
 *
 * @author ct
 */
@Component
public class MailSendWorker {
  private final MsgMailLogRepository mailLogRepository;

  private final MsgMailAccountRepository accountRepository;

  private final MailSenderFactory mailSenderFactory;

  public MailSendWorker(
      MsgMailLogRepository mailLogRepository,
      MsgMailAccountRepository accountRepository,
      MailSenderFactory mailSenderFactory) {
    this.mailLogRepository = mailLogRepository;
    this.accountRepository = accountRepository;
    this.mailSenderFactory = mailSenderFactory;
  }

  @Async("messageSendExecutor")
  public void send(Long logId) {
    MsgMailLog log = mailLogRepository.findById(logId).orElse(null);
    if (log == null) {
      return;
    }
    log.setSendStatus(MsgMailLog.STATUS_SENDING);
    mailLogRepository.save(log);
    try {
      MsgMailAccount account = accountRepository.findById(log.getAccountId()).orElse(null);
      if (account == null) {
        throw new IllegalStateException("发件账号不存在（id=" + log.getAccountId() + "）");
      }
      String authCode = Sm4FieldCrypto.decrypt(account.getAuthCode());
      JavaMailSenderImpl sender = mailSenderFactory.create(account, authCode);
      sender.send(buildMimeMessage(sender, account, log));
      log.setSendStatus(MsgMailLog.STATUS_SUCCESS);
    } catch (Exception ex) {
      log.setSendStatus(MsgMailLog.STATUS_FAILED);
      log.setFailReason(StringUtils.substring(ex.getMessage(), 0, 500));
    }
    mailLogRepository.save(log);
  }

  private MimeMessage buildMimeMessage(
      JavaMailSenderImpl sender, MsgMailAccount account, MsgMailLog log)
      throws MessagingException, UnsupportedEncodingException {
    MimeMessage message = sender.createMimeMessage();
    boolean html = !"0".equals(log.getIsHtml());
    // TODO(P4/ct-file): ATTACH_FILE_IDS 附件经 api-file 取流挂载（一期快照留痕不挂实体附件）
    MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
    String fromAddr = account.getEmailAddr();
    if (StringUtils.isNotEmpty(account.getNickName())) {
      helper.setFrom(new InternetAddress(fromAddr, account.getNickName(), "UTF-8"));
    } else {
      helper.setFrom(fromAddr);
    }
    helper.setTo(log.getToAddrs().split(","));
    if (StringUtils.isNotEmpty(log.getCcAddrs())) {
      helper.setCc(log.getCcAddrs().split(","));
    }
    helper.setSubject(log.getSubject());
    helper.setText(log.getContent(), html);
    helper.setSentDate(new Date());
    return message;
  }
}
