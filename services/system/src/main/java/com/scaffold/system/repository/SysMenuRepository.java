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
import com.scaffold.system.domain.SysMenu;

/**
 * SysMenu 数据访问（用户/角色维度菜单查询走关联 join）。
 *
 * @author scaffold
 */
@Repository
public interface SysMenuRepository extends ScaffoldRepository<SysMenu, Long>
{
    /** 全量菜单树（selectMenuTreeAll：M/C 型、正常状态、排序） */
    List<SysMenu> findByMenuTypeInAndStatusOrderByParentIdAscOrderNumAsc(Collection<String> menuTypes, String status);

    /** 用户菜单列表（selectMenuListByUserId：全部类型 + 可选条件） */
    @Query("select distinct m from SysMenu m join SysRoleMenu rm on rm.menuId = m.menuId "
         + "join SysUserRole ur on ur.roleId = rm.roleId "
         + "join SysRole ro on ro.roleId = ur.roleId "
         + "where ur.userId = :userId and ro.status = '0' "
         + "and (:menuName is null or m.menuName like concat('%', :menuName, '%')) "
         + "and (:visible is null or m.visible = :visible) "
         + "and (:status is null or m.status = :status) "
         + "order by m.parentId, m.orderNum")
    List<SysMenu> selectMenuListByUserId(@Param("userId") Long userId, @Param("menuName") String menuName,
            @Param("visible") String visible, @Param("status") String status);

    /** 用户菜单树（selectMenuTreeByUserId：M/C 型） */
    @Query("select distinct m from SysMenu m join SysRoleMenu rm on rm.menuId = m.menuId "
         + "join SysUserRole ur on ur.roleId = rm.roleId "
         + "join SysRole ro on ro.roleId = ur.roleId "
         + "where ur.userId = :userId and m.menuType in ('M', 'C') and m.status = '0' and ro.status = '0' "
         + "order by m.parentId, m.orderNum")
    List<SysMenu> selectMenuTreeByUserId(@Param("userId") Long userId);

    /** 角色菜单树勾选（selectMenuListByRoleId：menuCheckStrictly 时剔除父节点） */
    @Query("select m.menuId from SysMenu m join SysRoleMenu rm on rm.menuId = m.menuId "
         + "where rm.roleId = :roleId "
         + "and (:menuCheckStrictly = false or m.menuId not in "
         + "(select m2.parentId from SysMenu m2 join SysRoleMenu rm2 on rm2.menuId = m2.menuId where rm2.roleId = :roleId)) "
         + "order by m.parentId, m.orderNum")
    List<Long> selectMenuListByRoleId(@Param("roleId") Long roleId, @Param("menuCheckStrictly") boolean menuCheckStrictly);

    /** 用户权限字符（selectMenuPermsByUserId） */
    @Query("select distinct m.perms from SysMenu m join SysRoleMenu rm on rm.menuId = m.menuId "
         + "join SysUserRole ur on ur.roleId = rm.roleId "
         + "join SysRole r on r.roleId = ur.roleId "
         + "where m.status = '0' and r.status = '0' and ur.userId = :userId and m.perms is not null")
    List<String> selectMenuPermsByUserId(@Param("userId") Long userId);

    /** 角色权限字符（selectMenuPermsByRoleId） */
    @Query("select distinct m.perms from SysMenu m join SysRoleMenu rm on rm.menuId = m.menuId "
         + "where m.status = '0' and rm.roleId = :roleId and m.perms is not null")
    List<String> selectMenuPermsByRoleId(@Param("roleId") Long roleId);

    /** 子菜单数（hasChildByMenuId） */
    long countByParentId(Long parentId);

    /** 同父同名菜单（checkMenuNameUnique） */
    Optional<SysMenu> findByMenuNameAndParentId(String menuName, Long parentId);

    /** 路由冲突检测（selectMenusByPathOrRouteName） */
    @Query("select m from SysMenu m where m.menuType in ('M', 'C') "
         + "and (m.path = :path or m.path = :routeName or m.routeName = :path or m.routeName = :routeName)")
    List<SysMenu> selectMenusByPathOrRouteName(@Param("path") String path, @Param("routeName") String routeName);

    /** 排序（updateMenuSort） */
    @Transactional
    @Modifying
    @Query("update SysMenu m set m.orderNum = :orderNum where m.menuId = :menuId")
    int updateMenuSort(@Param("menuId") Long menuId, @Param("orderNum") Integer orderNum);
}
