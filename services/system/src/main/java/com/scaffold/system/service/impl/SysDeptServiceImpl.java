package com.scaffold.system.service.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.scaffold.common.core.constant.UserConstants;
import com.scaffold.common.core.exception.ServiceException;
import com.scaffold.common.core.jpa.DataScopeContext;
import com.scaffold.common.core.jpa.JpaSpecs;
import com.scaffold.common.core.text.Convert;
import com.scaffold.common.core.utils.SpringUtils;
import com.scaffold.common.core.utils.StringUtils;
import com.scaffold.common.datascope.annotation.DataScope;
import com.scaffold.common.security.utils.SecurityUtils;
import com.scaffold.system.api.domain.SysDept;
import com.scaffold.system.api.domain.SysRole;
import com.scaffold.system.domain.vo.TreeSelect;
import com.scaffold.system.repository.SysDeptRepository;
import com.scaffold.system.repository.SysRoleRepository;
import com.scaffold.system.repository.SysUserRepository;
import com.scaffold.system.service.ISysDeptService;

/**
 * 部门管理 服务实现（JPA：Repository + Specification，数据权限经 @DataScope + DataScopeContext）
 *
 * @author ct
 */
@Service
public class SysDeptServiceImpl implements ISysDeptService
{
    private final SysDeptRepository deptRepository;
    private final SysRoleRepository roleRepository;
    private final SysUserRepository userRepository;

    public SysDeptServiceImpl(SysDeptRepository deptRepository, SysRoleRepository roleRepository,
            SysUserRepository userRepository)
    {
        this.deptRepository = deptRepository;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
    }

    /**
     * 查询部门管理数据（delFlag='0' + 动态条件 + 数据权限）
     *
     * @param dept 部门信息
     * @return 部门信息集合
     */
    @Override
    @DataScope(deptField = "deptId")
    public List<SysDept> selectDeptList(SysDept dept)
    {
        if (dept == null)
        {
            dept = new SysDept();
        }
        Specification<Object> spec = JpaSpecs.eqIf("deptId", dept.getDeptId())
                .and(JpaSpecs.eqIf("parentId", StringUtils.isNotNull(dept.getParentId()) && dept.getParentId() != 0 ? dept.getParentId() : null))
                .and(JpaSpecs.likeIf("deptName", dept.getDeptName()))
                .and(JpaSpecs.eqIfNotBlank("status", dept.getStatus()))
                .and(JpaSpecs.eqIfNotBlank("delFlag", "0"));
        return deptRepository.list(DataScopeContext.spec().and(spec));
    }

    /**
     * 查询部门树结构信息
     *
     * @param dept 部门信息
     * @return 部门树信息集合
     */
    @Override
    public List<TreeSelect> selectDeptTreeList(SysDept dept)
    {
        List<SysDept> depts = SpringUtils.getAopProxy(this).selectDeptList(dept);
        return buildDeptTreeSelect(depts);
    }

    /**
     * 构建前端所需要树结构
     *
     * @param depts 部门列表
     * @return 树结构列表
     */
    @Override
    public List<SysDept> buildDeptTree(List<SysDept> depts)
    {
        List<SysDept> returnList = new ArrayList<SysDept>();
        List<Long> tempList = depts.stream().map(SysDept::getDeptId).collect(Collectors.toList());
        for (SysDept dept : depts)
        {
            // 如果是顶级节点, 遍历该父节点的所有子节点
            if (!tempList.contains(dept.getParentId()))
            {
                recursionFn(depts, dept);
                returnList.add(dept);
            }
        }
        if (returnList.isEmpty())
        {
            returnList = depts;
        }
        return returnList;
    }

    /**
     * 构建前端所需要下拉树结构
     *
     * @param depts 部门列表
     * @return 下拉树结构列表
     */
    @Override
    public List<TreeSelect> buildDeptTreeSelect(List<SysDept> depts)
    {
        List<SysDept> deptTrees = buildDeptTree(depts);
        return deptTrees.stream().map(TreeSelect::new).collect(Collectors.toList());
    }

