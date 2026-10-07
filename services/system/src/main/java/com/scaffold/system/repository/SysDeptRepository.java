package com.scaffold.system.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.scaffold.common.core.jpa.ScaffoldRepository;
import com.scaffold.system.api.domain.SysDept;

/**
 * SysDept 数据访问（树结构查询走 ancestors 链 concat+LIKE，跨方言可移植）。
 *
 * @author scaffold
 */
@Repository
public interface SysDeptRepository extends ScaffoldRepository<SysDept, Long>
{
    /** 同父部门下同名部门（checkDeptNameUnique） */
    Optional<SysDept> findByDeptNameAndParentIdAndDelFlag(String deptName, Long parentId, String delFlag);

    /** 直接子节点数（hasChildByDeptId） */
    long countByParentIdAndDelFlag(Long parentId, String delFlag);

    /** ancestors 链上子部门（selectChildrenDeptById） */
    @Query("select d from SysDept d where concat(',', concat(d.ancestors, ',')) like concat('%,', :deptId, ',%')")
    List<SysDept> selectChildrenDeptById(@Param("deptId") Long deptId);

    /** 正常状态子部门数（selectNormalChildrenDeptById） */
    @Query("select count(d) from SysDept d where d.status = '0' and d.delFlag = '0' "
         + "and concat(',', concat(d.ancestors, ',')) like concat('%,', :deptId, ',%')")
    long countNormalChildren(@Param("deptId") Long deptId);

    /** 角色部门树勾选（selectDeptListByRoleId：deptCheckStrictly 时剔除父节点） */
    @Query("select d.deptId from SysDept d join SysRoleDept rd on rd.deptId = d.deptId "
         + "where rd.roleId = :roleId "
         + "and (:deptCheckStrictly = false or d.deptId not in "
         + "(select d2.parentId from SysDept d2 join SysRoleDept rd2 on rd2.deptId = d2.deptId where rd2.roleId = :roleId)) "
         + "order by d.parentId, d.orderNum")
    List<Long> selectDeptListByRoleId(@Param("roleId") Long roleId,
            @Param("deptCheckStrictly") boolean deptCheckStrictly);

    /** 启用上级部门（updateDeptStatusNormal） */
    @Transactional
    @Modifying
    @Query("update SysDept d set d.status = '0' where d.deptId in :deptIds")
    int updateDeptStatusNormal(@Param("deptIds") Collection<Long> deptIds);

    /** 排序（updateDeptSort） */
    @Transactional
    @Modifying
    @Query("update SysDept d set d.orderNum = :orderNum where d.deptId = :deptId")
    int updateDeptSort(@Param("deptId") Long deptId, @Param("orderNum") Integer orderNum);

    /** 软删除（deleteDeptById：del_flag='2'） */
    @Transactional
    @Modifying
    @Query("update SysDept d set d.delFlag = '2' where d.deptId = :deptId")
    int softDeleteById(@Param("deptId") Long deptId);
}
