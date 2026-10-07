package com.scaffold.system.repository;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.scaffold.common.core.jpa.ScaffoldRepository;
import com.scaffold.system.api.domain.SysDictType;

/**
 * 字典类型数据访问。
 *
 * @author scaffold
 */
@Repository
public interface SysDictTypeRepository extends ScaffoldRepository<SysDictType, Long> {

    /** 按字典类型取记录（唯一键） */
    Optional<SysDictType> findByDictType(String dictType);
}
