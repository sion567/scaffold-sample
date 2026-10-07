package com.scaffold.system.api.impl;

import java.io.ByteArrayInputStream;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.apache.dubbo.config.annotation.DubboService;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import java.util.Base64;
import com.scaffold.common.core.annotation.Excel;
import com.scaffold.common.core.text.Convert;
import com.scaffold.common.core.utils.DateUtils;
import com.scaffold.common.core.utils.PageUtils;
import com.scaffold.common.core.utils.StringUtils;
import com.scaffold.common.core.utils.poi.ExcelUtil;
import com.scaffold.common.core.web.controller.BaseController;
import com.scaffold.common.core.web.domain.AjaxResult;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import com.scaffold.common.core.web.page.PageDomain;
import com.scaffold.common.core.web.page.TableDataInfo;
import com.scaffold.common.log.annotation.Log;
import com.scaffold.common.log.enums.BusinessType;
import com.scaffold.common.security.annotation.RequiresPermissions;
import com.scaffold.common.security.service.TokenService;
import com.scaffold.common.security.utils.SecurityUtils;
import com.scaffold.system.api.SysUserResource;
import com.scaffold.system.api.domain.SysDept;
import com.scaffold.system.api.domain.SysRole;
import com.scaffold.system.api.domain.SysUser;
import com.scaffold.system.api.model.LoginUser;
import com.scaffold.system.service.ISysConfigService;
import com.scaffold.system.service.ISysDeptService;
import com.scaffold.system.service.ISysPasswordPolicyService;
import com.scaffold.system.service.ISysPermissionService;
import com.scaffold.system.service.ISysPostService;
import com.scaffold.system.service.ISysRoleService;
import com.scaffold.system.service.ISysUserPasswordHistoryService;
import com.scaffold.system.service.ISysUserService;

/**
 * 用户管理实现（Triple REST）
 *
 * @author scaffold
 */
@DubboService(protocol = "tri")
public class SysUserResourceImpl extends BaseController implements SysUserResource
{
    private final ISysUserService userService;
    private final ISysRoleService roleService;
    private final ISysDeptService deptService;
    private final ISysPostService postService;
    private final ISysPermissionService permissionService;
    private final ISysConfigService configService;
    private final ISysPasswordPolicyService passwordPolicyService;
    private final ISysUserPasswordHistoryService passwordHistoryService;
    private final TokenService tokenService;
    private final com.scaffold.common.redis.service.RedisService redisService;

    public SysUserResourceImpl(ISysUserService userService, ISysRoleService roleService, ISysDeptService deptService,
                               ISysPostService postService, ISysPermissionService permissionService,
                               ISysConfigService configService, ISysPasswordPolicyService passwordPolicyService,
                               ISysUserPasswordHistoryService passwordHistoryService, TokenService tokenService,
                               com.scaffold.common.redis.service.RedisService redisService)
    {
        this.userService = userService;
        this.roleService = roleService;
        this.deptService = deptService;
        this.postService = postService;
        this.permissionService = permissionService;
        this.configService = configService;
        this.passwordPolicyService = passwordPolicyService;
        this.passwordHistoryService = passwordHistoryService;
        this.tokenService = tokenService;
        this.redisService = redisService;
    }

    @RequiresPermissions("system:user:list")
    @Override
    public TableDataInfo list(Integer pageNum, Integer pageSize, String orderByColumn, String isAsc,
                              Long userId, String userName, String phonenumber, String status, Long deptId)
    {
        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setUserName(userName);
        user.setPhonenumber(phonenumber);
        user.setStatus(status);
        user.setDeptId(deptId);
        PageDomain pageDomain = new PageDomain();
        pageDomain.setPageNum(pageNum);
        pageDomain.setPageSize(pageSize);
        pageDomain.setOrderByColumn(orderByColumn);
        pageDomain.setIsAsc(isAsc);
        return userService.selectUserPage(user, pageDomain);
    }

    @Log(title = "用户管理", businessType = BusinessType.EXPORT)
    @RequiresPermissions("system:user:export")
    @Override
    public byte[] export(Long userId, String userName, String phonenumber, String status, Long deptId)
    {
        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setUserName(userName);
        user.setPhonenumber(phonenumber);
        user.setStatus(status);
        user.setDeptId(deptId);
        List<SysUser> list = userService.selectUserList(user);
        ExcelUtil<SysUser> util = new ExcelUtil<SysUser>(SysUser.class);
        return util.exportExcel(list, "用户数据");
    }

    @Log(title = "用户管理", businessType = BusinessType.IMPORT)
    @RequiresPermissions("system:user:import")
    @Override
    public AjaxResult importData(String fileBase64, boolean updateSupport) throws Exception
    {
        byte[] bytes = Base64.getDecoder().decode(fileBase64);
        ExcelUtil<SysUser> util = new ExcelUtil<SysUser>(SysUser.class);
        List<SysUser> userList = util.importExcel(new ByteArrayInputStream(bytes));
        String operName = SecurityUtils.getUsername();
        String message = userService.importUser(userList, updateSupport, operName);
        return success(message);
    }

