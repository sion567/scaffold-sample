package com.scaffold.job.arch;

import com.scaffold.common.test.arch.ArchTestSupport;
import java.util.Set;

/**
 * job 模块架构自检：端点权限注解全量扫描。
 * 新增端点未挂 @RequiresPermissions（且不在免鉴权白名单）直接红。
 *
 * @author ct
 */
class JobArchTest extends ArchTestSupport {
  @Override
  protected String basePackage() {
    return "com.scaffold.job.api";
  }

  @Override
  protected Set<String> permissionAllowlist() {
    return Set.of();
  }
}
