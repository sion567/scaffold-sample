package com.scaffold.system.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.scaffold.common.core.jpa.ScaffoldRepository;
import com.scaffold.system.api.domain.SysDictData;

/**
 * 字典数据数据访问。
 *
 * @author scaffold
 */
@Repository
public interface SysDictDataRepository extends ScaffoldRepository<SysDictData, Long> {

    /** 正常状态字典数据（selectDictDataByType） */
    List<SysDictData> findByDictTypeAndStatusOrderByDictSortAsc(String dictType, String status);

    /** 按状态取全部（缓存加载） */
    List<SysDictData> findByStatusOrderByDictSortAsc(String status);

    /** 标签查询（selectDictLabel） */
    Optional<SysDictData> findByDictTypeAndDictValue(String dictType, String dictValue);

    /** 同类型数据量（删除前校验） */
    long countByDictType(String dictType);

    /** 字典类型更名级联（updateDictDataType） */
    @Transactional
    @Modifying
    @Query("update SysDictData d set d.dictType = :newType where d.dictType = :oldType")
    int updateDictDataType(@Param("oldType") String oldType, @Param("newType") String newType);
}
