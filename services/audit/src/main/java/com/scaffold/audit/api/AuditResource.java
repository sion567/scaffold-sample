package com.scaffold.audit.api;

import com.scaffold.common.core.web.domain.AjaxResult;
import com.scaffold.common.core.web.page.TableDataInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 审计读写资源（Triple REST 对外接口，审计域权威入口）
 *
 * <p>读：操作日志/登录日志统一从这里出（scaffold-audit 自有表 audit_oper_log/audit_logininfor）； 写：经
 * RemoteLogService(Dubbo/protobuf) 落库，仅追加，无删除/清空接口（等保 8.1.4 e）。
 *
 * @author ct
 */
@RequestMapping("/audit")
public interface AuditResource {
  /** 操作日志查询 */
  @GetMapping("/oper-log")
  TableDataInfo operLog(
      @RequestParam(value = "pageNum", required = false) Integer pageNum,
      @RequestParam(value = "pageSize", required = false) Integer pageSize,
      @RequestParam(value = "orderByColumn", required = false) String orderByColumn,
      @RequestParam(value = "isAsc", required = false) String isAsc,
      @RequestParam(value = "title", required = false) String title,
      @RequestParam(value = "businessType", required = false) Integer businessType,
      @RequestParam(value = "status", required = false) Integer status,
      @RequestParam(value = "operName", required = false) String operName,
      @RequestParam(value = "operIp", required = false) String operIp,
      @RequestParam(value = "eventType", required = false) String eventType,
      @RequestParam(value = "bizType", required = false) String bizType,
      @RequestParam(value = "bizKey", required = false) String bizKey,
      @RequestParam(value = "beginTime", required = false) String beginTime,
      @RequestParam(value = "endTime", required = false) String endTime);

  /** 导出操作日志列表 */
  @PostMapping("/oper-log/export")
  @RequestMapping(
      value = "/oper-log/export",
      produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
  byte[] exportOperLog(
      @RequestParam(value = "title", required = false) String title,
      @RequestParam(value = "businessType", required = false) Integer businessType,
      @RequestParam(value = "status", required = false) Integer status,
      @RequestParam(value = "operName", required = false) String operName,
      @RequestParam(value = "operIp", required = false) String operIp,
      @RequestParam(value = "eventType", required = false) String eventType,
      @RequestParam(value = "bizType", required = false) String bizType,
      @RequestParam(value = "bizKey", required = false) String bizKey,
      @RequestParam(value = "beginTime", required = false) String beginTime,
      @RequestParam(value = "endTime", required = false) String endTime);

  /** 登录日志查询 */
  @GetMapping("/logininfor")
  TableDataInfo logininfor(
      @RequestParam(value = "pageNum", required = false) Integer pageNum,
      @RequestParam(value = "pageSize", required = false) Integer pageSize,
      @RequestParam(value = "orderByColumn", required = false) String orderByColumn,
      @RequestParam(value = "isAsc", required = false) String isAsc,
      @RequestParam(value = "userName", required = false) String userName,
      @RequestParam(value = "ipaddr", required = false) String ipaddr,
      @RequestParam(value = "status", required = false) String status,
      @RequestParam(value = "beginTime", required = false) String beginTime,
      @RequestParam(value = "endTime", required = false) String endTime);

  /** 导出登录日志列表 */
  @PostMapping("/logininfor/export")
  @RequestMapping(
      value = "/logininfor/export",
      produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
  byte[] exportLogininfor(
      @RequestParam(value = "userName", required = false) String userName,
      @RequestParam(value = "ipaddr", required = false) String ipaddr,
      @RequestParam(value = "status", required = false) String status,
      @RequestParam(value = "beginTime", required = false) String beginTime,
      @RequestParam(value = "endTime", required = false) String endTime);

  /** 登录失败 TOP10 */
  @GetMapping("/stats/login-fail-top10")
  AjaxResult loginFailTop10();

  /** 权限变更时间线 */
  @GetMapping("/timeline/permission/{userId}")
  AjaxResult permissionTimeline(@PathVariable("userId") Long userId);

  /** 业务留痕追溯查询（scene 模糊/bizType/bizId/operator 可选，时间倒序） */
  @GetMapping("/trace/list")
  AjaxResult traceList(
      @RequestParam(value = "scene", required = false) String scene,
      @RequestParam(value = "bizType", required = false) String bizType,
      @RequestParam(value = "bizId", required = false) String bizId,
      @RequestParam(value = "operator", required = false) String operator,
      @RequestParam(value = "limit", defaultValue = "50") int limit);

  /** 权限快照 */
  @GetMapping("/snapshot/permission/{userId}")
  AjaxResult permissionSnapshot(@PathVariable("userId") Long userId);

  /** 安全配置快照 */
  @GetMapping("/snapshot/config")
  AjaxResult configSnapshot();
}
