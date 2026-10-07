package com.scaffold.message.service.impl;

import com.scaffold.common.core.exception.ServiceException;
import com.scaffold.common.core.utils.StringUtils;
import com.scaffold.common.core.utils.crypto.Sm4FieldCrypto;
import com.scaffold.common.core.utils.uuid.SnowflakeIdGenerator;
import com.scaffold.message.api.domain.InnerSendDTO;
import com.scaffold.message.api.domain.MailSendDTO;
import com.scaffold.message.api.domain.SmsSendDTO;
import com.scaffold.message.domain.MsgInnerMessage;
import com.scaffold.message.domain.MsgInnerTemplate;
import com.scaffold.message.domain.MsgMailAccount;
import com.scaffold.message.domain.MsgMailLog;
import com.scaffold.message.domain.MsgMailTemplate;
import com.scaffold.message.domain.MsgSmsChannel;
import com.scaffold.message.domain.MsgSmsLog;
import com.scaffold.message.domain.MsgSmsTemplate;
import com.scaffold.message.gateway.SmsChannelConfig;
import com.scaffold.message.repository.MsgInnerMessageRepository;
import com.scaffold.message.repository.MsgInnerTemplateRepository;
import com.scaffold.message.repository.MsgMailAccountRepository;
import com.scaffold.message.repository.MsgMailLogRepository;
import com.scaffold.message.repository.MsgMailTemplateRepository;
import com.scaffold.message.repository.MsgSmsChannelRepository;
import com.scaffold.message.repository.MsgSmsLogRepository;
import com.scaffold.message.repository.MsgSmsTemplateRepository;
import com.scaffold.message.service.IMessageSendService;
import com.scaffold.message.service.TemplateRenderer;
import com.scaffold.system.api.UserDirectoryItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * 统一发送编排实现（§4.5/§4.6/§7）： 同步完成"入参校验 + 模板渲染校验 + 幂等检查 + 落库(PENDING)"立即返回 logId；
 * 异步执行真实发送，通道结果看日志状态（查询接口 + 回执）。
 *
 * @author ct
 */
@Service
public class MessageSendServiceImpl implements IMessageSendService {
  private static final String BIZ_TEST = "TEST";

  /** 站内信批量插入每批条数 */
  private static final int INNER_BATCH_SIZE = 500;

  private static final ObjectMapper JSON = new ObjectMapper();

  private final MsgSmsTemplateRepository smsTemplateRepository;

  private final MsgSmsChannelRepository smsChannelRepository;

  private final MsgSmsLogRepository smsLogRepository;

  private final MsgMailTemplateRepository mailTemplateRepository;

  private final MsgMailAccountRepository mailAccountRepository;

  private final MsgMailLogRepository mailLogRepository;

  private final MsgInnerTemplateRepository innerTemplateRepository;

  private final MsgInnerMessageRepository innerMessageRepository;

  private final UserDirectoryClient userDirectoryClient;

  private final SnowflakeIdGenerator idGenerator;

  private final SmsSendWorker smsSendWorker;

  private final MailSendWorker mailSendWorker;

  public MessageSendServiceImpl(
      MsgSmsTemplateRepository smsTemplateRepository,
      MsgSmsChannelRepository smsChannelRepository,
      MsgSmsLogRepository smsLogRepository,
      MsgMailTemplateRepository mailTemplateRepository,
      MsgMailAccountRepository mailAccountRepository,
      MsgMailLogRepository mailLogRepository,
      MsgInnerTemplateRepository innerTemplateRepository,
      MsgInnerMessageRepository innerMessageRepository,
      UserDirectoryClient userDirectoryClient,
      SnowflakeIdGenerator idGenerator,
      SmsSendWorker smsSendWorker,
      MailSendWorker mailSendWorker) {
    this.smsTemplateRepository = smsTemplateRepository;
    this.smsChannelRepository = smsChannelRepository;
    this.smsLogRepository = smsLogRepository;
    this.mailTemplateRepository = mailTemplateRepository;
    this.mailAccountRepository = mailAccountRepository;
    this.mailLogRepository = mailLogRepository;
    this.innerTemplateRepository = innerTemplateRepository;
    this.innerMessageRepository = innerMessageRepository;
    this.userDirectoryClient = userDirectoryClient;
    this.idGenerator = idGenerator;
    this.smsSendWorker = smsSendWorker;
    this.mailSendWorker = mailSendWorker;
  }

  // ==================== 短信 ====================

