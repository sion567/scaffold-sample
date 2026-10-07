package com.scaffold.message.service.impl;

import com.scaffold.common.core.exception.ServiceException;
import com.scaffold.common.core.jpa.JpaSpecs;
import com.scaffold.common.core.utils.PageUtils;
import com.scaffold.common.core.web.page.PageDomain;
import com.scaffold.common.core.web.page.TableDataInfo;
import com.scaffold.common.core.utils.StringUtils;
import com.scaffold.common.core.utils.uuid.SnowflakeIdGenerator;
import com.scaffold.common.security.utils.SecurityUtils;
import com.scaffold.message.domain.MsgInnerTemplate;
import com.scaffold.message.repository.MsgInnerTemplateRepository;
import com.scaffold.message.service.IMsgInnerTemplateService;
import com.scaffold.message.service.TemplateRenderer;
import java.util.Arrays;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

/**
 * 站内信模板服务实现（录入时解析 PARAM_NAMES，含标题与正文占位符）
 *
 * @author ct
 */
@Service
public class MsgInnerTemplateServiceImpl implements IMsgInnerTemplateService {
  private final MsgInnerTemplateRepository templateRepository;
  private final SnowflakeIdGenerator idGenerator;

  public MsgInnerTemplateServiceImpl(
      MsgInnerTemplateRepository templateRepository, SnowflakeIdGenerator idGenerator) {
    this.templateRepository = templateRepository;
    this.idGenerator = idGenerator;
  }

  @Override
  public MsgInnerTemplate queryById(Long id) {
    return templateRepository.findById(id).orElse(null);
  }

  @Override
  public List<MsgInnerTemplate> queryList(MsgInnerTemplate query) {
    return templateRepository.list(toSpec(query), Sort.by(Sort.Direction.DESC, "id"));
  }


  /**
   * 分页查询（JPA PageRequest 分页，分页参数取自请求上下文）
   */
  @Override
  public TableDataInfo queryPage(MsgInnerTemplate query, PageDomain page) {
    return TableDataInfo.from(templateRepository.page(toSpec(query), PageUtils.toPageRequest(page)));
  }
  /** 动态条件（对齐原 MsgInnerTemplateMapper.xml selectList：未删除 + 可选筛选） */
  private Specification<Object> toSpec(MsgInnerTemplate query) {
    if (query == null) {
      return JpaSpecs.eqIfNotBlank("delFlag", "0");
    }
    return JpaSpecs.eqIfNotBlank("delFlag", "0")
        .and(JpaSpecs.likeIf("templateCode", query.getTemplateCode()))
        .and(JpaSpecs.likeIf("templateName", query.getTemplateName()))
        .and(JpaSpecs.eqIfNotBlank("category", query.getCategory()))
        .and(JpaSpecs.eqIfNotBlank("status", query.getStatus()));
  }

  @Override
  public int insert(MsgInnerTemplate template) {
    checkUnique(template);
    fillParams(template);
    template.setDelFlag("0");
    template.setCreateBy(SecurityUtils.getUsername());
    template.setId(idGenerator.nextId());
    templateRepository.save(template);
    return 1;
  }

  @Override
  public int update(MsgInnerTemplate template) {
    if (template.getId() == null) {
      throw new ServiceException("模板 ID 不能为空");
    }
    checkUnique(template);
    fillParams(template);
    MsgInnerTemplate managed =
        templateRepository
            .findById(template.getId())
            .orElseThrow(() -> new ServiceException("更新失败（模板不存在，请刷新重试）"));
    applyUpdate(managed, template);
    managed.setUpdateBy(SecurityUtils.getUsername());
    templateRepository.save(managed);
    return 1;
  }

  /** 对齐原 update 的条件覆盖：空串不改、JUMP_URL 允许置空清掉 */
  private void applyUpdate(MsgInnerTemplate managed, MsgInnerTemplate src) {
    if (StringUtils.isNotEmpty(src.getTemplateCode())) {
      managed.setTemplateCode(src.getTemplateCode());
    }
    if (StringUtils.isNotEmpty(src.getTemplateName())) {
      managed.setTemplateName(src.getTemplateName());
    }
    if (StringUtils.isNotEmpty(src.getTitle())) {
      managed.setTitle(src.getTitle());
    }
    if (StringUtils.isNotEmpty(src.getTemplateContent())) {
      managed.setTemplateContent(src.getTemplateContent());
    }
    if (StringUtils.isNotEmpty(src.getMissingParamMode())) {
      managed.setMissingParamMode(src.getMissingParamMode());
    }
    managed.setJumpUrl(src.getJumpUrl());
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
    MsgInnerTemplate managed =
        templateRepository
            .findById(id)
            .orElseThrow(() -> new ServiceException("模板不存在"));
    managed.setStatus(status);
    managed.setUpdateBy(SecurityUtils.getUsername());
    templateRepository.save(managed);
    return 1;
  }

  private void checkUnique(MsgInnerTemplate template) {
    MsgInnerTemplate existing =
        templateRepository.findByTemplateCodeAndDelFlag(template.getTemplateCode(), "0");
    if (existing != null && !existing.getId().equals(template.getId())) {
      throw new ServiceException("模板编码已存在（" + template.getTemplateCode() + "）");
    }
  }

  private void fillParams(MsgInnerTemplate template) {
    template.setParamNames(
        TemplateRenderer.parseParamNames(template.getTitle(), template.getTemplateContent()));
  }
}
