package com.scaffold.system.arch;

import com.scaffold.common.test.arch.ArchTestSupport;
import java.util.Set;

/**
 * system 模块架构自检：端点权限注解全量扫描。
 * 新增端点未挂 @RequiresPermissions（且不在免鉴权白名单）直接红。
 *
 * @author ct
 */
class SystemArchTest extends ArchTestSupport {
  @Override
  protected String basePackage() {
    return "com.scaffold.system.api";
  }


  /**
   * 存量免权限端点登记（RuoYi 传统：当前用户自有数据端点与下拉数据源，依赖 JWT 登录态，
   * 不挂细粒度权限串）。新代码请勿向此清单追加——能挂 {@code @RequiresPermissions} 的照常挂。
   */
  @Override
  protected Set<String> permissionAllowlist() {
    return Set.of(
        "SysConfigResourceImpl#getConfigKey",
        "SysConfigResourceImpl#getInfo",
        "SysDictDataResourceImpl#dictType",
        "SysDictTypeResourceImpl#optionselect",
        "SysMenuResourceImpl#getRouters",
        "SysMenuResourceImpl#roleMenuTreeselect",
        "SysMenuResourceImpl#treeselect",
        "SysNoticeResourceImpl#getInfo",
        "SysNoticeResourceImpl#listTop",
        "SysNoticeResourceImpl#markRead",
        "SysNoticeResourceImpl#markReadAll",
        "SysPostResourceImpl#optionselect",
        "SysProfileResourceImpl#avatar",
        "SysProfileResourceImpl#profile",
        "SysProfileResourceImpl#updateProfile",
        "SysProfileResourceImpl#updatePwd",
        "SysUserResourceImpl#getInfo",
        "SysUserResourceImpl#importTemplate");
  }
}
