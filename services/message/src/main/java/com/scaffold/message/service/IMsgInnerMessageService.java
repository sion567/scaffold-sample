package com.scaffold.message.service;

import com.scaffold.common.core.web.page.PageDomain;
import com.scaffold.common.core.web.page.TableDataInfo;
import com.scaffold.message.domain.MsgInnerMessage;
import java.util.List;
import java.util.Map;

/**
 * 站内信消息 服务层（用户端接口服务端强制 RECEIVER_ID = 当前登录人，不信任前端传参）
 *
 * @author ct
 */
public interface IMsgInnerMessageService {
  List<MsgInnerMessage> queryList(MsgInnerMessage query);

  /** 我的消息分页（readFlag 筛选） */
  List<MsgInnerMessage> queryMine(String readFlag);

  /**
   * 分页查询我的消息（JPA PageRequest 分页）
   */
  TableDataInfo queryMinePage(String readFlag, PageDomain page);

  /** 未读数（铃铛轮询） */
  int unreadCount();

  /** 标记已读（幂等） */
  int markRead(Long id);

  /** 全部已读 */
  int markAllRead();

  int deleteByIds(Long[] ids);

  /** 人工发送（管理端：模板+变量+接收范围，接收范围为用户 ID 列表） */
  List<Long> sendManual(
      String templateCode,
      Map<String, String> params,
      List<Long> receiverIds,
      String jumpUrl,
      String operator);

  /**
   * 分页查询（JPA PageRequest 分页，分页参数取自请求上下文）
   */
  TableDataInfo queryPage(MsgInnerMessage query, PageDomain page);
}
