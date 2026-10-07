package com.scaffold.audit.arch;

import com.scaffold.common.test.arch.ArchTestSupport;
import java.util.Set;

/**
 * 安全架构自检：audit 模块端点权限注解全量扫描。 新增端点未挂 @RequiresPermissions（且不在白名单）将直接红。
 *
 * @author ct
 */
class AuditArchTest extends ArchTestSupport {
  @Override
  protected String basePackage() {
    return "com.scaffold.audit.api";
  }

  @Override
  protected Set<String> permissionAllowlist() {
    return Set.of();
  }
}
