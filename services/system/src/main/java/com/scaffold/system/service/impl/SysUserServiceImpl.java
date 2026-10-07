package com.scaffold.system.service.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import com.scaffold.common.core.constant.UserConstants;
import com.scaffold.common.core.exception.ServiceException;
import com.scaffold.common.core.utils.SpringUtils;
import com.scaffold.common.core.utils.StringUtils;
import com.scaffold.common.core.utils.bean.BeanValidators;
import com.scaffold.common.datascope.annotation.DataScope;
import com.scaffold.common.security.utils.SecurityUtils;
import com.scaffold.system.api.domain.SysRole;
import com.scaffold.system.api.domain.SysUser;
import com.scaffold.system.domain.SysPost;
import com.scaffold.system.domain.SysUserPost;
import com.scaffold.system.domain.SysUserRole;
import com.scaffold.common.core.jpa.DataScopeContext;
import com.scaffold.common.core.jpa.JpaSpecs;
import com.scaffold.system.repository.SysPostRepository;
import com.scaffold.system.repository.SysRoleRepository;
import com.scaffold.system.repository.SysUserPostRepository;
import com.scaffold.system.repository.SysUserRepository;
import com.scaffold.system.repository.SysUserRoleRepository;
import org.springframework.data.jpa.domain.Specification;
import com.scaffold.system.service.ISysConfigService;
import com.scaffold.system.service.ISysDeptService;
import com.scaffold.system.service.ISysUserPasswordHistoryService;
import com.scaffold.system.service.ISysUserService;

/**
 * 用户 业务层处理
 * 
 * @author ct
 */
@Service
public class SysUserServiceImpl implements ISysUserService
{
    private static final Logger log = LoggerFactory.getLogger(SysUserServiceImpl.class);

    private final SysUserRepository userRepository;
    private final SysRoleRepository roleRepository;
    private final SysPostRepository postRepository;
    private final SysUserRoleRepository userRoleRepository;
    private final SysUserPostRepository userPostRepository;
    private final ISysConfigService configService;
    private final ISysDeptService deptService;
    private final ISysUserPasswordHistoryService passwordHistoryService;
    protected final Validator validator;

    public SysUserServiceImpl(SysUserRepository userRepository, SysRoleRepository roleRepository, SysPostRepository postRepository,
                             SysUserRoleRepository userRoleRepository, SysUserPostRepository userPostRepository,
                             ISysConfigService configService, ISysDeptService deptService,
                             ISysUserPasswordHistoryService passwordHistoryService, Validator validator)
    {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.postRepository = postRepository;
        this.userRoleRepository = userRoleRepository;
        this.userPostRepository = userPostRepository;
        this.configService = configService;
        this.deptService = deptService;
        this.passwordHistoryService = passwordHistoryService;
        this.validator = validator;
    }

    /**
     * 根据条件分页查询用户列表
     * 
     * @param user 用户信息
     * @return 用户信息集合信息
     */
    @Override
    @DataScope(deptField = "deptId", userField = "userId")
    public List<SysUser> selectUserList(SysUser user)
    {
        if (user == null)
        {
            user = new SysUser();
        }
        List<SysUser> users = userRepository.list(DataScopeContext.spec().and(buildUserSpec(user)));
        fillDeptInfo(users);
        return users;
    }

