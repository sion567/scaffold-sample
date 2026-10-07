package com.scaffold.common.datasource.dubbo;

import java.util.Set;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import com.scaffold.common.core.constant.SecurityConstants;
import com.scaffold.common.core.context.SecurityContextHolder;
import com.scaffold.common.core.utils.StringUtils;
import com.scaffold.system.api.model.LoginUser;

/**
 * 审核人强制只读过滤器（可选等保能力，未配置即关闭——
 * 仅当配置了 {@code scaffold.audit.readonly-ds} 时由 AuditReadOnlyAutoConfiguration 激活）。
 *
 * 职责：登录用户为审计管理员（aud_admin 角色或 aud_* 角色前缀，用户名 aud_* 前缀兜底）时，
 * 该次调用内的所有数据库操作强制切换到只读数据源，
 * 从数据库层杜绝审核人通过任何接口（含未加权限注解的写接口）执行写操作。
 *
 * 说明：
 * 1. 通过 Dubbo Filter SPI 注册（PROVIDER 端，order=100），必须晚于 SecurityContextFilter（填充 LoginUser 上下文）；
 * 2. 切换发生在业务方法调用前、Service 层事务开启前，@Transactional 连接按当前线程数据源获取，天然生效；
 * 3. 只读数据源不存在时，SQL 执行阶段抛 CannotFindDataSourceException（fail-closed，请求失败而非静默降级）。
 *
 * @author scaffold
 */
@Activate(group = CommonConstants.PROVIDER, order = 100)
public class AuditReadOnlyFilter implements Filter
{
    /** 审计管理员角色键（需在 sys_role 初始化数据中创建） */
    private static final String ROLE_AUDITOR = "aud_admin";
    /** 审计角色前缀（兼容方案 5.2 命名规范） */
    private static final String ROLE_PREFIX = "aud";

    /** 能力开关（默认关闭，配置 scaffold.audit.readonly-ds 后由自动配置打开） */
    private static volatile boolean enabled = false;

    /** 只读数据源 key（由 AuditReadOnlyAutoConfiguration 从配置 scaffold.audit.readonly-ds 注入） */
    private static volatile String readOnlyDs = "audit";

    /** 打开只读门控并设置只读数据源 key（仅自动配置在显式配置了该能力时调用） */
    public static void enable(String ds)
    {
        if (StringUtils.isNotEmpty(ds))
        {
            readOnlyDs = ds;
        }
        enabled = true;
    }

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException
    {
        if (!enabled)
        {
            return invoker.invoke(invocation);
        }
        LoginUser loginUser = SecurityContextHolder.get(SecurityConstants.LOGIN_USER, LoginUser.class);
        if (!isAuditor(loginUser))
        {
            return invoker.invoke(invocation);
        }
        DynamicDataSourceContextHolder.push(readOnlyDs);
        try
        {
            return invoker.invoke(invocation);
        }
        finally
        {
            DynamicDataSourceContextHolder.poll();
        }
    }

    /**
     * 是否审核人：角色集合包含 aud_admin，或含 aud_* 前缀角色；账号前缀 aud_* 兜底（兼容方案 5.2 命名规范）。
     */
    private boolean isAuditor(LoginUser loginUser)
    {
        if (loginUser == null)
        {
            return false;
        }
        Set<String> roles = loginUser.getRoles();
        if (roles != null)
        {
            if (roles.contains(ROLE_AUDITOR))
            {
                return true;
            }
            for (String role : roles)
            {
                if (StringUtils.isNotEmpty(role) && role.startsWith(ROLE_PREFIX + "_"))
                {
                    return true;
                }
            }
        }
        return StringUtils.isNotEmpty(loginUser.getUsername()) && loginUser.getUsername().startsWith(ROLE_PREFIX + "_");
    }
}
