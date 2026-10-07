package com.scaffold.system.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.scaffold.common.core.constant.UserConstants;
import com.scaffold.common.core.exception.ServiceException;
import com.scaffold.common.core.utils.SpringUtils;
import com.scaffold.common.core.utils.StringUtils;
import com.scaffold.common.datascope.annotation.DataScope;
import com.scaffold.common.security.utils.SecurityUtils;
import com.scaffold.system.api.domain.SysRole;
import com.scaffold.system.api.domain.SysRoleDept;
import com.scaffold.system.domain.SysRoleMenu;
import com.scaffold.system.domain.SysUserRole;
import org.springframework.data.jpa.domain.Specification;
import com.scaffold.common.core.jpa.DataScopeContext;
import com.scaffold.common.core.jpa.JpaSpecs;
import com.scaffold.system.repository.SysRoleDeptRepository;
import com.scaffold.system.repository.SysRoleRepository;
import com.scaffold.system.repository.SysRoleMenuRepository;
import com.scaffold.system.repository.SysUserRoleRepository;
import com.scaffold.system.service.ISysRoleService;

/**
 * 角色 业务层处理
 * 
 * @author ct
 */
@Service
public class SysRoleServiceImpl implements ISysRoleService
{
    private final SysRoleRepository roleRepository;
    private final SysRoleMenuRepository roleMenuRepository;
    private final SysUserRoleRepository userRoleRepository;
    private final SysRoleDeptRepository roleDeptRepository;

    public SysRoleServiceImpl(SysRoleRepository roleRepository, SysRoleMenuRepository roleMenuRepository,
                            SysUserRoleRepository userRoleRepository, SysRoleDeptRepository roleDeptRepository)
    {
        this.roleRepository = roleRepository;
        this.roleMenuRepository = roleMenuRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleDeptRepository = roleDeptRepository;
    }

    /**
     * 根据条件分页查询角色数据
     * 
     * @param role 角色信息
     * @return 角色数据集合信息
     */
    @Override
    @DataScope(deptField = "deptId")
    public List<SysRole> selectRoleList(SysRole role)
    {
        if (role == null)
        {
            role = new SysRole();
        }
        Specification<Object> spec = JpaSpecs.likeIf("roleName", role.getRoleName())
                .and(JpaSpecs.eqIfNotBlank("status", role.getStatus()))
                .and(JpaSpecs.likeIf("roleKey", role.getRoleKey()))
                .and(JpaSpecs.eqIfNotBlank("delFlag", "0"))
                .and(JpaSpecs.dateRangeIf("createTime", role.getParams()));
        return roleRepository.list(DataScopeContext.spec().and(spec));
    }

    @Override
    public com.scaffold.common.core.web.page.TableDataInfo selectRolePage(SysRole role, com.scaffold.common.core.web.page.PageDomain page)
    {
        if (role == null)
        {
            role = new SysRole();
        }
        Specification<Object> spec = JpaSpecs.likeIf("roleName", role.getRoleName())
                .and(JpaSpecs.eqIfNotBlank("status", role.getStatus()))
                .and(JpaSpecs.likeIf("roleKey", role.getRoleKey()))
                .and(JpaSpecs.eqIfNotBlank("delFlag", "0"))
                .and(JpaSpecs.dateRangeIf("createTime", role.getParams()));
        return com.scaffold.common.core.web.page.TableDataInfo.from(roleRepository.page(DataScopeContext.spec().and(spec),
                com.scaffold.common.core.utils.PageUtils.toPageRequest(page)));
    }

    /**
     * 根据用户ID查询角色
     * 
     * @param userId 用户ID
     * @return 角色列表
     */
    @Override
    public List<SysRole> selectRolesByUserId(Long userId)
    {
        List<SysRole> userRoles = roleRepository.selectRolePermissionByUserId(userId);
        List<SysRole> roles = selectRoleAll();
        for (SysRole role : roles)
        {
            for (SysRole userRole : userRoles)
            {
                if (role.getRoleId().longValue() == userRole.getRoleId().longValue())
                {
                    role.setFlag(true);
                    break;
                }
            }
        }
        return roles;
    }

