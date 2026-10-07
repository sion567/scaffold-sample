package com.scaffold.message.service.impl;

import com.scaffold.common.core.exception.ServiceException;
import com.scaffold.common.core.jpa.JpaSpecs;
import com.scaffold.common.core.utils.PageUtils;
import com.scaffold.common.core.web.page.PageDomain;
import com.scaffold.common.core.web.page.TableDataInfo;
import com.scaffold.common.core.utils.StringUtils;
import com.scaffold.common.security.utils.SecurityUtils;
import com.scaffold.common.core.utils.uuid.SnowflakeIdGenerator;
import com.scaffold.message.domain.MsgSmsTemplate;
import com.scaffold.message.repository.MsgSmsTemplateRepository;
import com.scaffold.message.service.IMsgSmsTemplateService;
import com.scaffold.message.service.TemplateRenderer;
import java.util.Arrays;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

/**
 * 短信模板服务实现（录入时解析 PARAM_NAMES；厂商模板模式 VENDOR_CODE 必填）
 *
 * @author ct
 */
@Service
public class MsgSmsTemplateServiceImpl implements IMsgSmsTemplateService {
  private final MsgSmsTemplateRepository templateRepository;
  private final SnowflakeIdGenerator idGenerator;

  public MsgSmsTemplateServiceImpl(
      MsgSmsTemplateRepository templateRepository, SnowflakeIdGenerator idGenerator) {
    this.templateRepository = templateRepository;
    this.idGenerator = idGenerator;
  }

  @Override
  public MsgSmsTemplate queryById(Long id) {
    return templateRepository.findById(id).orElse(null);
  }

  @Override
  public List<MsgSmsTemplate> queryList(MsgSmsTemplate query) {
    return templateRepository.list(toSpec(query), Sort.by(Sort.Direction.DESC, "id"));
  }


  /**
   * 分页查询（JPA PageRequest 分页，分页参数取自请求上下文）
   */
  @Override
  public TableDataInfo queryPage(MsgSmsTemplate query, PageDomain page) {
    return TableDataInfo.from(templateRepository.page(toSpec(query), PageUtils.toPageRequest(page)));
  }
  /** 动态条件（对齐原 MsgSmsTemplateMapper.xml selectList：未删除 + 可选筛选） */
  private Specification<Object> toSpec(MsgSmsTemplate query) {
    if (query == null) {
      return JpaSpecs.eqIfNotBlank("delFlag", "0");
    }
    return JpaSpecs.eqIfNotBlank("delFlag", "0")
        .and(JpaSpecs.likeIf("templateCode", query.getTemplateCode()))
        .and(JpaSpecs.likeIf("templateName", query.getTemplateName()))
        .and(JpaSpecs.eqIf("channelId", query.getChannelId()))
        .and(JpaSpecs.eqIfNotBlank("category", query.getCategory()))
        .and(JpaSpecs.eqIfNotBlank("status", query.getStatus()));
  }

  @Override
  public int insert(MsgSmsTemplate template) {
    checkUnique(template);
    fillParams(template);
    template.setDelFlag("0");
    template.setCreateBy(SecurityUtils.getUsername());
    template.setId(idGenerator.nextId());
    templateRepository.save(template);
    return 1;
  }

  @Override
  public int update(MsgSmsTemplate template) {
    if (template.getId() == null) {
      throw new ServiceException("模板 ID 不能为空");
    }
    checkUnique(template);
    fillParams(template);
    // 加载-合并-保存：null/空串字段不改（对齐原动态 update），乐观锁冲突翻译为业务异常
    MsgSmsTemplate managed =
        templateRepository
            .findById(template.getId())
            .orElseThrow(() -> new ServiceException("更新失败（模板不存在，请刷新重试）"));
    applyUpdate(managed, template);
    managed.setUpdateBy(SecurityUtils.getUsername());
    templateRepository.save(managed);
    return 1;
  }

  /** 对齐原 update 的条件覆盖：空串不改、可空字段（渠道/厂商编码/签名/备注）允许置空 */
  private void applyUpdate(MsgSmsTemplate managed, MsgSmsTemplate src) {
    if (StringUtils.isNotEmpty(src.getTemplateCode())) {
      managed.setTemplateCode(src.getTemplateCode());
    }
    if (StringUtils.isNotEmpty(src.getTemplateName())) {
      managed.setTemplateName(src.getTemplateName());
    }
    managed.setChannelId(src.getChannelId());
    if (StringUtils.isNotEmpty(src.getTemplateMode())) {
      managed.setTemplateMode(src.getTemplateMode());
    }
    managed.setVendorCode(src.getVendorCode());
    managed.setSignName(src.getSignName());
    if (StringUtils.isNotEmpty(src.getTemplateContent())) {
      managed.setTemplateContent(src.getTemplateContent());
    }
    if (src.getParamNames() != null) {
      managed.setParamNames(src.getParamNames());
    }
    if (src.getCategory() != null) {
      managed.setCategory(src.getCategory());
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
    // 软删除（对齐原 update DEL_FLAG='2'）
    for (Long id : ids) {
      templateRepository
          .findById(id)
          .ifPresent(
              t -> {
                t.setDelFlag("2");
                templateRepository.save(t);
              });
    }
    return ids.length;
  }

  @Override
  public int changeStatus(Long id, String status) {
    MsgSmsTemplate managed =
        templateRepository
            .findById(id)
            .orElseThrow(() -> new ServiceException("模板不存在"));
    managed.setStatus(status);
    managed.setUpdateBy(SecurityUtils.getUsername());
    templateRepository.save(managed);
    return 1;
  }

  private void checkUnique(MsgSmsTemplate template) {
    MsgSmsTemplate existing = templateRepository.findByTemplateCodeAndDelFlag(template.getTemplateCode(), "0");
    if (existing != null && !existing.getId().equals(template.getId())) {
      throw new ServiceException("模板编码已存在（" + template.getTemplateCode() + "）");
    }
  }

  private void fillParams(MsgSmsTemplate template) {
    if (MsgSmsTemplate.MODE_VENDOR_CODE.equals(template.getTemplateMode())
        && StringUtils.isEmpty(template.getVendorCode())) {
      throw new ServiceException("厂商模板模式下 VENDOR_CODE 必填");
    }
    // MODE=1 时 TEMPLATE_CONTENT 存参数说明，不参与占位符解析
    if (!MsgSmsTemplate.MODE_VENDOR_CODE.equals(template.getTemplateMode())) {
      template.setParamNames(
          TemplateRenderer.parseParamNames(template.getTemplateContent(), template.getSignName()));
    } else {
      template.setParamNames(template.getTemplateContent());
    }
  }
}
