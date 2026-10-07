package com.scaffold.gen.arch;

import com.scaffold.common.test.arch.ArchTestSupport;
import java.util.Set;

/**
 * gen 模块架构自检：端点权限注解扫描。
 * gen 的逆向查询为 JdbcTemplate 目录 SQL（GenCatalogDao），不经 mapper XML 门禁。
 *
 * @author ct
 */
class GenArchTest extends ArchTestSupport {
  @Override
  protected String basePackage() {
    return "com.scaffold.gen.api";
  }

  /** 存量只读端点（列信息查询，无写操作），后续权限收敛时再挂 @RequiresPermissions */
  @Override
  protected Set<String> permissionAllowlist() {
    return Set.of("GenResourceImpl#columnList");
  }


  /** 方言变体语句（GenTableMapper.xml / GenTableColumnMapper.xml 内）设计内使用的方言构造 */
}
