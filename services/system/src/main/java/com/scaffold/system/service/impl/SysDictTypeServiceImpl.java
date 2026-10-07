package com.scaffold.system.service.impl;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jakarta.annotation.PostConstruct;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.scaffold.common.core.constant.UserConstants;
import com.scaffold.common.core.exception.ServiceException;
import com.scaffold.common.core.jpa.JpaSpecs;
import com.scaffold.common.core.utils.StringUtils;
import com.scaffold.common.security.utils.DictUtils;
import com.scaffold.system.api.domain.SysDictData;
import com.scaffold.system.api.domain.SysDictType;
import com.scaffold.system.repository.SysDictDataRepository;
import com.scaffold.system.repository.SysDictTypeRepository;
import com.scaffold.system.service.ISysDictTypeService;

/**
 * 字典 业务层处理（JPA：Repository + Specification）
 *
 * @author ct
 */
@Service
public class SysDictTypeServiceImpl implements ISysDictTypeService
{
    private final SysDictTypeRepository dictTypeRepository;
    private final SysDictDataRepository dictDataRepository;

    public SysDictTypeServiceImpl(SysDictTypeRepository dictTypeRepository, SysDictDataRepository dictDataRepository)
    {
        this.dictTypeRepository = dictTypeRepository;
        this.dictDataRepository = dictDataRepository;
    }

    /**
     * 项目启动时，初始化字典到缓存
     */
    @PostConstruct
    public void init()
    {
        loadingDictCache();
    }

    /**
     * 根据条件分页查询字典类型
     *
     * @param dictType 字典类型信息
     * @return 字典类型集合信息
     */
    @Override
    public List<SysDictType> selectDictTypeList(SysDictType dictType)
    {
        return dictTypeRepository.list(toSpec(dictType));
    }

    @Override
    public com.scaffold.common.core.web.page.TableDataInfo selectDictTypePage(SysDictType dictType, com.scaffold.common.core.web.page.PageDomain page)
    {
        return com.scaffold.common.core.web.page.TableDataInfo.from(
                dictTypeRepository.page(toSpec(dictType), com.scaffold.common.core.utils.PageUtils.toPageRequest(page)));
    }

    /** 动态条件（对齐原 SysDictTypeMapper.xml selectDictTypeList） */
    private Specification<?> toSpec(SysDictType dictType)
    {
        if (dictType == null)
        {
            dictType = new SysDictType();
        }
        return JpaSpecs.likeIf("dictName", dictType.getDictName())
                .and(JpaSpecs.eqIfNotBlank("status", dictType.getStatus()))
                .and(JpaSpecs.likeIf("dictType", dictType.getDictType()))
                .and(JpaSpecs.dateRangeIf("createTime", dictType.getParams()));
    }

    /**
     * 根据所有字典类型
     *
     * @return 字典类型集合信息
     */
    @Override
    public List<SysDictType> selectDictTypeAll()
    {
        return dictTypeRepository.findAll();
    }

    /**
     * 根据字典类型查询字典数据
     *
     * @param dictType 字典类型
     * @return 字典数据集合信息
     */
    @Override
    public List<SysDictData> selectDictDataByType(String dictType)
    {
        List<SysDictData> dictDatas = DictUtils.getDictCache(dictType);
        if (StringUtils.isNotEmpty(dictDatas))
        {
            return dictDatas;
        }
        dictDatas = dictDataRepository.findByDictTypeAndStatusOrderByDictSortAsc(dictType, "0");
        if (StringUtils.isNotEmpty(dictDatas))
        {
            DictUtils.setDictCache(dictType, dictDatas);
            return dictDatas;
        }
        return null;
    }

    /**
     * 根据字典类型ID查询信息
     *
     * @param dictId 字典类型ID
     * @return 字典类型
     */
    @Override
    public SysDictType selectDictTypeById(Long dictId)
    {
        return dictTypeRepository.findById(dictId).orElse(null);
    }

    /**
     * 根据字典类型查询信息
     *
     * @param dictType 字典类型
     * @return 字典类型
     */
    @Override
    public SysDictType selectDictTypeByType(String dictType)
    {
        return dictTypeRepository.findByDictType(dictType).orElse(null);
    }

    /**
     * 批量删除字典类型信息
     *
     * @param dictIds 需要删除的字典ID
     */
    @Override
    public void deleteDictTypeByIds(Long[] dictIds)
    {
        for (Long dictId : dictIds)
        {
            SysDictType dictType = selectDictTypeById(dictId);
            if (dictDataRepository.countByDictType(dictType.getDictType()) > 0)
            {
                throw new ServiceException(String.format("%1$s已分配,不能删除", dictType.getDictName()));
            }
            dictTypeRepository.deleteById(dictId);
            DictUtils.removeDictCache(dictType.getDictType());
        }
    }

    /**
     * 加载字典缓存数据
     */
    @Override
    public void loadingDictCache()
    {
        Map<String, List<SysDictData>> dictDataMap = dictDataRepository.findByStatusOrderByDictSortAsc("0")
                .stream().collect(Collectors.groupingBy(SysDictData::getDictType));
        for (Map.Entry<String, List<SysDictData>> entry : dictDataMap.entrySet())
        {
            DictUtils.setDictCache(entry.getKey(), entry.getValue().stream().sorted(Comparator.comparing(SysDictData::getDictSort)).collect(Collectors.toList()));
        }
    }

    /**
     * 清空字典缓存数据
     */
    @Override
    public void clearDictCache()
    {
        DictUtils.clearDictCache();
    }

    /**
     * 重置字典缓存数据
     */
    @Override
    public void resetDictCache()
    {
        clearDictCache();
        loadingDictCache();
    }

    /**
     * 新增保存字典类型信息
     *
     * @param dict 字典类型信息
     * @return 结果
     */
    @Override
    public int insertDictType(SysDictType dict)
    {
        dictTypeRepository.save(dict);
        DictUtils.setDictCache(dict.getDictType(), null);
        return 1;
    }

    /**
     * 修改保存字典类型信息
     *
     * @param dict 字典类型信息
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateDictType(SysDictType dict)
    {
        SysDictType oldDict = dictTypeRepository.findById(dict.getDictId()).orElseThrow(
                () -> new ServiceException("字典类型不存在或已被删除"));
        dictDataRepository.updateDictDataType(oldDict.getDictType(), dict.getDictType());
        dictTypeRepository.save(dict);
        List<SysDictData> dictDatas = dictDataRepository.findByDictTypeAndStatusOrderByDictSortAsc(dict.getDictType(), "0");
        DictUtils.setDictCache(dict.getDictType(), dictDatas);
        return 1;
    }

    /**
     * 校验字典类型称是否唯一
     *
     * @param dict 字典类型
     * @return 结果
     */
    @Override
    public boolean checkDictTypeUnique(SysDictType dict)
    {
        Long dictId = StringUtils.isNull(dict.getDictId()) ? -1L : dict.getDictId();
        SysDictType info = dictTypeRepository.findByDictType(dict.getDictType()).orElse(null);
        if (StringUtils.isNotNull(info) && info.getDictId().longValue() != dictId.longValue())
        {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }
}