  @Override
  public Long sendSms(SmsSendDTO dto) {
    MsgSmsTemplate template = requireEnabledSmsTemplate(dto.getTemplateCode());
    Map<String, String> params = dto.getParams() == null ? Map.of() : dto.getParams();

    MsgSmsLog log = new MsgSmsLog();
    log.setTemplateId(template.getId());
    log.setTemplateCode(template.getTemplateCode());
    log.setMobile(dto.getMobile());
    log.setSignName(StringUtils.isNotEmpty(template.getSignName()) ? template.getSignName() : null);
    log.setSendStatus(MsgSmsLog.STATUS_PENDING);
    log.setRetryCount(0);
    log.setSendTime(new Date());
    log.setBizType(StringUtils.isEmpty(dto.getBizType()) ? BIZ_TEST : dto.getBizType());
    log.setBizId(dto.getBizId());
    log.setCreateBy(dto.getOperator());

    String content;
    if (MsgSmsTemplate.MODE_VENDOR_CODE.equals(template.getTemplateMode())) {
      // 厂商模板模式：正文快照存参数说明，真实参数由通道侧按 VENDOR_CODE 组装
      content = "vendor:" + template.getVendorCode() + " " + params;
    } else {
      content = TemplateRenderer.render(template.getTemplateContent(), params, null);
      if (content.length() > 500) {
        throw new ServiceException("短信正文超过500字上限（当前" + content.length() + "字）");
      }
    }
    log.setContent(content);
    try {
      log.setParamJson(JSON.writeValueAsString(params));
    } catch (Exception ignored) {
      log.setParamJson(String.valueOf(params));
    }

    // 幂等：非 TEST 业务命中唯一约束返回已有 logId，不重复发送
    if (!BIZ_TEST.equals(log.getBizType())) {
      String idemKey =
          StringUtils.isNotEmpty(dto.getIdempotentKey())
              ? dto.getIdempotentKey()
              : SmsSendWorker.digestKey(dto.getMobile(), template, params);
      log.setIdempotentKey(idemKey);
      MsgSmsLog existing =
          smsLogRepository.findByBizTypeAndBizIdAndTemplateCodeAndIdempotentKey(
              log.getBizType(), log.getBizId(), template.getTemplateCode(), idemKey);
      if (existing != null) {
        return existing.getId();
      }
    }

    MsgSmsChannel channel = routeChannel(template);
    log.setChannelId(channel.getId());
    log.setChannelType(channel.getChannelType());
    if (StringUtils.isEmpty(log.getSignName()) && StringUtils.isNotEmpty(channel.getSignName())) {
      log.setSignName(channel.getSignName());
    }

    log.setId(idGenerator.nextId());
    try {
      smsLogRepository.save(log);
    } catch (DataIntegrityViolationException ex) {
      MsgSmsLog existing =
          smsLogRepository.findByBizTypeAndBizIdAndTemplateCodeAndIdempotentKey(
              log.getBizType(), log.getBizId(), template.getTemplateCode(), log.getIdempotentKey());
      if (existing != null) {
        return existing.getId();
      }
      throw ex;
    }
    smsSendWorker.send(log.getId());
    return log.getId();
  }

  /** 原文测试邮件（SMTP 连通测试，不走模板表） */
  @Override
  public Long sendRawMail(
      Long accountId, String toAddr, String subject, String htmlContent, String operator) {
    MsgMailAccount account = mailAccountRepository.findById(accountId).orElse(null);
    if (account == null || !"0".equals(account.getStatus())) {
      throw new ServiceException("发件账号不存在或未启用（id=" + accountId + "）");
    }
    MsgMailLog log = new MsgMailLog();
    log.setId(idGenerator.nextId());
    log.setAccountId(accountId);
    log.setToAddrs(toAddr);
    log.setSubject(subject);
    log.setContent(htmlContent);
    log.setIsHtml("1");
    log.setSendStatus(MsgMailLog.STATUS_PENDING);
    log.setRetryCount(0);
    log.setSendTime(new Date());
    log.setBizType("TEST");
    log.setCreateBy(operator);
    mailLogRepository.save(log);
    mailSendWorker.send(log.getId());
    return log.getId();
  }

