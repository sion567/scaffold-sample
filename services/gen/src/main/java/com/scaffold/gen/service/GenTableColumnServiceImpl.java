package com.scaffold.gen.service;

import java.util.List;
import org.springframework.stereotype.Service;
import com.scaffold.common.core.text.Convert;
import com.scaffold.gen.domain.GenTableColumn;
import com.scaffold.gen.repository.GenTableColumnRepository;

/**
 * 业务字段 服务层实现
 * 
 * @author ct
 */
@Service
public class GenTableColumnServiceImpl implements IGenTableColumnService
{
	private final GenTableColumnRepository genTableColumnRepository;

	public GenTableColumnServiceImpl(GenTableColumnRepository genTableColumnRepository)
	{
		this.genTableColumnRepository = genTableColumnRepository;
	}

	/**
     * 查询业务字段列表
     * 
     * @param tableId 业务字段编号
     * @return 业务字段集合
     */
	@Override
	public List<GenTableColumn> selectGenTableColumnListByTableId(Long tableId)
	{
	    return genTableColumnRepository.findByTableIdOrderBySortAsc(tableId);
	}
	
    /**
     * 新增业务字段
     * 
     * @param genTableColumn 业务字段信息
     * @return 结果
     */
	@Override
	public int insertGenTableColumn(GenTableColumn genTableColumn)
	{
	    genTableColumnRepository.save(genTableColumn);
	    return 1;
	}
	
	/**
     * 修改业务字段
     * 
     * @param genTableColumn 业务字段信息
     * @return 结果
     */
	@Override
	public int updateGenTableColumn(GenTableColumn genTableColumn)
	{
	    genTableColumnRepository.save(genTableColumn);
	    return 1;
	}

	/**
     * 删除业务字段对象
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
	@Override
	public int deleteGenTableColumnByIds(String ids)
	{
		java.util.List<Long> idList = java.util.Arrays.asList(Convert.toLongArray(ids));
		genTableColumnRepository.deleteAllById(idList);
		return idList.size();
	}
}