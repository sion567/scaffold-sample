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
import com.scaffold.message.domain.MsgSmsChannel;
import com.scaffold.message.repository.MsgSmsChannelRepository;
import com.scaffold.message.repository.MsgSmsTemplateRepository;
import com.scaffold.message.service.IMessageSendService;
import com.scaffold.message.service.IMsgSmsChannelService;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

/**
 * 短信渠道服务实现（SecretKey SM4 密文落库 + 永不回显明文）
 *
 * @author ct
 */
@Service
public class MsgSmsChannelServiceImpl implements IMsgSmsChannelService {
  private static final String MASK = "********";

  private final MsgSmsChannelRepository channelRepository;

  private final MsgSmsTemplateRepository smsTemplateRepository;

  private final IMessageSendService sendService;

  private final SnowflakeIdGenerator idGenerator;

  public MsgSmsChannelServiceImpl(
      MsgSmsChannelRepository channelRepository,
      MsgSmsTemplateRepository smsTemplateRepository,
      IMessageSendService sendService,
      SnowflakeIdGenerator idGenerator) {
    this.channelRepository = channelRepository;
    this.smsTemplateRepository = smsTemplateRepository;
    this.sendService = sendService;
    this.idGenerator = idGenerator;
  }

  @Override
  public MsgSmsChannel queryById(Long id) {
    MsgSmsChannel channel = channelRepository.findById(id).orElse(null);
    mask(channel);
    return channel;
  }

  @Override
  public List<MsgSmsChannel> queryList(MsgSmsChannel query) {
    List<MsgSmsChannel> list =
        channelRepository.list(toSpec(query), Sort.by(Sort.Direction.ASC, "priority", "id"));
    list.forEach(this::mask);
    return list;
  }

  /** 动态条件（对齐原 MsgSmsChannelMapper.xml selectList：未删除 + 可选筛选） */
  private Specification<Object> toSpec(MsgSmsChannel query) {
    if (query == null) {
      return JpaSpecs.eqIfNotBlank("delFlag", "0");
    }
    return JpaSpecs.eqIfNotBlank("delFlag", "0")
        .and(JpaSpecs.likeIf("channelName", query.getChannelName()))
        .and(JpaSpecs.eqIfNotBlank("channelType", query.getChannelType()))
        .and(JpaSpecs.eqIfNotBlank("status", query.getStatus()));
  }

  @Override
  public int insert(MsgSmsChannel channel) {
    if (channelRepository.findByChannelNameAndDelFlag(channel.getChannelName(), "0") != null) {
      throw new ServiceException("渠道名称已存在（" + channel.getChannelName() + "）");
    }
    if (StringUtils.isNotEmpty(channel.getSecretKey())) {
      channel.setSecretKey(Sm4FieldCrypto.encrypt(channel.getSecretKey()));
    }
    channel.setDelFlag("0");
    channel.setCreateBy(SecurityUtils.getUsername());
    // 原流程未分配主键（存量缺陷），迁移统一走应用侧雪花
    channel.setId(idGenerator.nextId());
    channelRepository.save(channel);
    return 1;
  }

  @Override
  public int update(MsgSmsChannel channel) {
    if (channel.getId() == null) {
      throw new ServiceException("渠道 ID 不能为空");
    }
    // 编辑时密钥留空即不改
    if (StringUtils.isEmpty(channel.getSecretKey()) || MASK.equals(channel.getSecretKey())) {
      channel.setSecretKey(null);
    } else {
      channel.setSecretKey(Sm4FieldCrypto.encrypt(channel.getSecretKey()));
    }
    channel.setUpdateBy(SecurityUtils.getUsername());
    MsgSmsChannel managed =
        channelRepository
            .findById(channel.getId())
            .orElseThrow(() -> new ServiceException("更新失败（渠道不存在，请刷新重试）"));
    applyUpdate(managed, channel);
    channelRepository.save(managed);
    return 1;
  }

  /** 对齐原 update 的条件覆盖：空串不改、ACCESS_KEY/SIGN_NAME/ENDPOINT/REGION 允许置空 */
  private void applyUpdate(MsgSmsChannel managed, MsgSmsChannel src) {
    if (StringUtils.isNotEmpty(src.getChannelName())) {
      managed.setChannelName(src.getChannelName());
    }
    if (StringUtils.isNotEmpty(src.getChannelType())) {
      managed.setChannelType(src.getChannelType());
    }
    managed.setAccessKey(src.getAccessKey());
    if (src.getSecretKey() != null) {
      managed.setSecretKey(src.getSecretKey());
    }
    managed.setSignName(src.getSignName());
    managed.setEndpoint(src.getEndpoint());
    managed.setRegion(src.getRegion());
    if (src.getPriority() != null) {
      managed.setPriority(src.getPriority());
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
      if (smsTemplateRepository.countByChannelIdAndDelFlag(id, "0") > 0) {
        throw new ServiceException("渠道被短信模板引用，不可删除（id=" + id + "）");
      }
    }
    // 软删除（对齐原 update DEL_FLAG='2'）
    for (Long id : ids) {
      channelRepository
          .findById(id)
          .ifPresent(
              c -> {
                c.setDelFlag("2");
                channelRepository.save(c);
              });
    }
    return ids.length;
  }

  @Override
  public int changeStatus(Long id, String status) {
    MsgSmsChannel managed =
        channelRepository
            .findById(id)
            .orElseThrow(() -> new ServiceException("渠道不存在"));
    managed.setStatus(status);
    managed.setUpdateBy(SecurityUtils.getUsername());
    channelRepository.save(managed);
    return 1;
  }

  @Override
  public Long testChannel(Long id, String mobile) {
    MsgSmsChannel channel = channelRepository.findById(id).orElse(null);
    if (channel == null) {
      throw new ServiceException("渠道不存在（id=" + id + "）");
    }
    return sendService.sendRawSms(
        id,
        mobile,
        "【渠道连通测试】scaffold-message 渠道 " + channel.getChannelName() + " 测试短信",
        SecurityUtils.getUsername());
  }

  private void mask(MsgSmsChannel channel) {
    if (channel != null && StringUtils.isNotEmpty(channel.getSecretKey())) {
      channel.setSecretKey(MASK);
    }
  }

  /**
   * 分页查询（JPA PageRequest 分页，分页参数取自请求上下文）
   */
  @Override
  public TableDataInfo queryPage(MsgSmsChannel query, PageDomain page) {
    return TableDataInfo.from(channelRepository.page(toSpec(query), PageUtils.toPageRequest(page)));
  }
}
