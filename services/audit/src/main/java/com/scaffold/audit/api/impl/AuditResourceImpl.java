package com.scaffold.audit.api.impl;

import com.scaffold.audit.repository.LogininforRepository;
import com.scaffold.audit.repository.OperLogRepository;
import com.scaffold.audit.repository.SecuritySnapshotRepository;
import com.scaffold.audit.repository.AuditTraceRepository;
import com.scaffold.common.core.jpa.JpaSpecs;
import com.scaffold.common.core.utils.PageUtils;
import com.scaffold.common.core.utils.poi.ExcelUtil;
import com.scaffold.common.core.web.controller.BaseController;
import com.scaffold.common.core.web.domain.AjaxResult;
import com.scaffold.common.core.web.page.PageDomain;
import com.scaffold.common.core.web.page.TableDataInfo;
import com.scaffold.common.security.annotation.RequireExportApproval;
import com.scaffold.common.security.annotation.RequiresPermissions;
import com.scaffold.system.api.domain.SysLogininfor;
import com.scaffold.system.api.domain.SysOperLog;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

/**
 * 审计读写资源实现（Triple REST）
 *
 * <p>数据源为 scaffold-audit 自有表 audit_oper_log / audit_logininfor（仅追加）。
 *
 * <p>权限串对应 V1__init_system.sql 审计菜单（2000 审计中心）： audit:operlog:list / audit:logininfor:list /
 * audit:config:list / audit:snapshot:list / audit:stats:list
 *
 * @author ct
 */
@DubboService
public class AuditResourceImpl extends BaseController implements com.scaffold.audit.api.AuditResource {
  private final OperLogRepository operLogRepository;

  private final LogininforRepository logininforRepository;

  private final AuditTraceRepository traceRepository;

  private final SecuritySnapshotRepository securitySnapshotRepository;

  public AuditResourceImpl(
      OperLogRepository operLogRepository,
      LogininforRepository logininforRepository,
      AuditTraceRepository traceRepository,
      SecuritySnapshotRepository securitySnapshotRepository) {
    this.operLogRepository = operLogRepository;
    this.logininforRepository = logininforRepository;
    this.traceRepository = traceRepository;
    this.securitySnapshotRepository = securitySnapshotRepository;
  }

  @Override
  @RequiresPermissions("audit:operlog:list")
  public TableDataInfo operLog(
      Integer pageNum,
      Integer pageSize,
      String orderByColumn,
      String isAsc,
      String title,
      Integer businessType,
      Integer status,
      String operName,
      String operIp,
      String eventType,
      String bizType,
      String bizKey,
      String beginTime,
      String endTime) {
    Map<String, Object> range = new LinkedHashMap<>();
    range.put("beginTime", beginTime);
    range.put("endTime", endTime);
    Specification<Object> spec =
        JpaSpecs.likeIf("title", title)
            .and(JpaSpecs.eqIf("businessType", businessType))
            .and(JpaSpecs.eqIf("status", status))
            .and(JpaSpecs.likeIf("operName", operName))
            .and(JpaSpecs.likeIf("operIp", operIp))
            .and(JpaSpecs.eqIf("eventType", eventType))
            .and(JpaSpecs.eqIf("bizType", bizType))
            .and(JpaSpecs.likeIf("bizKey", bizKey))
            .and(JpaSpecs.dateRangeIf("operTime", range));
    return TableDataInfo.from(
        operLogRepository.page(spec, page(pageNum, pageSize, orderByColumn, isAsc, "operId")));
  }

  @Override
  @RequiresPermissions("audit:operlog:export")
  @RequireExportApproval("audit-oper-log")
  public byte[] exportOperLog(
      String title,
      Integer businessType,
      Integer status,
      String operName,
      String operIp,
      String eventType,
      String bizType,
      String bizKey,
      String beginTime,
      String endTime) {
    Map<String, Object> range = new LinkedHashMap<>();
    range.put("beginTime", beginTime);
    range.put("endTime", endTime);
    Specification<Object> spec =
        JpaSpecs.likeIf("title", title)
            .and(JpaSpecs.eqIf("businessType", businessType))
            .and(JpaSpecs.eqIf("status", status))
            .and(JpaSpecs.likeIf("operName", operName))
            .and(JpaSpecs.likeIf("operIp", operIp))
            .and(JpaSpecs.eqIf("eventType", eventType))
            .and(JpaSpecs.eqIf("bizType", bizType))
            .and(JpaSpecs.likeIf("bizKey", bizKey))
            .and(JpaSpecs.dateRangeIf("operTime", range));
    List<SysOperLog> list =
        operLogRepository.list(spec, Sort.by(Sort.Direction.DESC, "operId"));
    ExcelUtil<SysOperLog> util = new ExcelUtil<SysOperLog>(SysOperLog.class);
    return util.exportExcel(list, "操作日志");
  }

