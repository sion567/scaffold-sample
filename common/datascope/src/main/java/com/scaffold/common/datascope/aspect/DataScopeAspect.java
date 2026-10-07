package com.scaffold.common.datascope.aspect;

import java.util.ArrayList;
import java.util.List;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import com.scaffold.common.core.constant.Constants;
import com.scaffold.common.core.constant.UserConstants;
import com.scaffold.common.core.context.SecurityContextHolder;
import com.scaffold.common.core.jpa.DataScopeContext;
import com.scaffold.common.core.text.Convert;
import com.scaffold.common.core.utils.StringUtils;
import com.scaffold.common.datascope.annotation.DataScope;
import com.scaffold.common.security.utils.SecurityUtils;
import com.scaffold.system.api.domain.SysDept;
import com.scaffold.system.api.domain.SysRole;
import com.scaffold.system.api.domain.SysRoleDept;
import com.scaffold.system.api.domain.SysUser;
import com.scaffold.system.api.model.LoginUser;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

/**
 * 数据过滤处理（JPA Specification 形态）。
 *
 * <p>原实现把 `${params.dataScope}` SQL 片段塞进实体的 params；本实现把按角色解析出的
 * {@link org.springframework.data.jpa.domain.Specification} 放入 {@link DataScopeContext}
 * 线程上下文，由 {@code ScaffoldServiceImpl#dataScope()} 在查询时取用并叠加，
 * 方法返回后清理——SQL 注入面（字段名拼接）随动态 SQL 一起消失，
 * 字段名经 Criteria 属性解析（不存在即抛错），注解里的属性名仍做安全校验兜底。</p>
 *
 * <p>范围语义与原版一一对应：ALL 不过滤；CUSTOM 按 sys_role_dept 子查询；
 * DEPT 本部门；DEPT_AND_CHILD 本部门及 ancestors 链上部门（用 concat+LIKE 替代
 * find_in_set，跨方言可移植）；SELF 仅本人（无 userField 时退化为恒假）。</p>
 *
 * @author ct
 */
@Aspect
@Component
public class DataScopeAspect
{
    /**
     * 数据权限过滤关键字（兼容保留常量名）
     */
    public static final String DATA_SCOPE = "dataScope";

    @Around("@annotation(controllerDataScope)")
    public Object doAround(ProceedingJoinPoint point, DataScope controllerDataScope) throws Throwable
    {
        try
        {
            handleDataScope(controllerDataScope);
            return point.proceed();
        }
        finally
        {
            DataScopeContext.clear();
        }
    }

    /**
     * 属性名安全校验（P2-7）：userField/deptField 来自注解声明，作为 Criteria 属性名使用，
     * 非法标识符属配置错误，fail-fast 暴露而不是等属性解析报错。
     */
    static boolean isSafeIdentifier(String identifier)
    {
        if (StringUtils.isEmpty(identifier))
        {
            return true;
        }
        return identifier.matches("[A-Za-z_][A-Za-z0-9_]{0,63}");
    }

    protected void handleDataScope(DataScope controllerDataScope)
    {
        // 获取当前的用户
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (StringUtils.isNotNull(loginUser))
        {
            SysUser currentUser = loginUser.getSysUser();
            // 如果是超级管理员，则不过滤数据
            if (StringUtils.isNotNull(currentUser) && !currentUser.isAdmin())
            {
                String permission = StringUtils.defaultIfEmpty(controllerDataScope.permission(), SecurityContextHolder.getPermission());
                DataScopeContext.hold(buildSpecification(currentUser, controllerDataScope, permission));
            }
        }
    }

