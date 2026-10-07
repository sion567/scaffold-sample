package com.scaffold.system.service.impl;

import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.scaffold.common.core.jpa.JpaSpecs;
import com.scaffold.common.security.utils.DictUtils;
import com.scaffold.system.api.domain.SysDictData;
import com.scaffold.system.repository.SysDictDataRepository;
import com.scaffold.system.service.ISysDictDataService;

/**
 * 字典 业务层处理（JPA：Repository + Specification）
 *
 * @author ct
 */
@Service
public class SysDictDataServiceImpl implements ISysDictDataService
{
    private final SysDictDataRepository dictDataRepository;

    public SysDictDataServiceImpl(SysDictDataRepository dictDataRepository)
    {
        this.dictDataRepository = dictDataRepository;
    }

    /**
     * 根据条件分页查询字典数据（dictType 等值、dictLabel 模糊、status 等值，dict_sort 升序）
     *
     * @param dictData 字典数据信息
     * @return 字典数据集合信息
     */
    @Override
    public List<SysDictData> selectDictDataList(SysDictData dictData)
    {
        if (dictData == null)
        {
            dictData = new SysDictData();
        }
        Specification<?> spec = JpaSpecs.eqIfNotBlank("dictType", dictData.getDictType())
                .and(JpaSpecs.likeIf("dictLabel", dictData.getDictLabel()))
                .and(JpaSpecs.eqIfNotBlank("status", dictData.getStatus()));
        return dictDataRepository.list(spec, org.springframework.data.domain.Sort.by("dictSort"));
    }

    @Override
    public com.scaffold.common.core.web.page.TableDataInfo selectDictDataPage(SysDictData dictData, com.scaffold.common.core.web.page.PageDomain page)
    {
        if (dictData == null)
        {
            dictData = new SysDictData();
        }
        Specification<?> spec = JpaSpecs.eqIfNotBlank("dictType", dictData.getDictType())
                .and(JpaSpecs.likeIf("dictLabel", dictData.getDictLabel()))
                .and(JpaSpecs.eqIfNotBlank("status", dictData.getStatus()));
        return com.scaffold.common.core.web.page.TableDataInfo.from(dictDataRepository.page(spec,
                com.scaffold.common.core.utils.PageUtils.toPageRequest(page)));
    }

    /**
     * 根据字典类型和字典键值查询字典数据信息
     *
     * @param dictType 字典类型
     * @param dictValue 字典键值
     * @return 字典标签
     */
    @Override
    public String selectDictLabel(String dictType, String dictValue)
    {
        return dictDataRepository.findByDictTypeAndDictValue(dictType, dictValue)
                .map(SysDictData::getDictLabel).orElse(null);
    }

    /**
     * 根据字典数据ID查询信息
     *
     * @param dictCode 字典数据ID
     * @return 字典数据
     */
    @Override
    public SysDictData selectDictDataById(Long dictCode)
    {
        return dictDataRepository.findById(dictCode).orElse(null);
    }

    /**
     * 批量删除字典数据信息
     *
     * @param dictCodes 需要删除的字典数据ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDictDataByIds(Long[] dictCodes)
    {
        for (Long dictCode : dictCodes)
        {
            SysDictData data = selectDictDataById(dictCode);
            dictDataRepository.deleteById(dictCode);
            List<SysDictData> dictDatas = dictDataRepository.findByDictTypeAndStatusOrderByDictSortAsc(data.getDictType(), "0");
            DictUtils.setDictCache(data.getDictType(), dictDatas);
        }
    }

    /**
     * 新增保存字典数据信息
     *
     * @param data 字典数据信息
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertDictData(SysDictData data)
    {
        dictDataRepository.save(data);
        List<SysDictData> dictDatas = dictDataRepository.findByDictTypeAndStatusOrderByDictSortAsc(data.getDictType(), "0");
        DictUtils.setDictCache(data.getDictType(), dictDatas);
        return 1;
    }

    /**
     * 修改保存字典数据信息
     *
     * @param data 字典数据信息
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateDictData(SysDictData data)
    {
        dictDataRepository.save(data);
        List<SysDictData> dictDatas = dictDataRepository.findByDictTypeAndStatusOrderByDictSortAsc(data.getDictType(), "0");
        DictUtils.setDictCache(data.getDictType(), dictDatas);
        return 1;
    }
}