  @Override
  public Long retrySms(Long logId) {
    MsgSmsLog log = smsLogRepository.findById(logId).orElse(null);
    if (log == null) {
      throw new ServiceException("短信日志不存在");
    }
    if (!MsgSmsLog.STATUS_FAILED.equals(log.getSendStatus())) {
      throw new ServiceException("仅失败记录可重发（当前状态：" + log.getSendStatus() + "）");
    }
    if (log.getRetryCount() != null && log.getRetryCount() >= 5) {
      throw new ServiceException("重发次数已达上限（5次）");
    }
    log.setRetryCount((log.getRetryCount() == null ? 0 : log.getRetryCount()) + 1);
    log.setSendStatus(MsgSmsLog.STATUS_SENDING);
    log.setFailReason(null);
    smsLogRepository.save(log);
    smsSendWorker.send(logId);
    return logId;
  }

  @Override
  public Long sendRawSms(Long channelId, String mobile, String content, String operator) {
    MsgSmsChannel channel = smsChannelRepository.findById(channelId).orElse(null);
    if (channel == null || !"0".equals(channel.getStatus())) {
      throw new ServiceException("渠道不存在或未启用（id=" + channelId + "）");
    }
    MsgSmsLog log = new MsgSmsLog();
    log.setId(idGenerator.nextId());
    log.setChannelId(channel.getId());
    log.setChannelType(channel.getChannelType());
    log.setMobile(mobile);
    log.setSignName(channel.getSignName());
    log.setContent(content);
    log.setSendStatus(MsgSmsLog.STATUS_PENDING);
    log.setRetryCount(0);
    log.setSendTime(new Date());
    log.setBizType("TEST");
    log.setCreateBy(operator);
    smsLogRepository.save(log);
    smsSendWorker.send(log.getId());
    return log.getId();
  }

  /** 渠道路由：模板绑定渠道定向走，否则按优先级取启用渠道 */
  private MsgSmsChannel routeChannel(MsgSmsTemplate template) {
    if (template.getChannelId() != null) {
      MsgSmsChannel bound = smsChannelRepository.findById(template.getChannelId()).orElse(null);
      if (bound == null || !"0".equals(bound.getStatus())) {
        throw new ServiceException("模板绑定渠道未启用（channelId=" + template.getChannelId() + "）");
      }
      return bound;
    }
    MsgSmsChannel query = new MsgSmsChannel();
    query.setStatus("0");
    List<MsgSmsChannel> enabled =
        smsChannelRepository.findByChannelTypeAndStatusAndDelFlagOrderByPriorityAscIdAsc(
            query.getChannelType(), query.getStatus(), "0");
    if (enabled.isEmpty()) {
      throw new ServiceException("无可用短信渠道（请先配置并启用渠道）");
    }
    return enabled.get(0);
  }

  /** 渠道行 → 运行时配置（SecretKey 解密为明文，仅内存持有） */
  static SmsChannelConfig buildChannelConfig(MsgSmsChannel channel) {
    SmsChannelConfig config = new SmsChannelConfig();
    config.setChannelId(channel.getId());
    config.setChannelType(channel.getChannelType());
    config.setAccessKey(channel.getAccessKey());
    config.setSecretKey(Sm4FieldCrypto.decrypt(channel.getSecretKey()));
    config.setSignName(channel.getSignName());
    config.setEndpoint(channel.getEndpoint());
    config.setRegion(channel.getRegion());
    return config;
  }

  private MsgSmsTemplate requireEnabledSmsTemplate(String templateCode) {
    MsgSmsTemplate template =
        smsTemplateRepository.findByTemplateCodeAndDelFlag(templateCode, "0");
    if (template == null) {
      throw new ServiceException("短信模板不存在（" + templateCode + "）");
    }
    if (!"0".equals(template.getStatus())) {
      throw new ServiceException("短信模板已停用（" + templateCode + "）");
    }
    return template;
  }

  // ==================== 邮件 ====================