    /**
     * 根据用户ID查询权限
     * 
     * @param userId 用户ID
     * @return 权限列表
     */
    @Override
    public Set<String> selectRolePermissionByUserId(Long userId)
    {
        List<SysRole> perms = roleRepository.selectRolePermissionByUserId(userId);
        Set<String> permsSet = new HashSet<>();
        for (SysRole perm : perms)
        {
            if (StringUtils.isNotNull(perm))
            {
                permsSet.addAll(Arrays.asList(perm.getRoleKey().trim().split(",")));
            }
        }
        return permsSet;
    }

    /**
     * 查询所有角色
     * 
     * @return 角色列表
     */
    @Override
    public List<SysRole> selectRoleAll()
    {
        return SpringUtils.getAopProxy(this).selectRoleList(new SysRole());
    }

    /**
     * 根据用户ID获取角色选择框列表
     * 
     * @param userId 用户ID
     * @return 选中角色ID列表
     */
    @Override
    public List<Long> selectRoleListByUserId(Long userId)
    {
        return userRoleRepository.selectRoleIdsByUserId(userId);
    }

    /**
     * 通过角色ID查询角色
     * 
     * @param roleId 角色ID
     * @return 角色对象信息
     */
    @Override
    public SysRole selectRoleById(Long roleId)
    {
        return roleRepository.findById(roleId).orElse(null);
    }