    /**
     * 按角色解析数据范围为 Specification（OR 叠加各角色范围；ALL 短路为恒真）。
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private <T> org.springframework.data.jpa.domain.Specification<T> buildSpecification(
            SysUser user, DataScope controllerDataScope, String permission)
    {
        String deptField = controllerDataScope.deptField();
        String userField = controllerDataScope.userField();
        if (!isSafeIdentifier(deptField) || !isSafeIdentifier(userField))
        {
            throw new IllegalArgumentException("DataScope 注解含非法属性名: deptField=" + deptField + ", userField=" + userField);
        }
        List<String> conditions = new ArrayList<>();
        List<String> scopeCustomIds = new ArrayList<>();
        user.getRoles().forEach(role -> {
            if (Constants.Dept.DATA_SCOPE_CUSTOM.equals(role.getDataScope())
                    && StringUtils.equals(role.getStatus(), UserConstants.ROLE_NORMAL)
                    && (StringUtils.isEmpty(permission) || StringUtils.containsAny(role.getPermissions(), Convert.toStrArray(permission))))
            {
                scopeCustomIds.add(Convert.toStr(role.getRoleId()));
            }
        });

        List<org.springframework.data.jpa.domain.Specification<T>> parts = new ArrayList<>();
        for (SysRole role : user.getRoles())
        {
            String dataScope = role.getDataScope();
            if (conditions.contains(dataScope) || StringUtils.equals(role.getStatus(), UserConstants.ROLE_DISABLE))
            {
                continue;
            }
            if (StringUtils.isNotEmpty(permission) && !StringUtils.containsAny(role.getPermissions(), Convert.toStrArray(permission)))
            {
                continue;
            }
            if (Constants.Dept.DATA_SCOPE_ALL.equals(dataScope))
            {
                parts.clear();
                conditions.add(dataScope);
                break; // 全部数据权限：不过滤
            }
            else if (Constants.Dept.DATA_SCOPE_CUSTOM.equals(dataScope))
            {
                parts.add(customScope(deptField, scopeCustomIds.size() > 1 ? null : role.getRoleId(), scopeCustomIds));
            }
            else if (Constants.Dept.DATA_SCOPE_DEPT.equals(dataScope))
            {
                parts.add((root, query, cb) -> cb.equal(root.get(deptField), user.getDeptId()));
            }
            else if (Constants.Dept.DATA_SCOPE_DEPT_AND_CHILD.equals(dataScope))
            {
                parts.add(deptAndChildScope(deptField, user.getDeptId()));
            }
            else if (Constants.Dept.DATA_SCOPE_SELF.equals(dataScope))
            {
                if (StringUtils.isNotBlank(userField))
                {
                    parts.add((root, query, cb) -> cb.equal(root.get(userField), user.getUserId()));
                }
                else
                {
                    // 数据权限为仅本人且没有 userField 属性不查询任何数据
                    parts.add((root, query, cb) -> cb.equal(root.get(deptField), 0L));
                }
            }
            conditions.add(dataScope);
        }

        // 角色都不包含传递过来的权限字符，限制为不查询任何数据
        if (StringUtils.isEmpty(conditions))
        {
            return (root, query, cb) -> cb.equal(root.get(deptField), 0L);
        }
        if (parts.isEmpty())
        {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.or(parts.stream().map(s -> s.toPredicate(root, query, cb)).toArray(Predicate[]::new));
    }

    /** CUSTOM：deptField IN (SELECT dept_id FROM sys_role_dept WHERE role_id = ? / IN (...)) */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static <T> org.springframework.data.jpa.domain.Specification<T> customScope(
            String deptField, Long singleRoleId, List<String> scopeCustomIds)
    {
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<SysRoleDept> rd = sub.from(SysRoleDept.class);
            sub.select(rd.get("deptId"));
            if (singleRoleId != null)
            {
                sub.where(cb.equal(rd.get("roleId"), singleRoleId));
            }
            else
            {
                List<Long> ids = new ArrayList<>();
                for (String id : scopeCustomIds)
                {
                    try { ids.add(Long.valueOf(id)); } catch (NumberFormatException ignored2) { /* 非法角色ID跳过 */ }
                }
                sub.where(rd.get("roleId").in(ids));
            }
            jakarta.persistence.criteria.CriteriaBuilder.In in = cb.in(root.get(deptField).as(Long.class));
            in.value(sub);
            return in;
        };
    }

    /** DEPT_AND_CHILD：deptField IN (SELECT dept_id FROM sys_dept WHERE dept_id = ? OR ancestors 链含 ?) */
    private static <T> org.springframework.data.jpa.domain.Specification<T> deptAndChildScope(String deptField, Long deptId)
    {
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<SysDept> d = sub.from(SysDept.class);
            sub.select(d.get("deptId"));
            // 等价 find_in_set(deptId, ancestors)：",0,100," LIKE "%,100,%"（跨方言 concat 可移植）
            sub.where(cb.or(
                    cb.equal(d.get("deptId"), deptId),
                    cb.like(cb.concat(cb.concat(",", d.get("ancestors")), ","), "%," + deptId + ",%")));
            jakarta.persistence.criteria.CriteriaBuilder.In in = cb.in(root.get(deptField).as(Long.class));
            in.value(sub);
            return in;
        };
    }
}