  @Override
  public Long sendMail(MailSendDTO dto) {
    MsgMailTemplate template =
        mailTemplateRepository.findByTemplateCodeAndDelFlag(dto.getTemplateCode(), "0");
    if (template == null) {
      throw new ServiceException("邮件模板不存在（" + dto.getTemplateCode() + "）");
    }
    if (!"0".equals(template.getStatus())) {
      throw new ServiceException("邮件模板已停用（" + dto.getTemplateCode() + "）");
    }
    if (dto.getTo() == null || dto.getTo().isEmpty()) {
      throw new ServiceException("收件人不能为空");
    }
    if (dto.getTo().size() > 50) {
      throw new ServiceException("收件人超过50个上限");
    }
    Map<String, String> params = dto.getParams() == null ? Map.of() : dto.getParams();

    MsgMailAccount account;
    if (template.getAccountId() != null) {
      account = mailAccountRepository.findById(template.getAccountId()).orElse(null);
      if (account == null || !"0".equals(account.getStatus())) {
        throw new ServiceException("模板绑定发件账号未启用（accountId=" + template.getAccountId() + "）");
      }
    } else {
      java.util.List<com.scaffold.message.domain.MsgMailAccount> defaults =
          mailAccountRepository.findByStatusAndDelFlagOrderByIdAsc("0", "0");
      account = defaults.isEmpty() ? null : defaults.get(0);
      if (account == null) {
        throw new ServiceException("无可用发件账号（请先配置并启用邮箱账号）");
      }
    }

    // 日限流：发送前查当日 SUCCESS 计数 vs DAILY_LIMIT
    if (account.getDailyLimit() != null && account.getDailyLimit() > 0) {
      int today =
          (int)
              mailLogRepository.countByAccountIdAndSendStatusAndSendTimeGreaterThanEqual(
                  account.getId(),
                  "SUCCESS",
                  java.sql.Timestamp.valueOf(java.time.LocalDate.now().atStartOfDay()));
      if (today >= account.getDailyLimit()) {
        throw new ServiceException("发件账号已达当日发送上限（" + account.getDailyLimit() + "封）");
      }
    }

    MsgMailLog log = new MsgMailLog();
    log.setTemplateId(template.getId());
    log.setTemplateCode(template.getTemplateCode());
    log.setAccountId(account.getId());
    log.setToAddrs(String.join(",", dto.getTo()));
    log.setCcAddrs(dto.getCc() == null ? null : String.join(",", dto.getCc()));
    log.setSubject(TemplateRenderer.render(template.getSubject(), params, null));
    log.setContent(TemplateRenderer.render(template.getTemplateContent(), params, null));
    log.setIsHtml(template.getIsHtml());
    log.setAttachFileIds(
        dto.getAttachments() == null || dto.getAttachments().isEmpty()
            ? null
            : joinIds(dto.getAttachments()));
    log.setSendStatus(MsgMailLog.STATUS_PENDING);
    log.setRetryCount(0);
    log.setSendTime(new Date());
    log.setBizType(StringUtils.isEmpty(dto.getBizType()) ? BIZ_TEST : dto.getBizType());
    log.setBizId(dto.getBizId());
    log.setCreateBy(dto.getOperator());

    if (!BIZ_TEST.equals(log.getBizType())) {
      String idemKey =
          StringUtils.isNotEmpty(dto.getIdempotentKey())
              ? dto.getIdempotentKey()
              : SmsSendWorker.md5(
                  log.getToAddrs() + "|" + template.getTemplateCode() + "|" + params);
      log.setIdempotentKey(idemKey);
      MsgMailLog existing =
          mailLogRepository.findByBizTypeAndBizIdAndTemplateCodeAndIdempotentKey(
              log.getBizType(), log.getBizId(), template.getTemplateCode(), idemKey);
      if (existing != null) {
        return existing.getId();
      }
    }

    log.setId(idGenerator.nextId());
    try {
      mailLogRepository.save(log);
    } catch (DataIntegrityViolationException ex) {
      MsgMailLog existing =
          mailLogRepository.findByBizTypeAndBizIdAndTemplateCodeAndIdempotentKey(
              log.getBizType(), log.getBizId(), template.getTemplateCode(), log.getIdempotentKey());
      if (existing != null) {
        return existing.getId();
      }
      throw ex;
    }
    mailSendWorker.send(log.getId());
    return log.getId();
  }

  @Override
  public Long retryMail(Long logId) {
    MsgMailLog log = mailLogRepository.findById(logId).orElse(null);
    if (log == null) {
      throw new ServiceException("邮件记录不存在");
    }
    if (!MsgMailLog.STATUS_FAILED.equals(log.getSendStatus())) {
      throw new ServiceException("仅失败记录可重发（当前状态：" + log.getSendStatus() + "）");
    }
    if (log.getRetryCount() != null && log.getRetryCount() >= 5) {
      throw new ServiceException("重发次数已达上限（5次）");
    }
    log.setRetryCount((log.getRetryCount() == null ? 0 : log.getRetryCount()) + 1);
    log.setSendStatus(MsgMailLog.STATUS_SENDING);
    log.setFailReason(null);
    mailLogRepository.save(log);
    mailSendWorker.send(logId);
    return logId;
  }

  private String joinIds(List<Long> ids) {
    StringBuilder sb = new StringBuilder();
    for (Long id : ids) {
      if (sb.length() > 0) {
        sb.append(',');
      }
      sb.append(id);
    }
    return sb.toString();
  }

