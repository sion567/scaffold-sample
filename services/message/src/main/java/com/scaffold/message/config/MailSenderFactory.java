package com.scaffold.message.config;

import com.scaffold.common.core.utils.StringUtils;
import com.scaffold.message.domain.MsgMailAccount;
import java.util.Properties;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

/**
 * MailSender 动态构建（§4.3：按 MSG_MAIL_ACCOUNT 行构建，不落 Spring 静态 Bean； 授权码此处已解密为明文，仅内存持有）。
 *
 * @author ct
 */
@Component
public class MailSenderFactory {
  public JavaMailSenderImpl create(MsgMailAccount account, String authCodePlain) {
    JavaMailSenderImpl sender = new JavaMailSenderImpl();
    sender.setHost(account.getSmtpHost());
    sender.setPort(account.getSmtpPort() == null ? 465 : account.getSmtpPort());
    sender.setUsername(account.getAuthUser());
    sender.setPassword(authCodePlain);
    sender.setDefaultEncoding("UTF-8");

    Properties props = sender.getJavaMailProperties();
    props.put("mail.transport.protocol", "smtp");
    props.put("mail.smtp.auth", "true");
    props.put("mail.smtp.connectiontimeout", "5000");
    props.put("mail.smtp.timeout", "10000");
    if (!"0".equals(account.getSmtpSsl())) {
      props.put("mail.smtp.ssl.enable", "true");
    }
    if (StringUtils.isNotEmpty(account.getNickName())) {
      props.put("mail.smtp.from", account.getEmailAddr());
    }
    return sender;
  }
}
