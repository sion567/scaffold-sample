package com.scaffold.audit.dubbo;

import com.scaffold.audit.api.convert.CollectorAuditLogConvert;
import com.scaffold.audit.api.convert.QueryAuditLogConvert;
import com.scaffold.audit.api.proto.BoolResponse;
import com.scaffold.audit.api.proto.RemoteAuditService;
import com.scaffold.audit.api.proto.SaveCollectorAuditRequest;
import com.scaffold.audit.api.proto.SaveQueryAuditRequest;
import com.scaffold.audit.repository.CollectorAuditRepository;
import com.scaffold.audit.repository.QueryAuditRepository;
import com.scaffold.common.core.domain.R;
import com.scaffold.common.core.utils.StringUtils;
import com.scaffold.common.core.utils.uuid.SnowflakeIdGenerator;
import com.scaffold.system.api.domain.CollectorAuditLog;
import com.scaffold.system.api.domain.QueryAuditLog;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import org.apache.dubbo.config.annotation.DubboService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 审计服务实现（采集留痕 + 查询审计，IDL/protobuf，Triple 协议） 日志防篡改：仅追加，不提供修改/删除接口
 *
 * @author ct
 */
@DubboService
public class RemoteAuditProtoServiceImpl implements RemoteAuditService {

  private static final Logger log = LoggerFactory.getLogger(RemoteAuditProtoServiceImpl.class);

  /** query_audit_log.result_summary 列宽（超长截断，与原 SUBSTRING 写入语义一致） */
  static final int RESULT_SUMMARY_MAX = 500;

  private final CollectorAuditRepository collectorAuditRepository;
  private final QueryAuditRepository queryAuditRepository;
  private final SnowflakeIdGenerator idGenerator;

  public RemoteAuditProtoServiceImpl(
      CollectorAuditRepository collectorAuditRepository,
      QueryAuditRepository queryAuditRepository,
      SnowflakeIdGenerator idGenerator) {
    this.collectorAuditRepository = collectorAuditRepository;
    this.queryAuditRepository = queryAuditRepository;
    this.idGenerator = idGenerator;
  }

  @Override
  public BoolResponse saveCollectorAudit(SaveCollectorAuditRequest request) {
    CollectorAuditLog collectorAuditLog = CollectorAuditLogConvert.toJava(request.getAuditLog());
    // 主键应用侧生成（雪花），不依赖 DB 自增
    collectorAuditLog.setId(idGenerator.nextId());
    // 服务端时钟定入库时间（与原 CURRENT_TIMESTAMP 写入语义一致）
    collectorAuditLog.setCreateTime(new Date());
    try {
      collectorAuditRepository.save(collectorAuditLog);
      return BoolResponse.newBuilder().setCode(R.SUCCESS).setData(true).build();
    } catch (Exception e) {
      // 审计写入失败不影响业务采集，但必须本地留痕告警
      log.error(
          "[audit] 采集留痕写入失败: domain={}, source={}, {}",
          collectorAuditLog.getDomain(),
          collectorAuditLog.getSource(),
          e.getMessage(),
          e);
      return BoolResponse.newBuilder()
          .setCode(R.FAIL)
          .setMsg("采集留痕写入失败: " + e.getMessage())
          .build();
    }
  }

  @Override
  public CompletableFuture<BoolResponse> saveCollectorAuditAsync(
      SaveCollectorAuditRequest request) {
    return CompletableFuture.completedFuture(saveCollectorAudit(request));
  }

  @Override
  public BoolResponse saveQueryAudit(SaveQueryAuditRequest request) {
    QueryAuditLog queryAuditLog = QueryAuditLogConvert.toJava(request.getAuditLog());
    queryAuditLog.setId(idGenerator.nextId());
    // 超长截断到列宽（与原 SUBSTRING(result_summary, 1, 500) 写入语义一致）
    queryAuditLog.setResultSummary(
        StringUtils.substring(queryAuditLog.getResultSummary(), 0, RESULT_SUMMARY_MAX));
    try {
      queryAuditRepository.save(queryAuditLog);
      return BoolResponse.newBuilder().setCode(R.SUCCESS).setData(true).build();
    } catch (Exception e) {
      log.error(
          "[audit] 查询审计写入失败: subject={}, {}", queryAuditLog.getSubjectUid(), e.getMessage(), e);
      return BoolResponse.newBuilder()
          .setCode(R.FAIL)
          .setMsg("查询审计写入失败: " + e.getMessage())
          .build();
    }
  }

  @Override
  public CompletableFuture<BoolResponse> saveQueryAuditAsync(SaveQueryAuditRequest request) {
    return CompletableFuture.completedFuture(saveQueryAudit(request));
  }
}
