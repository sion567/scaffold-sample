package com.scaffold.audit.dubbo;

import com.scaffold.audit.api.convert.SysLogininforConvert;
import com.scaffold.audit.api.convert.SysOperLogConvert;
import com.scaffold.audit.api.proto.BoolResponse;
import com.scaffold.audit.api.proto.RemoteLogService;
import com.scaffold.audit.api.proto.SaveLogRequest;
import com.scaffold.audit.api.proto.SaveLogininforRequest;
import com.scaffold.audit.repository.LogininforRepository;
import com.scaffold.audit.repository.OperLogRepository;
import com.scaffold.common.core.domain.R;
import com.scaffold.common.core.utils.uuid.SnowflakeIdGenerator;
import com.scaffold.common.security.annotation.InnerAuth;
import com.scaffold.system.api.domain.SysLogininfor;
import com.scaffold.system.api.domain.SysOperLog;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import org.apache.dubbo.config.annotation.DubboService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 审计日志写入口（IDL/protobuf，Triple 协议）
 *
 * <p>scaffold-audit 为审计域权威模块：操作日志写 audit_oper_log，登录日志写 audit_logininfor。 日志防篡改：仅追加，不提供修改/删除接口。
 *
 * @author ct
 */
@DubboService
public class RemoteLogProtoServiceImpl implements RemoteLogService {

  private static final Logger log = LoggerFactory.getLogger(RemoteLogProtoServiceImpl.class);

  private final OperLogRepository operLogRepository;
  private final LogininforRepository logininforRepository;
  private final SnowflakeIdGenerator idGenerator;

  public RemoteLogProtoServiceImpl(
      OperLogRepository operLogRepository,
      LogininforRepository logininforRepository,
      SnowflakeIdGenerator idGenerator) {
    this.operLogRepository = operLogRepository;
    this.logininforRepository = logininforRepository;
    this.idGenerator = idGenerator;
  }

  @Override
  @InnerAuth
  public BoolResponse saveLog(SaveLogRequest request) {
    try {
      SysOperLog operLog = SysOperLogConvert.toJava(request.getOperLog());
      // 主键应用侧生成（雪花），不依赖 DB 自增
      operLog.setOperId(idGenerator.nextId());
      // 服务端时钟定操作时间（等保审计时间权威源，与原 CURRENT_TIMESTAMP 写入语义一致）
      operLog.setOperTime(new Date());
      operLogRepository.save(operLog);
      return BoolResponse.newBuilder().setCode(R.SUCCESS).setData(true).build();
    } catch (Exception e) {
      // 审计写入失败不能中断业务，但必须本地留痕告警
      log.error("[audit] 操作日志写入失败: {}", e.getMessage(), e);
      return BoolResponse.newBuilder()
          .setCode(R.FAIL)
          .setMsg("操作日志写入失败: " + e.getMessage())
          .build();
    }
  }

  @Override
  public CompletableFuture<BoolResponse> saveLogAsync(SaveLogRequest request) {
    return CompletableFuture.completedFuture(saveLog(request));
  }

  @Override
  @InnerAuth
  public BoolResponse saveLogininfor(SaveLogininforRequest request) {
    try {
      SysLogininfor logininfor = SysLogininforConvert.toJava(request.getLogininfor());
      logininfor.setInfoId(idGenerator.nextId());
      logininfor.setAccessTime(new Date());
      logininforRepository.save(logininfor);
      return BoolResponse.newBuilder().setCode(R.SUCCESS).setData(true).build();
    } catch (Exception e) {
      log.error("[audit] 登录日志写入失败: {}", e.getMessage(), e);
      return BoolResponse.newBuilder()
          .setCode(R.FAIL)
          .setMsg("登录日志写入失败: " + e.getMessage())
          .build();
    }
  }

  @Override
  public CompletableFuture<BoolResponse> saveLogininforAsync(SaveLogininforRequest request) {
    return CompletableFuture.completedFuture(saveLogininfor(request));
  }
}
