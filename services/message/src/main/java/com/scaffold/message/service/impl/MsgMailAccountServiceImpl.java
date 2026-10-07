package com.scaffold.message.service.impl;

import com.scaffold.common.core.exception.ServiceException;
import com.scaffold.common.core.jpa.JpaSpecs;
import com.scaffold.common.core.utils.PageUtils;
import com.scaffold.common.core.web.page.PageDomain;
import com.scaffold.common.core.web.page.TableDataInfo;
import com.scaffold.common.core.utils.StringUtils;
import com.scaffold.common.core.utils.crypto.Sm4FieldCrypto;
import com.scaffold.common.core.utils.uuid.SnowflakeIdGenerator;
import com.scaffold.common.security.utils.SecurityUtils;
import com.scaffold.message.domain.MsgMailAccount;
import com.scaffold.message.repository.MsgMailAccountRepository;
import com.scaffold.message.repository.MsgMailTemplateRepository;
import com.scaffold.message.service.IMessageSendService;
import com.scaffold.message.service.IMsgMailAccountService;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

/**
 * 邮箱账号服务实现（授权码 SM4 密文落库 + 永不回显明文）
 *
 * @author ct
 */
@Service
public class MsgMailAccountServiceImpl implements IMsgMailAccountService {
  private static final String MASK = "********";

  private final MsgMailAccountRepository accountRepository;

  private final MsgMailTemplateRepository mailTemplateRepository;

  private final IMessageSendService sendService;

  private final SnowflakeIdGenerator idGenerator;

  public MsgMailAccountServiceImpl(
      MsgMailAccountRepository accountRepository,
      MsgMailTemplateRepository mailTemplateRepository,
      IMessageSendService sendService,
      SnowflakeIdGenerator idGenerator) {
    this.accountRepository = accountRepository;
    this.mailTemplateRepository = mailTemplateRepository;
    this.sendService = sendService;
    this.idGenerator = idGenerator;
  }

  @Override
  public MsgMailAccount queryById(Long id) {
    return mask(accountRepository.findById(id).orElse(null));
  }

  @Override
  public List<MsgMailAccount> queryList(MsgMailAccount query) {
    List<MsgMailAccount> list =
        accountRepository.list(toSpec(query), Sort.by(Sort.Direction.ASC, "id"));
    list.forEach(this::mask);
    return list;
  }

  /** 动态条件（对齐原 MsgMailAccountMapper.xml selectList：未删除 + 可选筛选） */
  private Specification<Object> toSpec(MsgMailAccount query) {
    if (query == null) {
      return JpaSpecs.eqIfNotBlank("delFlag", "0");
    }
    return JpaSpecs.eqIfNotBlank("delFlag", "0")
        .and(JpaSpecs.likeIf("accountName", query.getAccountName()))
        .and(JpaSpecs.likeIf("emailAddr", query.getEmailAddr()))
        .and(JpaSpecs.eqIfNotBlank("status", query.getStatus()));
  }

  @Override
  public int insert(MsgMailAccount account) {
    if (accountRepository.findByEmailAddrAndDelFlag(account.getEmailAddr(), "0") != null) {
      throw new ServiceException("发件邮箱已存在（" + account.getEmailAddr() + "）");
    }
    if (StringUtils.isEmpty(account.getAuthCode())) {
      throw new ServiceException("授权码不能为空");
    }
    account.setAuthCode(Sm4FieldCrypto.encrypt(account.getAuthCode()));
    account.setDelFlag("0");
    account.setCreateBy(SecurityUtils.getUsername());
    // 原流程未分配主键（存量缺陷），迁移统一走应用侧雪花
    account.setId(idGenerator.nextId());
    accountRepository.save(account);
    return 1;
  }