  @Override
  @RequiresPermissions("audit:logininfor:list")
  public TableDataInfo logininfor(
      Integer pageNum,
      Integer pageSize,
      String orderByColumn,
      String isAsc,
      String userName,
      String ipaddr,
      String status,
      String beginTime,
      String endTime) {
    Map<String, Object> range = new LinkedHashMap<>();
    range.put("beginTime", beginTime);
    range.put("endTime", endTime);
    Specification<Object> spec =
        JpaSpecs.likeIf("userName", userName)
            .and(JpaSpecs.likeIf("ipaddr", ipaddr))
            .and(JpaSpecs.eqIfNotBlank("status", status))
            .and(JpaSpecs.dateRangeIf("accessTime", range));
    return TableDataInfo.from(
        logininforRepository.page(
            spec, page(pageNum, pageSize, orderByColumn, isAsc, "infoId")));
  }

  @Override
  @RequiresPermissions("audit:logininfor:export")
  public byte[] exportLogininfor(
      String userName, String ipaddr, String status, String beginTime, String endTime) {
    Map<String, Object> range = new LinkedHashMap<>();
    range.put("beginTime", beginTime);
    range.put("endTime", endTime);
    Specification<Object> spec =
        JpaSpecs.likeIf("userName", userName)
            .and(JpaSpecs.likeIf("ipaddr", ipaddr))
            .and(JpaSpecs.eqIfNotBlank("status", status))
            .and(JpaSpecs.dateRangeIf("accessTime", range));
    List<SysLogininfor> list =
        logininforRepository.list(spec, Sort.by(Sort.Direction.DESC, "infoId"));
    ExcelUtil<SysLogininfor> util = new ExcelUtil<SysLogininfor>(SysLogininfor.class);
    return util.exportExcel(list, "登录日志");
  }

  @Override
  @RequiresPermissions("audit:stats:list")
  public AjaxResult loginFailTop10() {
    Date before = Date.from(java.time.Instant.now().minus(7, java.time.temporal.ChronoUnit.DAYS));
    List<LogininforRepository.LoginFailStat> stats =
        logininforRepository.selectLoginFailTop10(before, PageRequest.of(0, 10));
    // 保持原 Map 形态输出（键与原列别名一致）
    List<Map<String, Object>> rows = new ArrayList<>(stats.size());
    for (LogininforRepository.LoginFailStat stat : stats) {
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("ip", stat.getIp());
      row.put("username", stat.getUsername());
      row.put("failCount", stat.getFailCount());
      row.put("lastFailTime", stat.getLastFailTime());
      rows.add(row);
    }
    return success(rows);
  }

  @Override
  @RequiresPermissions("audit:trace:list")
  public AjaxResult traceList(
      String scene, String bizType, String bizId, String operator, int limit) {
    int pageSize = Math.min(Math.max(limit, 1), 200);
    Specification<Object> spec =
        JpaSpecs.likeIf("scene", scene)
            .and(JpaSpecs.eqIf("bizType", bizType))
            .and(JpaSpecs.eqIf("bizId", bizId))
            .and(JpaSpecs.likeIf("operator", operator));
    List<?> rows =
        traceRepository
            .page(
                spec,
                PageRequest.of(0, pageSize, Sort.by(Sort.Direction.DESC, "traceId")))
            .getContent();
    return success(rows);
  }

  @Override
  @RequiresPermissions("audit:snapshot:list")
  public AjaxResult permissionTimeline(Long userId) {
    return success(
        operLogRepository.selectPermissionChanges(userId, "user:" + userId + "%"));
  }

  @Override
  @RequiresPermissions("audit:snapshot:list")
  public AjaxResult permissionSnapshot(Long userId) {
    return success(securitySnapshotRepository.selectUserRoles(userId));
  }

  @Override
  @RequiresPermissions("audit:config:list")
  public AjaxResult configSnapshot() {
    return success(securitySnapshotRepository.selectSecurityConfig());
  }

  /** 分页参数构建：请求未指定排序时回落到各表的业务主键倒序（对齐原 SQL 固定 order by） */
  private Pageable page(
      Integer pageNum, Integer pageSize, String orderByColumn, String isAsc, String defaultSortField) {
    PageDomain pageDomain = new PageDomain();
    pageDomain.setPageNum(pageNum);
    pageDomain.setPageSize(pageSize);
    pageDomain.setOrderByColumn(orderByColumn);
    pageDomain.setIsAsc(isAsc);
    PageRequest pageable = PageUtils.toPageRequest(pageDomain);
    if (pageable.getSort().isUnsorted()) {
      pageable =
          PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
              Sort.by(Sort.Direction.DESC, defaultSortField));
    }
    return pageable;
  }
}
