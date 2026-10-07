package com.scaffold.message.arch;

import com.scaffold.common.test.arch.ArchTestSupport;
import java.util.Set;

/**
 * message 模块架构自检：端点权限注解全量扫描。
 * 新增端点未挂 @RequiresPermissions（且在免鉴权白名单的除外）直接红。
 *
 * @author ct
 */
class MessageArchTest extends ArchTestSupport {
  @Override
  protected String basePackage() {
    return "com.scaffold.message.api";
  }


  /**
   * 存量免权限端点登记：站内信"我的消息"类（当前用户自有数据，依赖 JWT 登录态）与
   * 短信回执回调（第三方服务商回调，业务层自行验签）。新代码请勿向此清单追加。
   */
  @Override
  protected Set<String> permissionAllowlist() {
    return Set.of(
        "InnerMessageResourceImpl#read",
        "InnerMessageResourceImpl#mine",
        "InnerMessageResourceImpl#unreadCount",
        "InnerMessageResourceImpl#readAll",
        "SmsCallbackResourceImpl#receipt");
  }
}
