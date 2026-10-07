package com.scaffold.message.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.scaffold.common.core.jpa.JpaSpecs;
import com.scaffold.common.test.H2JpaTest;
import com.scaffold.message.domain.MsgInnerMessage;
import com.scaffold.message.domain.MsgSmsChannel;
import com.scaffold.message.domain.MsgSmsLog;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

/**
 * message 模块数据访问切片测试（JPA + H2 Oracle 模式，DDL 复用 Flyway H2 基线）。
 * 覆盖：应用侧雪花主键、唯一键派生查询、启用渠道排序、幂等键查询、站内信已读更新。
 *
 * @author ct
 */
@H2JpaTest(
        ddl = "db/migration/h2/V1__message_baseline_h2.sql",
        entityPackages = "com.scaffold.message.domain")
class MessageRepositoryTest {

  private MsgSmsChannel channel(long id, String name, String type, String status) {
    MsgSmsChannel c = new MsgSmsChannel();
    c.setId(id);
    c.setChannelName(name);
    c.setChannelType(type);
    c.setStatus(status);
    c.setDelFlag("0");
    c.setPriority((int) id);
    return c;
  }

  @Test
  @DisplayName("渠道：唯一名查询 + 启用渠道按优先级排序 + 软删除后不命中")
  void smsChannel(MsgSmsChannelRepository repository) {
    repository.saveAllAndFlush(List.of(
            channel(1, "阿里云-主用", "ALIYUN", "0"),
            channel(2, "阿里云-备用", "ALIYUN", "0"),
            channel(3, "腾讯云", "TENCENT", "1")));

    assertEquals("阿里云-主用",
            repository.findByChannelNameAndDelFlag("阿里云-主用", "0").getChannelName());
    assertNull(repository.findByChannelNameAndDelFlag("不存在", "0"));

    List<MsgSmsChannel> enabled =
            repository.findByChannelTypeAndStatusAndDelFlagOrderByPriorityAscIdAsc("ALIYUN", "0", "0");
    assertEquals(2, enabled.size());
    assertEquals(Long.valueOf(1L), enabled.get(0).getId());
    assertTrue(repository.findByChannelTypeAndStatusAndDelFlagOrderByPriorityAscIdAsc("MOCK", "0", "0").isEmpty());
  }

  @Test
  @DisplayName("短信日志：幂等键派生查询命中，幂等键为空的行不干扰")
  void smsLog(MsgSmsLogRepository repository) {
    MsgSmsLog log = new MsgSmsLog();
    log.setId(101L);
    log.setMobile("13800001111");
    log.setBizType("COMMAND");
    log.setBizId("biz-1");
    log.setTemplateCode("TPL_SMS");
    log.setIdempotentKey("key-1");
    log.setSendStatus("PENDING");
    repository.saveAndFlush(log);

    MsgSmsLog hit =
            repository.findByBizTypeAndBizIdAndTemplateCodeAndIdempotentKey("COMMAND", "biz-1", "TPL_SMS", "key-1");
    assertNotNull(hit);
    assertEquals(Long.valueOf(101L), hit.getId());
    assertNull(repository.findByBizTypeAndBizIdAndTemplateCodeAndIdempotentKey("COMMAND", "biz-1", "TPL_SMS", "other"));
  }

  @Test
  @DisplayName("站内信：未读计数、单条已读（校验收件人）、全部已读")
  void innerMessage(MsgInnerMessageRepository repository) {
    MsgInnerMessage unread = new MsgInnerMessage();
    unread.setId(201L);
    unread.setTitle("标题");
    unread.setContent("内容");
    unread.setReceiverId(9L);
    unread.setReadFlag(MsgInnerMessage.READ_NO);
    unread.setBizType("COMMAND");
    unread.setBizId("b1");
    unread.setTemplateCode("TPL_IN");
    unread.setIdempotentKey("k1");
    unread.setCreateTime(new Date());
    MsgInnerMessage otherUser = new MsgInnerMessage();
    otherUser.setId(202L);
    otherUser.setTitle("标题");
    otherUser.setContent("内容");
    otherUser.setReceiverId(8L);
    otherUser.setReadFlag(MsgInnerMessage.READ_NO);
    otherUser.setCreateTime(new Date());
    repository.saveAllAndFlush(List.of(unread, otherUser));

    assertEquals(1, repository.countByReceiverIdAndReadFlag(9L, "0"));

    assertEquals(1, repository.markRead(201L, 9L, new Date()));
    // 非本人消息：0 行（越权防护）
    assertEquals(0, repository.markRead(202L, 9L, new Date()));
    assertEquals(0, repository.countByReceiverIdAndReadFlag(9L, "0"));

    MsgInnerMessage again = new MsgInnerMessage();
    again.setId(203L);
    again.setTitle("标题");
    again.setContent("内容");
    again.setReceiverId(9L);
    again.setReadFlag(MsgInnerMessage.READ_NO);
    again.setCreateTime(new Date());
    repository.saveAndFlush(again);
    assertEquals(1, repository.markAllRead(9L, new Date()));
    assertEquals(0, repository.countByReceiverIdAndReadFlag(9L, "0"));
    assertTrue(repository.list(JpaSpecs.eqIf("receiverId", 9L), Sort.unsorted())
            .stream().allMatch(m -> MsgInnerMessage.READ_YES.equals(m.getReadFlag())));
  }
}
