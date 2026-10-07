package com.scaffold.audit.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.scaffold.common.core.jpa.JpaSpecs;
import com.scaffold.common.test.H2JpaTest;
import com.scaffold.system.api.domain.SysLogininfor;
import com.scaffold.system.api.domain.SysOperLog;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

/**
 * 操作/登录日志数据访问切片测试（JPA + H2 Oracle 模式）。
 * 覆盖：仅追加写入、JpaSpecs 动态筛选、登录失败聚合 top-N、权限变更时间线、
 * 跨域只读快照（sys_role/sys_config 原生查询投影）。
 *
 * @author ct
 */
@H2JpaTest(
    ddl = "sql/audit/h2.sql",
    entityPackages = {"com.scaffold.audit.domain", "com.scaffold.system.api.domain"})
class AuditLogRepositoryTest {

  @Nested
  @DisplayName("OperLogRepository 写入与筛选")
  class OperLog {
    @Test
    @DisplayName("写入操作日志后按条件可查询到（isNew 恒 persist）")
    void insert_and_query(OperLogRepository repository) {
      SysOperLog operLog = new SysOperLog();
      operLog.setOperId(990001L);
      operLog.setTitle("导出用户");
      operLog.setOperName("admin");
      operLog.setStatus(0);
      operLog.setEventType("EXPORT");
      repository.save(operLog);

      Specification<Object> spec =
          JpaSpecs.eqIf("eventType", "EXPORT").and(JpaSpecs.eqIf("operId", 990001L));
      List<SysOperLog> list = repository.list(spec, Sort.unsorted());
      assertEquals(1, list.size());
      assertEquals(990001L, list.get(0).getOperId().longValue());
      assertEquals("导出用户", list.get(0).getTitle());
    }

    @Test
    @DisplayName("动态筛选：operName/title/bizKey 模糊、eventType 精确")
    void filters(OperLogRepository repository) {
      assertEquals(4, repository.list(JpaSpecs.likeIf("operName", "admin"), Sort.unsorted()).size());
      assertEquals(1, repository.list(JpaSpecs.likeIf("title", "登录"), Sort.unsorted()).size());
      assertEquals(
          1, repository.list(JpaSpecs.eqIf("eventType", "ROLE_ASSIGN"), Sort.unsorted()).size());
      assertEquals(3, repository.list(JpaSpecs.likeIf("bizKey", "user:"), Sort.unsorted()).size());
      assertTrue(repository.list(JpaSpecs.alwaysTrue(), Sort.unsorted()).size() >= 4);
    }

    @Test
    @DisplayName("权限变更时间线：userId=1 命中两条，userId=99 为空")
    void permissionTimeline(OperLogRepository repository) {
      assertEquals(2, repository.selectPermissionChanges(1L, "user:1%").size());
      assertTrue(repository.selectPermissionChanges(99L, "user:99%").isEmpty());
    }
  }

  @Nested
  @DisplayName("LogininforRepository 写入、筛选与失败聚合")
  class Logininfor {
    @Test
    @DisplayName("写入登录日志后按条件可查询到")
    void insert_and_query(LogininforRepository repository) {
      SysLogininfor logininfor = new SysLogininfor();
      logininfor.setInfoId(990002L);
      logininfor.setUserName("tester");
      logininfor.setIpaddr("192.168.1.1");
      logininfor.setStatus("0");
      logininfor.setMsg("登录成功");
      repository.save(logininfor);

      Specification<Object> spec =
          JpaSpecs.likeIf("userName", "tester").and(JpaSpecs.eqIf("ipaddr", "192.168.1.1"));
      List<SysLogininfor> list = repository.list(spec, Sort.by(Sort.Direction.DESC, "infoId"));
      assertEquals(1, list.size());
      assertEquals(990002L, list.get(0).getInfoId().longValue());
    }

    @Test
    @DisplayName("动态筛选：userName 模糊、status 精确")
    void filters(LogininforRepository repository) {
      assertEquals(2, repository.list(JpaSpecs.likeIf("userName", "admin"), Sort.unsorted()).size());
      List<SysLogininfor> failed =
          repository.list(JpaSpecs.eqIf("status", "1"), Sort.unsorted());
      assertEquals(1, failed.size());
      assertEquals("admin", failed.get(0).getUserName());
      assertEquals(1, repository.list(JpaSpecs.likeIf("ipaddr", "10.0.0"), Sort.unsorted()).size());
    }

    @Test
    @DisplayName("登录失败 top10：聚合命中种子失败记录")
    void loginFailTop10(LogininforRepository repository) {
      List<LogininforRepository.LoginFailStat> stats =
          repository.selectLoginFailTop10(new Date(0L), PageRequest.of(0, 10));
      assertNotNull(stats);
      assertEquals(1, stats.size());
      assertEquals("admin", stats.get(0).getUsername());
      assertEquals("127.0.0.1", stats.get(0).getIp());
      assertEquals(Long.valueOf(1L), stats.get(0).getFailCount());
      assertNotNull(stats.get(0).getLastFailTime());
    }
  }

  @Nested
  @DisplayName("SecuritySnapshotRepository 跨域只读快照")
  class Snapshot {
    @Test
    @DisplayName("userId=1：返回两个有效角色（排除已删除）")
    void userRoles(SecuritySnapshotRepository repository) {
      List<SecuritySnapshotRepository.UserRoleStat> roles = repository.selectUserRoles(1L);
      assertEquals(2, roles.size());
      assertTrue(roles.stream().anyMatch(r -> "admin".equals(r.getRoleKey())));
      assertTrue(roles.stream().noneMatch(r -> "deleted".equals(r.getRoleKey())));
    }

    @Test
    @DisplayName("安全配置快照：返回两条密码策略")
    void securityConfig(SecuritySnapshotRepository repository) {
      List<SecuritySnapshotRepository.SecurityConfigItem> configs = repository.selectSecurityConfig();
      assertEquals(2, configs.size());
      assertNotNull(configs.get(0).getConfigValue());
      assertNotNull(configs.get(0).getConfigKey());
    }
  }
}