    /**
     * 根据角色ID查询部门树信息
     *
     * @param roleId 角色ID
     * @return 选中部门列表
     */
    @Override
    public List<Long> selectDeptListByRoleId(Long roleId)
    {
        SysRole role = roleRepository.findById(roleId).orElse(null);
        boolean strictly = role != null && role.isDeptCheckStrictly();
        return deptRepository.selectDeptListByRoleId(roleId, strictly);
    }

    /**
     * 根据部门ID查询信息
     *
     * @param deptId 部门ID
     * @return 部门信息
     */
    @Override
    public SysDept selectDeptById(Long deptId)
    {
        return deptRepository.findById(deptId).orElse(null);
    }

    /**
     * 根据ID查询所有子部门（正常状态）
     *
     * @param deptId 部门ID
     * @return 子部门数
     */
    @Override
    public int selectNormalChildrenDeptById(Long deptId)
    {
        return (int) deptRepository.countNormalChildren(deptId);
    }

    /**
     * 查询部门列表（排除节点）
     *
     * @param deptId 部门ID
     * @return 排除该部门的列表
     */
    @Override
    public List<SysDept> selectDeptExcludeChild(Long deptId)
    {
        List<SysDept> depts = selectDeptList(new SysDept());
        depts.removeIf(d -> d.getDeptId().intValue() == deptId
            || ArrayUtils.contains(StringUtils.split(d.getAncestors(), ","), deptId + ""));
        return depts;
    }

    /**
     * 是否存在子节点
     *
     * @param deptId 部门ID
     * @return 结果
     */
    @Override
    public boolean hasChildByDeptId(Long deptId)
    {
        return deptRepository.countByParentIdAndDelFlag(deptId, "0") > 0;
    }

    /**
     * 查询部门是否存在用户
     *
     * @param deptId 部门ID
     * @return 结果 true 存在 false 不存在
     */
    @Override
    public boolean checkDeptExistUser(Long deptId)
    {
        return userRepository.countByDeptIdAndDelFlag(deptId, "0") > 0;
    }

