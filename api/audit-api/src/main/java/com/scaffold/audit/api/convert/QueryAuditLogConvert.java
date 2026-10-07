package com.scaffold.audit.api.convert;

import static com.scaffold.audit.api.convert.ProtoConverts.emptyToNull;
import static com.scaffold.audit.api.convert.ProtoConverts.toDate;
import static com.scaffold.audit.api.convert.ProtoConverts.toLong;
import static com.scaffold.audit.api.convert.ProtoConverts.toMillis;

import com.scaffold.audit.api.proto.QueryAuditLogProto;
import com.scaffold.system.api.domain.QueryAuditLog;

/**
 * QueryAuditLog <-> QueryAuditLogProto 转换。
 *
 * @author ct
 */
public final class QueryAuditLogConvert {
  private QueryAuditLogConvert() {}

  public static QueryAuditLogProto toProto(QueryAuditLog log) {
    if (log == null) {
      return null;
    }
    QueryAuditLogProto.Builder builder = QueryAuditLogProto.newBuilder();
    if (log.getId() != null) {
      builder.setId(log.getId());
    }
    if (log.getCallerSystem() != null) {
      builder.setCallerSystem(log.getCallerSystem());
    }
    if (log.getOperatorName() != null) {
      builder.setOperatorName(log.getOperatorName());
    }
    if (log.getSubjectUid() != null) {
      builder.setSubjectUid(log.getSubjectUid());
    }
    if (log.getSubjectMasked() != null) {
      builder.setSubjectMasked(log.getSubjectMasked());
    }
    if (log.getQueryType() != null) {
      builder.setQueryType(log.getQueryType());
    }
    if (log.getPurpose() != null) {
      builder.setPurpose(log.getPurpose());
    }
    if (log.getRequestId() != null) {
      builder.setRequestId(log.getRequestId());
    }
    if (log.getResultCount() != null) {
      builder.setResultCount(log.getResultCount());
    }
    if (log.getResultSummary() != null) {
      builder.setResultSummary(log.getResultSummary());
    }
    if (log.getQueryTime() != null) {
      builder.setQueryTime(toMillis(log.getQueryTime()));
    }
    return builder.build();
  }

  public static QueryAuditLog toJava(QueryAuditLogProto proto) {
    if (proto == null) {
      return null;
    }
    QueryAuditLog log = new QueryAuditLog();
    log.setId(toLong(proto.getId()));
    log.setCallerSystem(emptyToNull(proto.getCallerSystem()));
    log.setOperatorName(emptyToNull(proto.getOperatorName()));
    log.setSubjectUid(emptyToNull(proto.getSubjectUid()));
    log.setSubjectMasked(emptyToNull(proto.getSubjectMasked()));
    log.setQueryType(emptyToNull(proto.getQueryType()));
    log.setPurpose(emptyToNull(proto.getPurpose()));
    log.setRequestId(emptyToNull(proto.getRequestId()));
    log.setResultCount(toLong(proto.getResultCount()));
    log.setResultSummary(emptyToNull(proto.getResultSummary()));
    log.setQueryTime(toDate(proto.getQueryTime()));
    return log;
  }
}