    /** 部门过滤：本部门或 ancestors 链上子部门（等价原 XML 的 OR 子查询） */
    private Specification<Object> deptOrChildrenSpec(Long deptId)
    {
        if (deptId == null)
        {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> {
            jakarta.persistence.criteria.Subquery<Long> sub = query.subquery(Long.class);
            jakarta.persistence.criteria.Root<com.scaffold.system.api.domain.SysDept> d =
                    sub.from(com.scaffold.system.api.domain.SysDept.class);
            sub.select(d.get("deptId"));
            sub.where(cb.like(cb.concat(cb.concat(",", d.get("ancestors")), ","), "%," + deptId + ",%"));
            return cb.or(cb.equal(root.get("deptId"), deptId), root.get("deptId").in(sub));
        };
    }

    /**
     * 根据条件分页查询已分配用户角色列表
     * 
     * @param user 用户信息
     * @return 用户信息集合信息
     */
    @Override
    @DataScope(deptField = "deptId", userField = "userId")
    public List<SysUser> selectAllocatedList(SysUser user)
    {
        Long roleId = (Long) user.getParams().get("roleId");
        Specification<Object> spec = allocatedBaseSpec(user, roleId, true);
        List<SysUser> users = userRepository.list(DataScopeContext.spec().and(spec));
        fillDeptInfo(users);
        return users;
    }

    /**
     * 根据条件分页查询未分配用户角色列表
     * 
     * @param user 用户信息
     * @return 用户信息集合信息
     */
    @Override
    @DataScope(deptField = "deptId", userField = "userId")
    public List<SysUser> selectUnallocatedList(SysUser user)
    {
        Long roleId = (Long) user.getParams().get("roleId");
        Specification<Object> spec = allocatedBaseSpec(user, roleId, false);
        List<SysUser> users = userRepository.list(DataScopeContext.spec().and(spec));
        fillDeptInfo(users);
        return users;
    }

    /** 已分配/未分配公共条件（等价原 XML 两查询：having/不存在该角色映射 + 姓名/手机号过滤） */
    private Specification<Object> allocatedBaseSpec(SysUser user, Long roleId, boolean allocated)
    {
        return (root, query, cb) -> {
            jakarta.persistence.criteria.Subquery<Long> sub = query.subquery(Long.class);
            jakarta.persistence.criteria.Root<com.scaffold.system.domain.SysUserRole> ur =
                    sub.from(com.scaffold.system.domain.SysUserRole.class);
            sub.select(ur.get("userId"));
            sub.where(cb.and(cb.equal(ur.get("roleId"), roleId), cb.equal(ur.get("userId"), root.get("userId"))));
            jakarta.persistence.criteria.Predicate roleCond = allocated ? cb.exists(sub) : cb.not(cb.exists(sub));
            jakarta.persistence.criteria.Predicate base = cb.and(
                    cb.equal(root.get("delFlag"), "0"),
                    likeIf(cb, root.get("userName").as(String.class), user.getUserName()),
                    likeIf(cb, root.get("phonenumber").as(String.class), user.getPhonenumber()));
            return cb.and(base, roleCond);
        };
    }

    /** like 条件（空白跳过） */
    private static jakarta.persistence.criteria.Predicate likeIf(
            jakarta.persistence.criteria.CriteriaBuilder cb, jakarta.persistence.criteria.Expression<String> path, String value)
    {
        return StringUtils.isEmpty(value) ? cb.conjunction() : cb.like(path, "%" + value.trim() + "%");
    }

    /** 批量回填部门展示信息（原 XML join sys_dept 的 deptName/leader） */
    private void fillDeptInfo(List<SysUser> users)
    {
        Set<Long> deptIds = new HashSet<>();
        for (SysUser u : users)
        {
            if (u.getDeptId() != null)
            {
                deptIds.add(u.getDeptId());
            }
        }
        if (deptIds.isEmpty())
        {
            return;
        }
        Map<Long, com.scaffold.system.api.domain.SysDept> deptMap = new java.util.HashMap<>();
        for (com.scaffold.system.api.domain.SysDept d : userRepository.findDepts(deptIds))
        {
            deptMap.put(d.getDeptId(), d);
        }
        for (SysUser u : users)
        {
            com.scaffold.system.api.domain.SysDept d = deptMap.get(u.getDeptId());
            if (d != null)
            {
                u.setDept(d);
            }
        }
    }

    /** 用户列表动态条件（对齐原 SysUserMapper.xml selectUserList） */
    private Specification<Object> buildUserSpec(SysUser user)
    {
        if (user == null)
        {
            user = new SysUser();
        }
        Long deptId = user.getDeptId() != null && user.getDeptId() != 0 ? user.getDeptId() : null;
        return JpaSpecs.eqIf("userId", user.getUserId())
                .and(JpaSpecs.likeIf("userName", user.getUserName()))
                .and(JpaSpecs.eqIfNotBlank("status", user.getStatus()))
                .and(JpaSpecs.likeIf("phonenumber", user.getPhonenumber()))
                .and(JpaSpecs.dateRangeIf("createTime", user.getParams()))
                .and(deptOrChildrenSpec(deptId))
                .and(JpaSpecs.eqIfNotBlank("delFlag", "0"));
    }

    @Override
    public com.scaffold.common.core.web.page.TableDataInfo selectUserPage(SysUser user, com.scaffold.common.core.web.page.PageDomain page)
    {
        org.springframework.data.domain.Page<SysUser> result = userRepository.page(
                DataScopeContext.spec().and(buildUserSpec(user)),
                com.scaffold.common.core.utils.PageUtils.toPageRequest(page));
        fillDeptInfo(result.getContent());
        return com.scaffold.common.core.web.page.TableDataInfo.from(result);
    }

    @Override
    public com.scaffold.common.core.web.page.TableDataInfo selectAllocatedPage(SysUser user, com.scaffold.common.core.web.page.PageDomain page)
    {
        Long roleId = (Long) user.getParams().get("roleId");
        org.springframework.data.domain.Page<SysUser> result = userRepository.page(
                DataScopeContext.spec().and(allocatedBaseSpec(user, roleId, true)),
                com.scaffold.common.core.utils.PageUtils.toPageRequest(page));
        fillDeptInfo(result.getContent());
        return com.scaffold.common.core.web.page.TableDataInfo.from(result);
    }

    @Override
    public com.scaffold.common.core.web.page.TableDataInfo selectUnallocatedPage(SysUser user, com.scaffold.common.core.web.page.PageDomain page)
    {
        Long roleId = (Long) user.getParams().get("roleId");
        org.springframework.data.domain.Page<SysUser> result = userRepository.page(
                DataScopeContext.spec().and(allocatedBaseSpec(user, roleId, false)),
                com.scaffold.common.core.utils.PageUtils.toPageRequest(page));
        fillDeptInfo(result.getContent());
        return com.scaffold.common.core.web.page.TableDataInfo.from(result);
    }

    /**
     * 通过用户名查询用户
     * 
     * @param userName 用户名
     * @return 用户对象信息
     */
    @Override
    public SysUser selectUserByUserName(String userName)
    {
        return userRepository.findByUserNameAndDelFlag(userName, "0").orElse(null);
    }

    /**
     * 通过用户ID查询用户
     * 
     * @param userId 用户ID
     * @return 用户对象信息
     */
    @Override
    public SysUser selectUserById(Long userId)
    {
        return userRepository.findById(userId).orElse(null);
    }

    /**
     * 查询用户所属角色组
     * 
     * @param userName 用户名
     * @return 结果
     */
    @Override
    public String selectUserRoleGroup(String userName)
    {
        List<SysRole> list = userRepository.selectRolesByUserName(userName);
        if (CollectionUtils.isEmpty(list))
        {
            return StringUtils.EMPTY;
        }
        return list.stream().map(SysRole::getRoleName).collect(Collectors.joining(","));
    }

    /**
     * 查询用户所属岗位组
     * 
     * @param userName 用户名
     * @return 结果
     */
    @Override
    public String selectUserPostGroup(String userName)
    {
        List<SysPost> list = postRepository.selectPostsByUserName(userName);
        if (CollectionUtils.isEmpty(list))
        {
            return StringUtils.EMPTY;
        }
        return list.stream().map(SysPost::getPostName).collect(Collectors.joining(","));
    }

    /**
     * 校验用户名称是否唯一
     * 
     * @param user 用户信息
     * @return 结果
     */
    @Override
    public boolean checkUserNameUnique(SysUser user)
    {
        Long userId = StringUtils.isNull(user.getUserId()) ? -1L : user.getUserId();
        SysUser info = userRepository.findByUserNameAndDelFlag(user.getUserName(), "0").orElse(null);
        if (StringUtils.isNotNull(info) && info.getUserId().longValue() != userId.longValue())
        {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 校验手机号码是否唯一
     *
     * @param user 用户信息
     * @return
     */
    @Override
    public boolean checkPhoneUnique(SysUser user)
    {
        Long userId = StringUtils.isNull(user.getUserId()) ? -1L : user.getUserId();
        SysUser info = userRepository.findByPhonenumberAndDelFlag(user.getPhonenumber(), "0").orElse(null);
        if (StringUtils.isNotNull(info) && info.getUserId().longValue() != userId.longValue())
        {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 校验email是否唯一
     *
     * @param user 用户信息
     * @return
     */
    @Override
    public boolean checkEmailUnique(SysUser user)
    {
        Long userId = StringUtils.isNull(user.getUserId()) ? -1L : user.getUserId();
        SysUser info = userRepository.findByEmailAndDelFlag(user.getEmail(), "0").orElse(null);
        if (StringUtils.isNotNull(info) && info.getUserId().longValue() != userId.longValue())
        {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 校验用户是否允许操作
     * 
     * @param user 用户信息
     */
    @Override
    public void checkUserAllowed(SysUser user)
    {
        if (StringUtils.isNotNull(user.getUserId()) && user.isAdmin())
        {
            throw new ServiceException("不允许操作超级管理员用户");
        }
    }

    /**
     * 校验用户是否有数据权限
     * 
     * @param userId 用户id
     */
    @Override
    public void checkUserDataScope(Long userId)
    {
        if (!SecurityUtils.isAdmin())
        {
            SysUser user = new SysUser();
            user.setUserId(userId);
            List<SysUser> users = SpringUtils.getAopProxy(this).selectUserList(user);
            if (StringUtils.isEmpty(users))
            {
                throw new ServiceException("没有权限访问用户数据！");
            }
        }
    }

    /**
     * 新增保存用户信息
     * 
     * @param user 用户信息
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertUser(SysUser user)
    {
        // 新增用户信息
        userRepository.save(user);
        // 新增用户岗位关联
        insertUserPost(user);
        // 新增用户与角色管理
        insertUserRole(user);
        return 1;
    }

    /**
     * 注册用户信息
     * 
     * @param user 用户信息
     * @return 结果
     */
    @Override
    public boolean registerUser(SysUser user)
    {
        userRepository.save(user);
        return true;
    }

    /**
     * 修改保存用户信息
     * 
     * @param user 用户信息
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateUser(SysUser user)
    {
        Long userId = user.getUserId();
        // 删除用户与角色关联
        userRoleRepository.deleteByUserId(userId);
        // 新增用户与角色管理
        insertUserRole(user);
        // 删除用户与岗位关联
        userPostRepository.deleteByUserId(userId);
        // 新增用户与岗位管理
        insertUserPost(user);
        userRepository.save(user);
        return 1;
    }

    /**
     * 用户授权角色
     * 
     * @param userId 用户ID
     * @param roleIds 角色组
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void insertUserAuth(Long userId, Long[] roleIds)
    {
        userRoleRepository.deleteByUserId(userId);
        insertUserRole(userId, roleIds);
    }

    /**
     * 修改用户状态
     * 
     * @param user 用户信息
     * @return 结果
     */
    @Override
    public int updateUserStatus(SysUser user)
    {
        return userRepository.updateStatus(user.getUserId(), user.getStatus());
    }

    /**
     * 修改用户基本信息
     * 
     * @param user 用户信息
     * @return 结果
     */
    @Override
    public boolean updateUserProfile(SysUser user)
    {
        userRepository.save(user);
        return true;
    }

    /**
     * 修改用户头像
     * 
     * @param userId 用户ID
     * @param avatar 头像地址
     * @return 结果
     */
    @Override
    public boolean updateUserAvatar(Long userId, String avatar)
    {
        return userRepository.updateAvatar(userId, avatar) > 0;
    }

    /**
     * 更新用户登录信息（IP和登录时间）
     * 
     * @param user 用户信息
     * @return 结果
     */
    public boolean updateLoginInfo(SysUser user)
    {
        // 登录留痕属于内部审计更新，不参与乐观锁（避免并发登录时 version 不匹配导致登录失败）
        return userRepository.updateLoginInfo(user.getUserId(), user.getLoginIp(), user.getLoginDate()) > 0;
    }

    /**
     * 重置用户密码
     * 
     * @param user 用户信息
     * @return 结果
     */
    @Override
    public int resetPwd(SysUser user)
    {
        return userRepository.resetPassword(user.getUserId(), user.getPassword());
    }

    /**
     * 重置用户密码
     * 
     * @param userId 用户ID
     * @param password 密码
     * @return 结果
     */
    @Override
    public int resetUserPwd(Long userId, String password)
    {
        return userRepository.resetPassword(userId, password);
    }

    /**
     * 重置用户密码并记录密码历史（同一事务：改密 + 记录历史 + 清理超限历史）
     *
     * @param userId 用户ID
     * @param newPassword SM3 加密后的新密码
     * @param historyCount 历史保留条数（<=0 不清理历史）
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int resetUserPwdWithHistory(Long userId, String newPassword, int historyCount)
    {
        int rows = userRepository.resetPassword(userId, newPassword);
        if (rows > 0)
        {
            passwordHistoryService.insertPasswordHistory(userId, newPassword);
            if (historyCount > 0)
            {
                passwordHistoryService.cleanupHistory(userId, historyCount);
            }
        }
        return rows;
    }

    /**
     * 新增用户角色信息
     * 
     * @param user 用户对象
     */
    public void insertUserRole(SysUser user)
    {
        this.insertUserRole(user.getUserId(), user.getRoleIds());
    }

    /**
     * 新增用户岗位信息
     * 
     * @param user 用户对象
     */
    public void insertUserPost(SysUser user)
    {
        Long[] posts = user.getPostIds();
        if (StringUtils.isNotEmpty(posts))
        {
            // 新增用户与岗位管理
            List<SysUserPost> list = new ArrayList<SysUserPost>();
            for (Long postId : posts)
            {
                SysUserPost up = new SysUserPost();
                up.setUserId(user.getUserId());
                up.setPostId(postId);
                list.add(up);
            }
            userPostRepository.saveAll(list);
        }
    }

    /**
     * 新增用户角色信息
     * 
     * @param userId 用户ID
     * @param roleIds 角色组
     */
    public void insertUserRole(Long userId, Long[] roleIds)
    {
        if (StringUtils.isNotEmpty(roleIds))
        {
            // 新增用户与角色管理
            List<SysUserRole> list = new ArrayList<SysUserRole>();
            for (Long roleId : roleIds)
            {
                SysUserRole ur = new SysUserRole();
                ur.setUserId(userId);
                ur.setRoleId(roleId);
                list.add(ur);
            }
            userRoleRepository.saveAll(list);
        }
    }

    /**
     * 通过用户ID删除用户
     * 
     * @param userId 用户ID
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteUserById(Long userId)
    {
        // 删除用户与角色关联
        userRoleRepository.deleteByUserId(userId);
        // 删除用户与岗位表
        userPostRepository.deleteByUserId(userId);
        return userRepository.softDeleteById(userId);
    }

    /**
     * 批量删除用户信息
     * 
     * @param userIds 需要删除的用户ID
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteUserByIds(Long[] userIds)
    {
        for (Long userId : userIds)
        {
            checkUserAllowed(new SysUser(userId));
            checkUserDataScope(userId);
        }
        // 删除用户与角色关联
        List<Long> userIdList = java.util.Arrays.asList(userIds);
        userRoleRepository.deleteByUserIdIn(userIdList);
        // 删除用户与岗位关联
        userPostRepository.deleteByUserIdIn(userIdList);
        userRepository.softDeleteByIds(userIdList);
        return userIds.length;
    }

    /**
     * 导入用户数据
     * 
     * @param userList 用户数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    @Override
    public String importUser(List<SysUser> userList, Boolean isUpdateSupport, String operName)
    {
        if (StringUtils.isNull(userList) || userList.size() == 0)
        {
            throw new ServiceException("导入用户数据不能为空！");
        }
        int successNum = 0;
        int failureNum = 0;
        StringBuilder successMsg = new StringBuilder();
        StringBuilder failureMsg = new StringBuilder();
        for (SysUser user : userList)
        {
            try
            {
                // 验证是否存在这个用户
                SysUser u = userRepository.findByUserNameAndDelFlag(user.getUserName(), "0").orElse(null);
                if (StringUtils.isNull(u))
                {
                    BeanValidators.validateWithException(validator, user);
                    deptService.checkDeptDataScope(user.getDeptId());
                    String password = configService.selectConfigByKey("sys.user.initPassword");
                    user.setPassword(SecurityUtils.encryptPassword(password));
                    user.setCreateBy(operName);
                    userRepository.save(user);
                    successNum++;
                    successMsg.append("<br/>" + successNum + "、账号 " + user.getUserName() + " 导入成功");
                }
                else if (isUpdateSupport)
                {
                    BeanValidators.validateWithException(validator, user);
                    checkUserAllowed(u);
                    checkUserDataScope(u.getUserId());
                    deptService.checkDeptDataScope(user.getDeptId());
                    user.setUserId(u.getUserId());
                    user.setDeptId(u.getDeptId());
                    user.setUpdateBy(operName);
                    userRepository.save(user);
                    successNum++;
                    successMsg.append("<br/>" + successNum + "、账号 " + user.getUserName() + " 更新成功");
                }
                else
                {
                    failureNum++;
                    failureMsg.append("<br/>" + failureNum + "、账号 " + user.getUserName() + " 已存在");
                }
            }
            catch (Exception e)
            {
                failureNum++;
                String msg = "<br/>" + failureNum + "、账号 " + user.getUserName() + " 导入失败：";
                failureMsg.append(msg + e.getMessage());
                log.error(msg, e);
            }
        }
        if (failureNum > 0)
        {
            failureMsg.insert(0, "很抱歉，导入失败！共 " + failureNum + " 条数据格式不正确，错误如下：");
            throw new ServiceException(failureMsg.toString());
        }
        else
        {
            successMsg.insert(0, "恭喜您，数据已全部导入成功！共 " + successNum + " 条，数据如下：");
        }
        return successMsg.toString();
    }

}