  // ==================== 站内信 ====================

  @Override
  public List<Long> sendInner(InnerSendDTO dto) {
    MsgInnerTemplate template =
        innerTemplateRepository.findByTemplateCodeAndDelFlag(dto.getTemplateCode(), "0");
    if (template == null) {
      throw new ServiceException("站内信模板不存在（" + dto.getTemplateCode() + "）");
    }
    if (!"0".equals(template.getStatus())) {
      throw new ServiceException("站内信模板已停用（" + dto.getTemplateCode() + "）");
    }
    Map<String, String> params = dto.getParams() == null ? Map.of() : dto.getParams();
    String title =
        TemplateRenderer.render(template.getTitle(), params, template.getMissingParamMode());
    String content =
        TemplateRenderer.render(
            template.getTemplateContent(), params, template.getMissingParamMode());
    String jumpUrl =
        StringUtils.isNotEmpty(dto.getJumpUrl()) ? dto.getJumpUrl() : template.getJumpUrl();

    // 接收范围展开（用户/角色/全员 三选一）→ 按人一行
    Map<Long, String> receivers = resolveReceivers(dto);
    if (receivers.isEmpty()) {
      throw new ServiceException("接收范围为空（未展开到任何用户）");
    }

    String bizType = StringUtils.isEmpty(dto.getBizType()) ? BIZ_TEST : dto.getBizType();
    String idemKeyBase = dto.getIdempotentKey();
    if (StringUtils.isEmpty(idemKeyBase)) {
      idemKeyBase = SmsSendWorker.md5(template.getTemplateCode() + "|" + params + "|" + jumpUrl);
    }
    String senderName = StringUtils.isNotEmpty(dto.getOperator()) ? dto.getOperator() : "system";

    List<MsgInnerMessage> batch = new ArrayList<>();
    List<Long> result = new ArrayList<>();
    for (Map.Entry<Long, String> entry : receivers.entrySet()) {
      Long receiverId = entry.getKey();
      // 幂等（按接收人粒度）：已投递过直接复用
      MsgInnerMessage existing =
          innerMessageRepository.findByBizTypeAndBizIdAndTemplateCodeAndReceiverIdAndIdempotentKey(
              bizType, dto.getBizId(), template.getTemplateCode(), receiverId, idemKeyBase);
      if (existing != null) {
        result.add(existing.getId());
        continue;
      }
      MsgInnerMessage message = new MsgInnerMessage();
      message.setId(idGenerator.nextId());
      message.setTemplateId(template.getId());
      message.setTemplateCode(template.getTemplateCode());
      message.setTitle(title);
      message.setContent(content);
      message.setJumpUrl(jumpUrl);
      message.setReceiverId(receiverId);
      message.setReceiverName(entry.getValue());
      message.setReadFlag(MsgInnerMessage.READ_NO);
      message.setSenderType("0");
      message.setSenderName(senderName);
      message.setBizType(bizType);
      message.setBizId(dto.getBizId());
      message.setIdempotentKey(idemKeyBase);
      message.setCreateBy(senderName);
      message.setCreateTime(new Date());
      batch.add(message);
      result.add(message.getId());
      if (batch.size() >= INNER_BATCH_SIZE) {
        innerMessageRepository.saveAll(batch);
        batch.clear();
      }
    }
    if (!batch.isEmpty()) {
      innerMessageRepository.saveAll(batch);
    }
    return result;
  }

  /** 接收范围解析：USER_ID → 姓名（冗余，列表免关联） */
  private Map<Long, String> resolveReceivers(InnerSendDTO dto) {
    List<UserDirectoryItem> rows;
    if (dto.getAllUser() != null && dto.getAllUser()) {
      rows = userDirectoryClient.listAll();
    } else if (dto.getRoleKeys() != null && !dto.getRoleKeys().isEmpty()) {
      rows = userDirectoryClient.listByRoleKeys(dto.getRoleKeys());
    } else if (dto.getReceiverIds() != null && !dto.getReceiverIds().isEmpty()) {
      rows = userDirectoryClient.listByIds(dto.getReceiverIds());
    } else {
      throw new ServiceException("接收范围三选一必填（receiverIds/roleKeys/allUser）");
    }
    Map<Long, String> receivers = new LinkedHashMap<>();
    for (UserDirectoryItem user : rows) {
      if (user.getUserId() != null) {
        String name =
            user.getNickName() != null ? user.getNickName() : user.getUserName();
        receivers.put(user.getUserId(), name);
      }
    }
    return receivers;
  }
}