    /**
     * 校验部门名称是否唯一
     *
     * @param dept 部门信息
     * @return 结果
     */
    @Override
    public boolean checkDeptNameUnique(SysDept dept)
    {
        Long deptId = StringUtils.isNull(dept.getDeptId()) ? -1L : dept.getDeptId();
        SysDept info = deptRepository.findByDeptNameAndParentIdAndDelFlag(dept.getDeptName(), dept.getParentId(), "0").orElse(null);
        if (StringUtils.isNotNull(info) && info.getDeptId().longValue() != deptId.longValue())
        {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 校验部门是否有数据权限
     *
     * @param deptId 部门id
     */
    @Override
    public void checkDeptDataScope(Long deptId)
    {
        if (!SecurityUtils.isAdmin() && StringUtils.isNotNull(deptId))
        {
            SysDept dept = new SysDept();
            dept.setDeptId(deptId);
            List<SysDept> depts = SpringUtils.getAopProxy(this).selectDeptList(dept);
            if (StringUtils.isEmpty(depts))
            {
                throw new ServiceException("没有权限访问部门数据！");
            }
        }
    }

    /**
     * 新增保存部门信息
     *
     * @param dept 部门信息
     * @return 结果
     */
    @Override
    public int insertDept(SysDept dept)
    {
        SysDept info = deptRepository.findById(dept.getParentId()).orElseThrow(
                () -> new ServiceException("上级部门不存在"));
        // 如果父节点不为正常状态,则不允许新增子节点
        if (!UserConstants.DEPT_NORMAL.equals(info.getStatus()))
        {
            throw new ServiceException("部门停用，不允许新增");
        }
        dept.setAncestors(info.getAncestors() + "," + dept.getParentId());
        deptRepository.save(dept);
        return 1;
    }

    /**
     * 修改保存部门信息
     *
     * @param dept 部门信息
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateDept(SysDept dept)
    {
        SysDept newParentDept = deptRepository.findById(dept.getParentId()).orElse(null);
        SysDept oldDept = deptRepository.findById(dept.getDeptId()).orElse(null);
        if (StringUtils.isNotNull(newParentDept) && StringUtils.isNotNull(oldDept))
        {
            String newAncestors = newParentDept.getAncestors() + "," + newParentDept.getDeptId();
            String oldAncestors = oldDept.getAncestors();
            dept.setAncestors(newAncestors);
            updateDeptChildren(dept.getDeptId(), newAncestors, oldAncestors);
        }
        int result = deptRepository.save(dept) != null ? 1 : 0;
        if (UserConstants.DEPT_NORMAL.equals(dept.getStatus()) && StringUtils.isNotEmpty(dept.getAncestors())
                && !StringUtils.equals("0", dept.getAncestors()))
        {
            // 如果该部门是启用状态，则启用该部门的所有上级部门
            updateParentDeptStatusNormal(dept);
        }
        return result;
    }

    /**
     * 修改该部门的父级部门状态
     *
     * @param dept 当前部门
     */
    private void updateParentDeptStatusNormal(SysDept dept)
    {
        String ancestors = dept.getAncestors();
        Long[] deptIds = Convert.toLongArray(ancestors);
        deptRepository.updateDeptStatusNormal(java.util.Arrays.asList(deptIds));
    }

    /**
     * 修改子元素关系
     *
     * @param deptId 被修改的部门ID
     * @param newAncestors 新的父ID集合
     * @param oldAncestors 旧的父ID集合
     */
    public void updateDeptChildren(Long deptId, String newAncestors, String oldAncestors)
    {
        List<SysDept> children = deptRepository.selectChildrenDeptById(deptId);
        for (SysDept child : children)
        {
            child.setAncestors(child.getAncestors().replaceFirst(oldAncestors, newAncestors));
        }
        if (children.size() > 0)
        {
            deptRepository.saveAll(children);
        }
    }

    /**
     * 保存部门排序
     *
     * @param deptIds 部门ID数组
     * @param orderNums 排序数组
     */
    @Override
    @Transactional
    public void updateDeptSort(String[] deptIds, String[] orderNums)
    {
        try
        {
            for (int i = 0; i < deptIds.length; i++)
            {
                deptRepository.updateDeptSort(Convert.toLong(deptIds[i]), Convert.toInt(orderNums[i]));
            }
        }
        catch (Exception e)
        {
            throw new ServiceException("保存排序异常，请联系管理员");
        }
    }

    /**
     * 删除部门管理信息（软删除）
     *
     * @param deptId 部门ID
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteDeptById(Long deptId)
    {
        return deptRepository.softDeleteById(deptId);
    }

    /**
     * 递归列表
     */
    private void recursionFn(List<SysDept> list, SysDept t)
    {
        // 得到子节点列表
        List<SysDept> childList = getChildList(list, t);
        t.setChildren(childList);
        for (SysDept tChild : childList)
        {
            if (hasChild(list, tChild))
            {
                recursionFn(list, tChild);
            }
        }
    }

    /**
     * 得到子节点列表
     */
    private List<SysDept> getChildList(List<SysDept> list, SysDept t)
    {
        List<SysDept> tlist = new ArrayList<SysDept>();
        Iterator<SysDept> it = list.iterator();
        while (it.hasNext())
        {
            SysDept n = (SysDept) it.next();
            if (StringUtils.isNotNull(n.getParentId()) && n.getParentId().longValue() == t.getDeptId().longValue())
            {
                tlist.add(n);
            }
        }
        return tlist;
    }

    /**
     * 判断是否有子节点
     */
    private boolean hasChild(List<SysDept> list, SysDept t)
    {
        return getChildList(list, t).size() > 0 ? true : false;
    }
}
