package com.scaffold.message.service.impl;

import com.scaffold.common.core.exception.ServiceException;
import com.scaffold.common.core.jpa.JpaSpecs;
import com.scaffold.common.core.utils.PageUtils;
import com.scaffold.common.core.web.page.PageDomain;
import com.scaffold.common.core.web.page.TableDataInfo;
import com.scaffold.common.core.utils.StringUtils;
import com.scaffold.common.core.utils.uuid.SnowflakeIdGenerator;
import com.scaffold.common.security.utils.SecurityUtils;
import com.scaffold.message.domain.MsgMailTemplate;
import com.scaffold.message.repository.MsgMailTemplateRepository;
import com.scaffold.message.service.IMsgMailTemplateService;
import com.scaffold.message.service.TemplateRenderer;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

/**
 * 邮件模板服务实现（录入时解析 PARAM_NAMES）
 *
 * @author ct
 */
@Service
public class MsgMailTemplateServiceImpl implements IMsgMailTemplateService {
  private final MsgMailTemplateRepository templateRepository;
  private final SnowflakeIdGenerator idGenerator;

  public MsgMailTemplateServiceImpl(
      MsgMailTemplateRepository templateRepository, SnowflakeIdGenerator idGenerator) {
    this.templateRepository = templateRepository;
    this.idGenerator = idGenerator;
  }

  @Override
  public MsgMailTemplate queryById(Long id) {
    return templateRepository.findById(id).orElse(null);
  }

  @Override
  public List<MsgMailTemplate> queryList(MsgMailTemplate query) {
    return templateRepository.list(toSpec(query), Sort.by(Sort.Direction.DESC, "id"));
  }


  /**
   * 分页查询（JPA PageRequest 分页，分页参数取自请求上下文）
   */
  @Override
  public TableDataInfo queryPage(MsgMailTemplate query, PageDomain page) {
    return TableDataInfo.from(templateRepository.page(toSpec(query), PageUtils.toPageRequest(page)));
  }
  /** 动态条件（对齐原 MsgMailTemplateMapper.xml selectList：未删除 + 可选筛选） */
  private Specification<Object> toSpec(MsgMailTemplate query) {
    if (query == null) {
      return JpaSpecs.eqIfNotBlank("delFlag", "0");
    }
    return JpaSpecs.eqIfNotBlank("delFlag", "0")
        .and(JpaSpecs.likeIf("templateCode", query.getTemplateCode()))
        .and(JpaSpecs.likeIf("templateName", query.getTemplateName()))
        .and(JpaSpecs.eqIf("accountId", query.getAccountId()))
        .and(JpaSpecs.eqIfNotBlank("category", query.getCategory()))
        .and(JpaSpecs.eqIfNotBlank("status", query.getStatus()));
  }

  @Override
  public int insert(MsgMailTemplate template) {
    checkUnique(template);
    fillParams(template);
    template.setDelFlag("0");
    template.setCreateBy(SecurityUtils.getUsername());
    template.setId(idGenerator.nextId());
    templateRepository.save(template);
    return 1;
  }

  @Override
  public int update(MsgMailTemplate template) {
    if (template.getId() == null) {
      throw new ServiceException("模板 ID 不能为空");
    }
    checkUnique(template);
    fillParams(template);
    template.setUpdateBy(SecurityUtils.getUsername());
    MsgMailTemplate managed =
        templateRepository
            .findById(template.getId())
            .orElseThrow(() -> new ServiceException("更新失败（模板不存在，请刷新重试）"));
    applyUpdate(managed, template);
    templateRepository.save(managed);
    return 1;
  }

  /** 对齐原 update 的条件覆盖：空串不改、ACCOUNT_ID 允许置空 */
  private void applyUpdate(MsgMailTemplate managed, MsgMailTemplate src) {
    if (StringUtils.isNotEmpty(src.getTemplateCode())) {
      managed.setTemplateCode(src.getTemplateCode());
    }
    if (StringUtils.isNotEmpty(src.getTemplateName())) {
      managed.setTemplateName(src.getTemplateName());
    }
    managed.setAccountId(src.getAccountId());
    if (StringUtils.isNotEmpty(src.getSubject())) {
      managed.setSubject(src.getSubject());
    }
    if (StringUtils.isNotEmpty(src.getTemplateContent())) {
      managed.setTemplateContent(src.getTemplateContent());
    }
    if (StringUtils.isNotEmpty(src.getIsHtml())) {
      managed.setIsHtml(src.getIsHtml());
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
    MsgMailTemplate managed =
        templateRepository
            .findById(id)
            .orElseThrow(() -> new ServiceException("模板不存在"));
    managed.setStatus(status);
    managed.setUpdateBy(SecurityUtils.getUsername());
    templateRepository.save(managed);
    return 1;
  }

  private void checkUnique(MsgMailTemplate template) {
    MsgMailTemplate existing =
        templateRepository.findByTemplateCodeAndDelFlag(template.getTemplateCode(), "0");
    if (existing != null && !existing.getId().equals(template.getId())) {
      throw new ServiceException("模板编码已存在（" + template.getTemplateCode() + "）");
    }
  }

  private void fillParams(MsgMailTemplate template) {
    template.setParamNames(
        TemplateRenderer.parseParamNames(template.getSubject(), template.getTemplateContent()));
  }
}