  @Override
  public int update(MsgMailAccount account) {
    if (account.getId() == null) {
      throw new ServiceException("账号 ID 不能为空");
    }
    // 编辑时授权码留空即不改
    if (StringUtils.isEmpty(account.getAuthCode()) || MASK.equals(account.getAuthCode())) {
      account.setAuthCode(null);
    } else {
      account.setAuthCode(Sm4FieldCrypto.encrypt(account.getAuthCode()));
    }
    account.setUpdateBy(SecurityUtils.getUsername());
    MsgMailAccount managed =
        accountRepository
            .findById(account.getId())
            .orElseThrow(() -> new ServiceException("更新失败（账号不存在，请刷新重试）"));
    applyUpdate(managed, account);
    accountRepository.save(managed);
    return 1;
  }

  /** 对齐原 update 的条件覆盖：空串不改、SMTP_PORT/SSL/NICK_NAME 允许置空 */
  private void applyUpdate(MsgMailAccount managed, MsgMailAccount src) {
    if (StringUtils.isNotEmpty(src.getAccountName())) {
      managed.setAccountName(src.getAccountName());
    }
    if (StringUtils.isNotEmpty(src.getEmailAddr())) {
      managed.setEmailAddr(src.getEmailAddr());
    }
    if (StringUtils.isNotEmpty(src.getSmtpHost())) {
      managed.setSmtpHost(src.getSmtpHost());
    }
    managed.setSmtpPort(src.getSmtpPort());
    managed.setSmtpSsl(src.getSmtpSsl());
    if (StringUtils.isNotEmpty(src.getAuthUser())) {
      managed.setAuthUser(src.getAuthUser());
    }
    if (src.getAuthCode() != null) {
      managed.setAuthCode(src.getAuthCode());
    }
    managed.setNickName(src.getNickName());
    if (src.getDailyLimit() != null) {
      managed.setDailyLimit(src.getDailyLimit());
    }
    if (StringUtils.isNotEmpty(src.getStatus())) {
      managed.setStatus(src.getStatus());
    }
    if (src.getRemark() != null) {
      managed.setRemark(src.getRemark());
    }
  }

  @Override
  public int deleteByIds(Long[] ids) {
    for (Long id : ids) {
      if (mailTemplateRepository.countByAccountIdAndDelFlag(id, "0") > 0) {
        throw new ServiceException("账号被邮件模板引用，不可删除（id=" + id + "）");
      }
    }
    // 软删除（对齐原 update DEL_FLAG='2'）
    for (Long id : ids) {
      accountRepository
          .findById(id)
          .ifPresent(
              a -> {
                a.setDelFlag("2");
                accountRepository.save(a);
              });
    }
    return ids.length;
  }

  @Override
  public int changeStatus(Long id, String status) {
    MsgMailAccount managed =
        accountRepository
            .findById(id)
            .orElseThrow(() -> new ServiceException("账号不存在"));
    managed.setStatus(status);
    managed.setUpdateBy(SecurityUtils.getUsername());
    accountRepository.save(managed);
    return 1;
  }

  @Override
  public Long testAccount(Long id, String toAddr) {
    MsgMailAccount account = accountRepository.findById(id).orElse(null);
    if (account == null) {
      throw new ServiceException("账号不存在（id=" + id + "）");
    }
    // 原文直发（BIZ_TYPE=TEST，不走模板表）
    return sendService.sendRawMail(
        id,
        toAddr,
        "【SMTP 连通测试】scaffold-message 发件账号 " + account.getAccountName(),
        "<p>这是 scaffold-message 发件账号连通测试邮件（BIZ_TYPE=TEST）。</p>",
        SecurityUtils.getUsername());
  }

  private MsgMailAccount mask(MsgMailAccount account) {
    if (account != null && StringUtils.isNotEmpty(account.getAuthCode())) {
      account.setAuthCode(MASK);
    }
    return account;
  }

  /**
   * 分页查询（JPA PageRequest 分页，分页参数取自请求上下文）
   */
  @Override
  public TableDataInfo queryPage(MsgMailAccount query, PageDomain page) {
    return TableDataInfo.from(accountRepository.page(toSpec(query), PageUtils.toPageRequest(page)));
  }
}