    /**
     * 校验角色名称是否唯一
     * 
     * @param role 角色信息
     * @return 结果
     */
    @Override
    public boolean checkRoleNameUnique(SysRole role)
    {
        Long roleId = StringUtils.isNull(role.getRoleId()) ? -1L : role.getRoleId();
        SysRole info = roleRepository.findByRoleName(role.getRoleName()).orElse(null);
        if (StringUtils.isNotNull(info) && info.getRoleId().longValue() != roleId.longValue())
        {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 校验角色权限是否唯一
     * 
     * @param role 角色信息
     * @return 结果
     */
    @Override
    public boolean checkRoleKeyUnique(SysRole role)
    {
        Long roleId = StringUtils.isNull(role.getRoleId()) ? -1L : role.getRoleId();
        SysRole info = roleRepository.findByRoleKey(role.getRoleKey()).orElse(null);
        if (StringUtils.isNotNull(info) && info.getRoleId().longValue() != roleId.longValue())
        {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 校验角色是否允许操作
     * 
     * @param role 角色信息
     */
    @Override
    public void checkRoleAllowed(SysRole role)
    {
        if (StringUtils.isNotNull(role.getRoleId()) && role.isAdmin())
        {
            throw new ServiceException("不允许操作超级管理员角色");
        }
    }

    /**
     * 校验角色是否有数据权限
     * 
     * @param roleIds 角色id
     */
    @Override
    public void checkRoleDataScope(Long... roleIds)
    {
        if (!SecurityUtils.isAdmin())
        {
            for (Long roleId : roleIds)
            {
                SysRole role = new SysRole();
                role.setRoleId(roleId);
                List<SysRole> roles = SpringUtils.getAopProxy(this).selectRoleList(role);
                if (StringUtils.isEmpty(roles))
                {
                    throw new ServiceException("没有权限访问角色数据！");
                }
            }
        }
    }

    /**
     * 通过角色ID查询角色使用数量
     * 
     * @param roleId 角色ID
     * @return 结果
     */
    @Override
    public int countUserRoleByRoleId(Long roleId)
    {
        return (int) userRoleRepository.countByRoleId(roleId);
    }

    /**
     * 新增保存角色信息
     * 
     * @param role 角色信息
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertRole(SysRole role)
    {
        // 新增角色信息
        roleRepository.save(role);
        return insertRoleMenu(role);
    }

    /**
     * 修改保存角色信息
     * 
     * @param role 角色信息
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateRole(SysRole role)
    {
        // 修改角色信息
        roleRepository.save(role);
        // 删除角色与菜单关联
        roleMenuRepository.deleteByRoleId(role.getRoleId());
        return insertRoleMenu(role);
    }

    /**
     * 修改角色状态
     * 
     * @param role 角色信息
     * @return 结果
     */
    @Override
    public int updateRoleStatus(SysRole role)
    {
        roleRepository.save(role);
        return 1;
    }

    /**
     * 修改数据权限信息
     * 
     * @param role 角色信息
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int authDataScope(SysRole role)
    {
        // 修改角色信息
        roleRepository.save(role);
        // 删除角色与部门关联
        roleDeptRepository.deleteByRoleId(role.getRoleId());
        // 新增角色和部门信息（数据权限）
        return insertRoleDept(role);
    }

    /**
     * 新增角色菜单信息
     * 
     * @param role 角色对象
     */
    public int insertRoleMenu(SysRole role)
    {
        int rows = 1;
        // 新增用户与角色管理
        List<SysRoleMenu> list = new ArrayList<SysRoleMenu>();
        for (Long menuId : role.getMenuIds())
        {
            SysRoleMenu rm = new SysRoleMenu();
            rm.setRoleId(role.getRoleId());
            rm.setMenuId(menuId);
            list.add(rm);
        }
        if (list.size() > 0)
        {
            roleMenuRepository.saveAll(list);
        }
        return rows;
    }

    /**
     * 新增角色部门信息(数据权限)
     *
     * @param role 角色对象
     */
    public int insertRoleDept(SysRole role)
    {
        int rows = 1;
        // 新增角色与部门（数据权限）管理
        List<SysRoleDept> list = new ArrayList<SysRoleDept>();
        for (Long deptId : role.getDeptIds())
        {
            SysRoleDept rd = new SysRoleDept();
            rd.setRoleId(role.getRoleId());
            rd.setDeptId(deptId);
            list.add(rd);
        }
        if (list.size() > 0)
        {
            roleDeptRepository.saveAll(list);
        }
        return rows;
    }

    /**
     * 通过角色ID删除角色
     * 
     * @param roleId 角色ID
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteRoleById(Long roleId)
    {
        // 删除角色与菜单关联
        roleMenuRepository.deleteByRoleId(roleId);
        // 删除角色与部门关联
        roleDeptRepository.deleteByRoleId(roleId);
        roleRepository.softDeleteById(roleId);
        return 1;
    }

    /**
     * 批量删除角色信息
     * 
     * @param roleIds 需要删除的角色ID
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteRoleByIds(Long[] roleIds)
    {
        for (Long roleId : roleIds)
        {
            checkRoleAllowed(new SysRole(roleId));
            checkRoleDataScope(roleId);
            SysRole role = selectRoleById(roleId);
            if (countUserRoleByRoleId(roleId) > 0)
            {
                throw new ServiceException(String.format("%1$s已分配,不能删除", role.getRoleName()));
            }
        }
        // 删除角色与菜单关联
        List<Long> roleIdList = java.util.Arrays.asList(roleIds);
        roleMenuRepository.deleteByRoleIds(roleIdList);
        // 删除角色与部门关联
        roleDeptRepository.deleteByRoleIds(roleIdList);
        roleRepository.softDeleteByIds(roleIdList);
        return roleIds.length;
    }

    /**
     * 取消授权用户角色
     * 
     * @param userRole 用户和角色关联信息
     * @return 结果
     */
    @Override
    public int deleteAuthUser(SysUserRole userRole)
    {
        userRoleRepository.deleteById(new com.scaffold.system.domain.SysUserRoleId(userRole.getUserId(), userRole.getRoleId()));
        return 1;
    }

    /**
     * 批量取消授权用户角色
     * 
     * @param roleId 角色ID
     * @param userIds 需要取消授权的用户数据ID
     * @return 结果
     */
    @Override
    public int deleteAuthUsers(Long roleId, Long[] userIds)
    {
        return userRoleRepository.deleteByRoleIdAndUserIds(roleId, java.util.Arrays.asList(userIds));
    }

    /**
     * 批量选择授权用户角色
     * 
     * @param roleId 角色ID
     * @param userIds 需要授权的用户数据ID
     * @return 结果
     */
    @Override
    public int insertAuthUsers(Long roleId, Long[] userIds)
    {
        // 新增用户与角色管理
        List<SysUserRole> list = new ArrayList<SysUserRole>();
        for (Long userId : userIds)
        {
            SysUserRole ur = new SysUserRole();
            ur.setUserId(userId);
            ur.setRoleId(roleId);
            list.add(ur);
        }
        userRoleRepository.saveAll(list);
        return list.size();
    }
}
