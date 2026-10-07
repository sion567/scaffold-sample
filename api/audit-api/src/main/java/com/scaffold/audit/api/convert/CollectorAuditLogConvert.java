package com.scaffold.audit.api.convert;

import static com.scaffold.audit.api.convert.ProtoConverts.emptyToNull;
import static com.scaffold.audit.api.convert.ProtoConverts.toDate;
import static com.scaffold.audit.api.convert.ProtoConverts.toLong;
import static com.scaffold.audit.api.convert.ProtoConverts.toMillis;

import com.scaffold.audit.api.proto.CollectorAuditLogProto;
import com.scaffold.system.api.domain.CollectorAuditLog;

/**
 * CollectorAuditLog <-> CollectorAuditLogProto 转换。
 *
 * @author ct
 */
public final class CollectorAuditLogConvert {
  private CollectorAuditLogConvert() {}

  public static CollectorAuditLogProto toProto(CollectorAuditLog log) {
    if (log == null) {
      return null;
    }
    CollectorAuditLogProto.Builder builder = CollectorAuditLogProto.newBuilder();
    if (log.getId() != null) {
      builder.setId(log.getId());
    }
    if (log.getDomain() != null) {
      builder.setDomain(log.getDomain());
    }
    if (log.getSource() != null) {
      builder.setSource(log.getSource());
    }
    if (log.getChannelType() != null) {
      builder.setChannelType(log.getChannelType());
    }
    if (log.getStatus() != null) {
      builder.setStatus(log.getStatus());
    }
    if (log.getRecordCount() != null) {
      builder.setRecordCount(log.getRecordCount());
    }
    if (log.getWatermarkBefore() != null) {
      builder.setWatermarkBefore(log.getWatermarkBefore());
    }
    if (log.getWatermarkAfter() != null) {
      builder.setWatermarkAfter(log.getWatermarkAfter());
    }
    if (log.getErrorMsg() != null) {
      builder.setErrorMsg(log.getErrorMsg());
    }
    if (log.getCostMs() != null) {
      builder.setCostMs(log.getCostMs());
    }
    if (log.getStartTime() != null) {
      builder.setStartTime(toMillis(log.getStartTime()));
    }
    if (log.getCreateTime() != null) {
      builder.setCreateTime(toMillis(log.getCreateTime()));
    }
    return builder.build();
  }

  public static CollectorAuditLog toJava(CollectorAuditLogProto proto) {
    if (proto == null) {
      return null;
    }
    CollectorAuditLog log = new CollectorAuditLog();
    log.setId(toLong(proto.getId()));
    log.setDomain(emptyToNull(proto.getDomain()));
    log.setSource(emptyToNull(proto.getSource()));
    log.setChannelType(emptyToNull(proto.getChannelType()));
    log.setStatus(emptyToNull(proto.getStatus()));
    log.setRecordCount(toLong(proto.getRecordCount()));
    log.setWatermarkBefore(emptyToNull(proto.getWatermarkBefore()));
    log.setWatermarkAfter(emptyToNull(proto.getWatermarkAfter()));
    log.setErrorMsg(emptyToNull(proto.getErrorMsg()));
    log.setCostMs(toLong(proto.getCostMs()));
    log.setStartTime(toDate(proto.getStartTime()));
    log.setCreateTime(toDate(proto.getCreateTime()));
    return log;
  }
}