    @Override
    public byte[] importTemplate()
    {
        ExcelUtil<SysUser> util = new ExcelUtil<SysUser>(SysUser.class);
        util.init(null, "用户数据", StringUtils.EMPTY, Excel.Type.IMPORT);
        try
        {
            util.writeSheet();
            // Use reflection to access private wb field
            SXSSFWorkbook wb = (SXSSFWorkbook) FieldUtils.getDeclaredField(ExcelUtil.class, "wb", true).get(util);
            try (java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream())
            {
                wb.write(out);
                return out.toByteArray();
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException("生成导入模板失败", e);
        }
    }

    @Override
    public AjaxResult getInfo()
    {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (StringUtils.isNull(loginUser))
        {
            return error("登录状态已过期，请重新登录");
        }
        SysUser user = loginUser.getSysUser();
        // 角色集合
        Set<String> roles = permissionService.getRolePermission(user);
        // 权限集合
        Set<String> permissions = permissionService.getMenuPermission(user);
        if (!loginUser.getPermissions().equals(permissions))
        {
            loginUser.setPermissions(permissions);
            tokenService.refreshToken(loginUser);
        }
        AjaxResult ajax = AjaxResult.success();
        ajax.put("user", user);
        ajax.put("roles", roles);
        ajax.put("permissions", permissions);
        ajax.put("pwdChrtype", getSysAccountChrtype());
        ajax.put("isDefaultModifyPwd", initPasswordIsModify(user.getPwdUpdateDate()));
        ajax.put("isPasswordExpired", passwordIsExpiration(user.getPwdUpdateDate()));
        return ajax;
    }

    /**
     * 获取用户密码自定义配置规则
     */
    public String getSysAccountChrtype()
    {
        return Convert.toStr(configService.selectConfigByKey("sys.account.chrtype"), "0");
    }

    /**
     * 检查初始密码是否提醒修改
     */
    public boolean initPasswordIsModify(Date pwdUpdateDate)
    {
        Integer initPasswordModify = Convert.toInt(configService.selectConfigByKey("sys.account.initPasswordModify"));
        return initPasswordModify != null && initPasswordModify == 1 && pwdUpdateDate == null;
    }

    /**
     * 检查密码是否过期
     */
    public boolean passwordIsExpiration(Date pwdUpdateDate)
    {
        // 等保权威键 sys.account.password.maxAge；兼容旧键 sys.account.passwordValidateDays（V10 已同步值）
        Integer passwordValidateDays = Convert.toInt(configService.selectConfigByKey("sys.account.password.maxAge"));
        if (passwordValidateDays == null || passwordValidateDays <= 0)
        {
            passwordValidateDays = Convert.toInt(configService.selectConfigByKey("sys.account.passwordValidateDays"));
        }
        if (passwordValidateDays != null && passwordValidateDays > 0)
        {
            if (StringUtils.isNull(pwdUpdateDate))
            {
                // 如果从未修改过初始密码，直接提醒过期
                return true;
            }
            Date nowDate = DateUtils.getNowDate();
            return DateUtils.differentDaysByMillisecond(nowDate, pwdUpdateDate) > passwordValidateDays;
        }
        return false;
    }

    @RequiresPermissions("system:user:query")
    @Override
    public AjaxResult getUserInfo(@PathVariable("userId") Long userId)
    {
        AjaxResult ajax = AjaxResult.success();
        if (StringUtils.isNotNull(userId))
        {
            userService.checkUserDataScope(userId);
            SysUser sysUser = userService.selectUserById(userId);
            ajax.put(AjaxResult.DATA_TAG, sysUser);
            ajax.put("postIds", postService.selectPostListByUserId(userId));
            ajax.put("roleIds", sysUser.getRoles().stream().map(SysRole::getRoleId).collect(Collectors.toList()));
        }
        List<SysRole> roles = roleService.selectRoleAll();
        ajax.put("roles", SecurityUtils.isAdmin(userId) ? roles : roles.stream().filter(r -> !r.isAdmin()).collect(Collectors.toList()));
        ajax.put("posts", postService.selectPostAll());
        return ajax;
    }

    @RequiresPermissions("system:user:add")
    @Log(title = "用户管理", businessType = BusinessType.INSERT)
    @Override
    public AjaxResult add(SysUser user)
    {
        deptService.checkDeptDataScope(user.getDeptId());
        roleService.checkRoleDataScope(user.getRoleIds());
        if (!userService.checkUserNameUnique(user))
        {
            return error("新增用户'" + user.getUserName() + "'失败，登录账号已存在");
        }
        else if (StringUtils.isNotEmpty(user.getPhonenumber()) && !userService.checkPhoneUnique(user))
        {
            return error("新增用户'" + user.getUserName() + "'失败，手机号码已存在");
        }
        else if (StringUtils.isNotEmpty(user.getEmail()) && !userService.checkEmailUnique(user))
        {
            return error("新增用户'" + user.getUserName() + "'失败，邮箱账号已存在");
        }
        // 密码策略校验（sys.account.password.minLength / complexity / customRules 配置）
        List<String> policyMessages = passwordPolicyService.validate(user.getPassword(), user.getUserName());
        if (!policyMessages.isEmpty())
        {
            return error(String.join("；", policyMessages));
        }
        user.setCreateBy(SecurityUtils.getUsername());
        user.setPassword(SecurityUtils.encryptPassword(user.getPassword()));
        return toAjax(userService.insertUser(user));
    }

    @RequiresPermissions("system:user:edit")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    @Override
    public AjaxResult edit(SysUser user)
    {
        userService.checkUserAllowed(user);
        userService.checkUserDataScope(user.getUserId());
        deptService.checkDeptDataScope(user.getDeptId());
        roleService.checkRoleDataScope(user.getRoleIds());
        if (!userService.checkUserNameUnique(user))
        {
            return error("修改用户'" + user.getUserName() + "'失败，登录账号已存在");
        }
        else if (StringUtils.isNotEmpty(user.getPhonenumber()) && !userService.checkPhoneUnique(user))
        {
            return error("修改用户'" + user.getUserName() + "'失败，手机号码已存在");
        }
        else if (StringUtils.isNotEmpty(user.getEmail()) && !userService.checkEmailUnique(user))
        {
            return error("修改用户'" + user.getUserName() + "'失败，邮箱账号已存在");
        }
        user.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(userService.updateUser(user));
    }

    @RequiresPermissions("system:user:remove")
    @Log(title = "用户管理", businessType = BusinessType.DELETE)
    @Override
    public AjaxResult remove(@PathVariable("userIds") String userIds)
    {
        Long[] userIdArray = Convert.toLongArray(userIds);
        if (ArrayUtils.contains(userIdArray, SecurityUtils.getUserId()))
        {
            return error("当前用户不能删除");
        }
        return toAjax(userService.deleteUserByIds(userIdArray));
    }

    @RequiresPermissions("system:user:edit")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    @Override
    public AjaxResult resetPwd(Long userId, String password)
    {
        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setPassword(password);
        userService.checkUserAllowed(user);
        userService.checkUserDataScope(user.getUserId());
        // 密码策略校验（sys.account.password.minLength / complexity / customRules 配置）
        List<String> policyMessages = passwordPolicyService.validate(password, null);
        if (!policyMessages.isEmpty())
        {
            return error(String.join("；", policyMessages));
        }
        // 密码历史校验：读 sys.account.password.history → 查该用户最近 N 条历史 → matchesPassword 逐条比对
        int historyCount = Convert.toInt(configService.selectConfigByKey("sys.account.password.history"), 5);
        if (passwordHistoryService.matchesRecentHistory(password, userId, historyCount))
        {
            return error("新密码不能与最近 " + historyCount + " 次使用过的密码相同");
        }
        String newPassword = SecurityUtils.encryptPassword(password);
        user.setPassword(newPassword);
        user.setUpdateBy(SecurityUtils.getUsername());
        // 同一事务：改密 + 记录历史 + 清理超限历史（防止用户后续改回管理员重置的密码）
        int result = userService.resetUserPwdWithHistory(user.getUserId(), newPassword, historyCount);
        return toAjax(result);
    }

    @RequiresPermissions("system:user:edit")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    @Override
    public AjaxResult changeStatus(Long userId, String status)
    {
        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setStatus(status);
        userService.checkUserAllowed(user);
        userService.checkUserDataScope(user.getUserId());
        user.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(userService.updateUserStatus(user));
    }

    @RequiresPermissions("system:user:query")
    @Override
    public AjaxResult authRole(@PathVariable("userId") Long userId)
    {
        AjaxResult ajax = AjaxResult.success();
        SysUser user = userService.selectUserById(userId);
        List<SysRole> roles = roleService.selectRolesByUserId(userId);
        ajax.put("user", user);
        ajax.put("roles", SecurityUtils.isAdmin(userId) ? roles : roles.stream().filter(r -> !r.isAdmin()).collect(Collectors.toList()));
        return ajax;
    }

    @RequiresPermissions("system:user:edit")
    @Log(title = "用户管理", businessType = BusinessType.GRANT)
    @Override
    public AjaxResult insertAuthRole(Long userId, String roleIds)
    {
        Long[] roleIdArray = Convert.toLongArray(roleIds);
        userService.checkUserDataScope(userId);
        roleService.checkRoleDataScope(roleIdArray);
        userService.insertUserAuth(userId, roleIdArray);
        return success();
    }

    @RequiresPermissions("system:user:list")
    @Override
    public AjaxResult deptTree(String deptName, String status)
    {
        SysDept dept = new SysDept();
        dept.setDeptName(deptName);
        dept.setStatus(status);
        return success(deptService.selectDeptTreeList(dept));
    }

    @RequiresPermissions("system:logininfor:unlock")
    @Log(title = "账户解锁", businessType = BusinessType.OTHER)
    @Override
    public AjaxResult unlock(String userName)
    {
        redisService.deleteObject(com.scaffold.common.core.constant.CacheConstants.PWD_ERR_CNT_KEY + userName);
        return success();
    }
}
